package com.example.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.example.data.ClipEntity
import com.example.data.ClipRepository
import com.example.data.SyncPreferences
import com.example.ime.DefaultImeDetector
import com.example.protocol.Protocol
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared abstraction for all Android clipboard capture and injection paths.
 * Normalizes input from IME, background service, TrampolineActivity, ProcessText, and Share.
 * Centralizes SHA-256 loop suppression, conflict tracking, and safe public ClipboardManager access.
 */
class ClipboardCaptureManager(
    private val context: Context,
    private val clipRepository: ClipRepository,
    private val syncPreferences: SyncPreferences,
    private val clipboardManager: ClipboardManager? = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager,
    private val sensitiveClassifier: SensitiveClassifier = DefaultSensitiveClassifier(),
    private val defaultImeDetector: DefaultImeDetector? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    fun interface OutboundClipListener {
        fun onClipCaptured(clip: Protocol.Message.Clip)
    }

    @Volatile
    private var outboundListener: OutboundClipListener? = null

    private val captureJob = SupervisorJob()
    private val captureScope = CoroutineScope(ioDispatcher + captureJob)

    @Volatile
    private var isMonitoring: Boolean = false

    private val primaryClipListener = ClipboardManager.OnPrimaryClipChangedListener {
        captureScope.launch {
            try {
                handlePrimaryClipChanged()
            } catch (_: Exception) {}
        }
    }

    /**
     * Starts continuous clipboard monitoring via ClipboardManager.OnPrimaryClipChangedListener.
     * Respects default-IME privilege: if defaultImeDetector is provided and MyClipSync
     * is not the default IME, monitoring will NOT be started.
     * Returns true if monitoring is now active.
     */
    @Synchronized
    fun startMonitoring(): Boolean {
        if (defaultImeDetector != null && !defaultImeDetector.isMyClipSyncDefaultIme()) {
            stopMonitoring()
            return false
        }
        if (isMonitoring) return true
        val cm = clipboardManager ?: return false
        return try {
            cm.addPrimaryClipChangedListener(primaryClipListener)
            isMonitoring = true
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Stops continuous clipboard monitoring and unregisters the listener.
     */
    @Synchronized
    fun stopMonitoring() {
        if (!isMonitoring) return
        val cm = clipboardManager ?: return
        try {
            cm.removePrimaryClipChangedListener(primaryClipListener)
        } catch (_: Exception) {}
        isMonitoring = false
    }

    fun isMonitoring(): Boolean = isMonitoring

    // Echo suppression state (armed when remote clip arrives from Linux)
    @Volatile
    private var suppressNextHash: String? = null

    @Volatile
    private var suppressUntilMs: Long = 0L

    // Conflict tracking for recent local clips
    @Volatile
    private var lastLocalClip: Protocol.Message.Clip? = null

    fun setOutboundClipListener(listener: OutboundClipListener?) {
        this.outboundListener = listener
    }

    fun getLastLocalClip(): Protocol.Message.Clip? = lastLocalClip

    /**
     * Arm echo suppression for a specific text snippet for [durationMs].
     */
    fun armSuppression(text: String, durationMs: Long = Protocol.SUPPRESS_WINDOW_MS) {
        suppressNextHash = Protocol.sha256(text)
        suppressUntilMs = System.currentTimeMillis() + durationMs
    }

    /**
     * Check if the given text matches the active suppression hash and window.
     */
    fun isSuppressed(text: String, now: Long = System.currentTimeMillis()): Boolean {
        val targetHash = suppressNextHash ?: return false
        if (now > suppressUntilMs) {
            suppressNextHash = null
            return false
        }
        val textHash = Protocol.sha256(text)
        return textHash == targetHash
    }

    /**
     * Check if the given text is a duplicate of a local clip captured within [Protocol.CONFLICT_WINDOW_MS].
     */
    fun isRecentDuplicate(text: String, now: Long = System.currentTimeMillis()): Boolean {
        val last = lastLocalClip ?: return false
        return last.text == text && (now - last.ts) < Protocol.CONFLICT_WINDOW_MS
    }

    /**
     * Safely reads the primary clip from ClipboardManager.
     * Handles null clipboard, empty clips, security exceptions (Android 10+ background),
     * and non-text MIME types gracefully without crashing.
     */
    suspend fun handlePrimaryClipChanged(): ClipEntity? = withContext(ioDispatcher) {
        val cm = clipboardManager ?: return@withContext null
        try {
            // Check description first if available
            val description = cm.primaryClipDescription
            if (description != null && !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                var isTextType = false
                for (i in 0 until description.mimeTypeCount) {
                    val mime = description.getMimeType(i)
                    if (mime.startsWith("text/")) {
                        isTextType = true
                        break
                    }
                }
                if (!isTextType) return@withContext null
            }

            val clipData = cm.primaryClip ?: return@withContext null
            if (clipData.itemCount <= 0) return@withContext null

            val item = clipData.getItemAt(0) ?: return@withContext null
            val text = item.coerceToText(context)?.toString() ?: ""
            if (text.isBlank()) return@withContext null

            val isSensitive = sensitiveClassifier.isSensitive(clipData)
            captureLocalClip(text = text, isSensitive = isSensitive)
        } catch (_: SecurityException) {
            // Android 10+ background restriction or OEM restriction
            null
        } catch (_: Exception) {
            // Defensive against dead IPC, null binders, etc.
            null
        }
    }

    /**
     * Main pipeline entry point for all local clipboard inputs.
     * Validates suppression, coalesces multi-path duplicates, persists to Room,
     * and notifies outbound listener for Bluetooth transmission.
     */
    suspend fun captureLocalClip(
        text: String,
        isSensitive: Boolean = false,
        pinned: Boolean = false
    ): ClipEntity? = withContext(ioDispatcher) {
        if (text.isBlank()) return@withContext null

        val now = System.currentTimeMillis()

        // 1. Loop prevention / echo suppression check
        if (isSuppressed(text, now)) {
            return@withContext null
        }

        // 2. Coalesce duplicate callbacks within conflict window
        if (isRecentDuplicate(text, now)) {
            return@withContext null
        }

        // 3. Construct protocol message
        val config = syncPreferences.getConfig()
        val clipMsg = Protocol.Message.Clip(
            src = config.deviceId,
            ts = now,
            text = text
        )
        lastLocalClip = clipMsg

        // 4. Construct entity and persist to Room
        val entity = ClipEntity(
            id = clipMsg.id,
            text = clipMsg.text,
            ts = clipMsg.ts,
            src = clipMsg.src,
            direction = "local",
            conflictLoser = false,
            pinned = pinned,
            sensitive = isSensitive
        )

        val inserted = clipRepository.insertClip(entity)
        if (!inserted) {
            return@withContext null
        }

        // 5. Hand off to outbound transport (ClipSyncService)
        outboundListener?.onClipCaptured(clipMsg)

        entity
    }

    /**
     * Applies a remote clip received from Linux to Android's primary clipboard,
     * arming echo suppression so that subsequent clipboard change listeners do not
     * re-broadcast it back to Linux.
     */
    suspend fun applyRemoteClip(
        text: String,
        isSensitive: Boolean = false
    ): Boolean {
        // Arm suppression before writing to clipboard
        armSuppression(text, Protocol.SUPPRESS_WINDOW_MS)

        return withContext(mainDispatcher) {
            try {
                val cm = clipboardManager ?: return@withContext false
                val clipData = ClipData.newPlainText("MyClipSync", text)
                if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val bundle = PersistableBundle().apply {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                    clipData.description.extras = bundle
                }
                cm.setPrimaryClip(clipData)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Set local clipboard without triggering re-broadcast (e.g. user taps "Copy" in history).
     */
    suspend fun copyToClipboardWithoutBroadcast(
        text: String,
        isSensitive: Boolean = false
    ): Boolean {
        armSuppression(text, Protocol.SUPPRESS_WINDOW_MS)
        return withContext(mainDispatcher) {
            try {
                val cm = clipboardManager ?: return@withContext false
                val clipData = ClipData.newPlainText("MyClipSync", text)
                if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val bundle = PersistableBundle().apply {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                    clipData.description.extras = bundle
                }
                cm.setPrimaryClip(clipData)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
