package com.example

import com.example.protocol.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProtocolUnitTest {

    @Test
    fun testProtocolRoundtrip() {
        val clip = Protocol.Message.Clip(
            src = "android-abc123",
            ts = 1711000000L,
            text = "Hello Linux Mint! Unicode test: ✓ 🚀 \n Tab \t & special chars"
        )
        val frame = Protocol.encodeFrame(clip)
        assertTrue(frame.size > 4)

        val framer = Protocol.StreamFramer()
        val decoded = framer.push(frame, frame.size)
        assertEquals(1, decoded.size)

        val msg = decoded[0] as Protocol.Message.Clip
        assertEquals(clip.id, msg.id)
        assertEquals(clip.src, msg.src)
        assertEquals(clip.ts, msg.ts)
        assertEquals(clip.text, msg.text)
        assertEquals("text/plain", msg.mime)
    }

    @Test
    fun testSplitFrames() {
        val msg1 = Protocol.Message.Clip(src = "android-1", ts = 100L, text = "First message")
        val msg2 = Protocol.Message.Ack(src = "android-1", ts = 101L, forId = msg1.id)

        val bytes1 = Protocol.encodeFrame(msg1)
        val bytes2 = Protocol.encodeFrame(msg2)
        val combined = bytes1 + bytes2

        val framer = Protocol.StreamFramer()

        // Push in tiny 10-byte chunks to test stream reconstruction
        val collected = mutableListOf<Protocol.Message>()
        var offset = 0
        while (offset < combined.size) {
            val chunkLen = Math.min(10, combined.size - offset)
            val chunk = combined.copyOfRange(offset, offset + chunkLen)
            collected.addAll(framer.push(chunk, chunkLen))
            offset += chunkLen
        }

        assertEquals(2, collected.size)
        assertTrue(collected[0] is Protocol.Message.Clip)
        assertTrue(collected[1] is Protocol.Message.Ack)
    }

    @Test
    fun testLastWriteWins() {
        // Higher timestamp wins
        assertTrue(Protocol.isLocalWinner(localTs = 2000L, localSrc = "android", remoteTs = 1000L, remoteSrc = "linux"))
        assertFalse(Protocol.isLocalWinner(localTs = 1000L, localSrc = "android", remoteTs = 2000L, remoteSrc = "linux"))

        // Tie-breaker: larger src wins
        assertTrue(Protocol.isLocalWinner(localTs = 1000L, localSrc = "linux-pc", remoteTs = 1000L, remoteSrc = "android-phone"))
        assertFalse(Protocol.isLocalWinner(localTs = 1000L, localSrc = "android-phone", remoteTs = 1000L, remoteSrc = "linux-pc"))
    }

    @Test
    fun testEchoSuppressionSha256() {
        val text = "Synchronized text snippet"
        val hash1 = Protocol.sha256(text)
        val hash2 = Protocol.sha256(text)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }
}
