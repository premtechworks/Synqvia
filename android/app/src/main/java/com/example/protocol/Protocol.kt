package com.example.protocol

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID

object Protocol {
    const val SERVICE_UUID_STRING = "7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002"
    val SERVICE_UUID: UUID = UUID.fromString(SERVICE_UUID_STRING)
    const val DEFAULT_CHANNEL = 1
    const val PROTOCOL_VERSION = 1
    const val MAX_PAYLOAD_BYTES = 2 * 1024 * 1024 // 2 MiB
    const val SUPPRESS_WINDOW_MS = 2000L
    const val CONFLICT_WINDOW_MS = 1500L
    const val MAX_OUTBOX_SIZE = 1000
    const val SEEN_CACHE_SIZE = 500

    sealed class Message(
        open val v: Int = PROTOCOL_VERSION,
        open val type: String,
        open val id: String,
        open val src: String,
        open val ts: Long
    ) {
        abstract fun toJson(): JSONObject

        data class Hello(
            override val id: String = UUID.randomUUID().toString(),
            override val src: String,
            val name: String,
            override val ts: Long = System.currentTimeMillis()
        ) : Message(PROTOCOL_VERSION, "hello", id, src, ts) {
            override fun toJson(): JSONObject = JSONObject().apply {
                put("v", v)
                put("type", type)
                put("id", id)
                put("src", src)
                put("name", name)
                put("ts", ts)
            }
        }

        data class Clip(
            override val id: String = UUID.randomUUID().toString(),
            override val src: String,
            override val ts: Long = System.currentTimeMillis(),
            val text: String,
            val mime: String = "text/plain"
        ) : Message(PROTOCOL_VERSION, "clip", id, src, ts) {
            override fun toJson(): JSONObject = JSONObject().apply {
                put("v", v)
                put("type", type)
                put("id", id)
                put("src", src)
                put("ts", ts)
                put("text", text)
                put("mime", mime)
            }
        }

        data class Ack(
            override val id: String = UUID.randomUUID().toString(),
            override val src: String,
            override val ts: Long = System.currentTimeMillis(),
            val forId: String
        ) : Message(PROTOCOL_VERSION, "ack", id, src, ts) {
            override fun toJson(): JSONObject = JSONObject().apply {
                put("v", v)
                put("type", type)
                put("id", id)
                put("src", src)
                put("ts", ts)
                put("for", forId)
            }
        }

        data class Bye(
            override val id: String = UUID.randomUUID().toString(),
            override val src: String,
            override val ts: Long = System.currentTimeMillis(),
            val reason: String = "shutdown" // shutdown, duplicate, version
        ) : Message(PROTOCOL_VERSION, "bye", id, src, ts) {
            override fun toJson(): JSONObject = JSONObject().apply {
                put("v", v)
                put("type", type)
                put("id", id)
                put("src", src)
                put("ts", ts)
                put("reason", reason)
            }
        }
    }

    fun parseMessage(jsonStr: String): Message? {
        return try {
            val obj = JSONObject(jsonStr)
            val v = obj.optInt("v", -1)
            if (v != PROTOCOL_VERSION) return null
            val type = obj.optString("type", "")
            val id = obj.optString("id", "")
            val src = obj.optString("src", "")
            val ts = obj.optLong("ts", System.currentTimeMillis())

            when (type) {
                "hello" -> Message.Hello(
                    id = id,
                    src = src,
                    name = obj.optString("name", "Unknown"),
                    ts = ts
                )
                "clip" -> Message.Clip(
                    id = id,
                    src = src,
                    ts = ts,
                    text = obj.optString("text", ""),
                    mime = obj.optString("mime", "text/plain")
                )
                "ack" -> Message.Ack(
                    id = id,
                    src = src,
                    ts = ts,
                    forId = obj.optString("for", "")
                )
                "bye" -> Message.Bye(
                    id = id,
                    src = src,
                    ts = ts,
                    reason = obj.optString("reason", "shutdown")
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Encode message to wire frame: [4 bytes Big-Endian length][UTF-8 JSON Payload]
     */
    fun encodeFrame(message: Message): ByteArray {
        val payload = message.toJson().toString().toByteArray(Charsets.UTF_8)
        val header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(payload.size).array()
        return header + payload
    }

    /**
     * Deterministic Last-Write-Wins (LWW) conflict resolver:
     * Returns true if local wins; false if remote wins.
     * Rule: higher ts wins; if ts equal, larger src (lexicographical) wins.
     */
    fun isLocalWinner(localTs: Long, localSrc: String, remoteTs: Long, remoteSrc: String): Boolean {
        return when {
            localTs > remoteTs -> true
            localTs < remoteTs -> false
            else -> localSrc >= remoteSrc
        }
    }

    /**
     * Compute SHA-256 hash of text for echo suppression tracking
     */
    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in hash) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    /**
     * Stream frame decoder that accumulates incoming chunks and outputs complete messages
     */
    class StreamFramer {
        private val buffer = ByteArrayOutputStream()

        @Synchronized
        fun push(data: ByteArray, length: Int): List<Message> {
            buffer.write(data, 0, length)
            val result = mutableListOf<Message>()
            var bytes = buffer.toByteArray()

            while (bytes.size >= 4) {
                val frameLen = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.BIG_ENDIAN).int
                if (frameLen < 0 || frameLen > MAX_PAYLOAD_BYTES) {
                    // Frame length corrupted or exceeded 2MB limit -> reset buffer
                    buffer.reset()
                    break
                }
                val totalRequired = 4 + frameLen
                if (bytes.size < totalRequired) {
                    // Waiting for more data chunks
                    break
                }

                val frameData = String(bytes, 4, frameLen, Charsets.UTF_8)
                parseMessage(frameData)?.let { result.add(it) }

                // Discard processed frame
                val remainingSize = bytes.size - totalRequired
                buffer.reset()
                if (remainingSize > 0) {
                    buffer.write(bytes, totalRequired, remainingSize)
                    bytes = buffer.toByteArray()
                } else {
                    break
                }
            }

            return result
        }

        @Synchronized
        fun reset() {
            buffer.reset()
        }
    }
}
