package com.github.premtechworks.synqvia.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.github.premtechworks.synqvia.MainActivity
import com.github.premtechworks.synqvia.SynqviaApp
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.clipboard.ClipboardCaptureManager
import com.github.premtechworks.synqvia.ime.DefaultImeDetector
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.data.ClipRepository
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.protocol.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.LinkedList

sealed class SyncConnectionState {
    data class Connected(val peerName: String, val mac: String) : SyncConnectionState()
    data class Retrying(val attempt: Int, val nextRetrySec: Int) : SyncConnectionState()
    data class Offline(val reason: String) : SyncConnectionState()
    data object Syncing : SyncConnectionState()
}

class ClipSyncService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var clipRepository: ClipRepository
    private lateinit var syncPreferences: SyncPreferences
    private lateinit var clipboardCaptureManager: ClipboardCaptureManager
    private lateinit var defaultImeDetector: DefaultImeDetector

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // Outbox queue for ack-tracked reliable delivery
    private val outbox = Collections.synchronizedList(LinkedList<Protocol.Message.Clip>())

    // Seen IDs LRU cache
    private val seenIds = Collections.synchronizedSet(LinkedHashSet<String>())

    private var connectionLoopJob: Job? = null
    private var currentPeerName: String = "Linux PC"

    private val imeSettingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            evaluateClipboardMonitoring()
        }
    }

    fun evaluateClipboardMonitoring() {
        val isDefault = defaultImeDetector.isSynqviaDefaultIme()
        _isDefaultImeFlow.value = isDefault
        if (isDefault) {
            clipboardCaptureManager.startMonitoring()
        } else {
            clipboardCaptureManager.stopMonitoring()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as SynqviaApp
        clipRepository = app.container.clipRepository
        syncPreferences = app.container.syncPreferences
        clipboardCaptureManager = app.container.clipboardCaptureManager
        defaultImeDetector = app.container.defaultImeDetector

        // Listen for outbound clips from shared pipeline
        clipboardCaptureManager.setOutboundClipListener { clip ->
            queueAndSendClip(clip)
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        createNotificationChannel()
        startForegroundWithNotification("Starting...")

        try {
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
                false,
                imeSettingsObserver
            )
        } catch (_: Exception) {
            // Defensive against OEM or security restrictions
        }

        evaluateClipboardMonitoring()

        startConnectionLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        evaluateClipboardMonitoring()

        when (intent?.action) {
            ACTION_INJECT -> {
                val injectedText = intent.getStringExtra(EXTRA_TEXT) ?: ""
                serviceScope.launch(Dispatchers.IO) {
                    clipboardCaptureManager.captureLocalClip(injectedText)
                }
            }
            ACTION_SYNC_NOW -> {
                serviceScope.launch(Dispatchers.IO) {
                    val selection = SelectionCache.getFreshSelection()
                    if (!selection.isNullOrBlank()) {
                        clipboardCaptureManager.captureLocalClip(selection)
                    } else {
                        clipboardCaptureManager.handlePrimaryClipChanged()
                    }
                }
            }
            ACTION_RECONNECT -> {
                startConnectionLoop()
            }
        }
        return START_STICKY
    }

    private fun queueAndSendClip(clip: Protocol.Message.Clip) {
        synchronized(outbox) {
            if (outbox.size >= Protocol.MAX_OUTBOX_SIZE) {
                outbox.removeFirst()
            }
            outbox.add(clip)
        }

        sendPendingFrame(clip)
    }

    private fun sendPendingFrame(msg: Protocol.Message) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val os = outputStream
                if (os != null && activeSocket?.isConnected == true) {
                    val bytes = Protocol.encodeFrame(msg)
                    os.write(bytes)
                    os.flush()
                }
            } catch (_: Exception) {
                // Socket error; will be handled by receiver loop / reconnect
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startConnectionLoop() {
        connectionLoopJob?.cancel()
        connectionLoopJob = serviceScope.launch(Dispatchers.IO) {
            var attempt = 0
            val backoffs = longArrayOf(2000L, 5000L, 10000L, 30000L)

            while (isActive) {
                val hasBtPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(
                        this@ClipSyncService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                if (!hasBtPermission) {
                    updateState(SyncConnectionState.Offline("Grant Bluetooth permission in Setup"))
                    delay(3000L)
                    continue
                }

                // If on Android Q+ and permission is now granted, ensure foreground type includes connectedDevice
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        startForeground(NOTIFICATION_ID, buildNotification("Ready"), computeForegroundServiceType())
                    } catch (_: Exception) {}
                }

                val config = syncPreferences.getConfig()
                val pcMac = config.pcMac.trim()

                if (pcMac.isBlank() || !SyncPreferences.isValidMac(pcMac)) {
                    updateState(SyncConnectionState.Offline("Set PC MAC address"))
                    delay(3000L)
                    continue
                }

                val adapter = bluetoothAdapter
                if (adapter == null || !adapter.isEnabled) {
                    updateState(SyncConnectionState.Offline("Bluetooth disabled"))
                    delay(4000L)
                    continue
                }

                attempt++
                val delayTime = backoffs[(attempt - 1).coerceAtMost(backoffs.size - 1)]
                updateState(SyncConnectionState.Retrying(attempt, (delayTime / 1000).toInt()))

                try {
                    val device: BluetoothDevice = adapter.getRemoteDevice(pcMac)
                    adapter.cancelDiscovery()

                    var socket: BluetoothSocket? = null
                    try {
                        // Standard SPP UUID connection
                        socket = device.createRfcommSocketToServiceRecord(Protocol.SERVICE_UUID)
                        socket.connect()
                    } catch (_: Exception) {
                        // Channel fallback if SDP record resolution failed
                        socket?.close()
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, config.channel) as BluetoothSocket
                        socket.connect()
                    }

                    if (socket != null && socket.isConnected) {
                        activeSocket = socket
                        outputStream = socket.outputStream
                        attempt = 0
                        currentPeerName = try { device.name ?: "Linux PC" } catch (_: Exception) { "Linux PC" }

                        updateState(SyncConnectionState.Connected(currentPeerName, pcMac))

                        // Send Hello
                        val hello = Protocol.Message.Hello(
                            src = config.deviceId,
                            name = config.deviceName
                        )
                        outputStream?.write(Protocol.encodeFrame(hello))
                        outputStream?.flush()

                        // Flush outbox in ts order
                        flushOutbox()

                        // Listen on socket
                        runReceiveLoop(socket.inputStream)
                    }
                } catch (_: Exception) {
                    // Disconnected or connection failed
                } finally {
                    cleanSocket()
                }

                delay(delayTime)
            }
        }
    }

    private fun flushOutbox() {
        synchronized(outbox) {
            outbox.sortBy { it.ts }
            for (clip in outbox) {
                sendPendingFrame(clip)
            }
        }
    }

    private fun runReceiveLoop(inputStream: InputStream) {
        val framer = Protocol.StreamFramer()
        val buffer = ByteArray(4096)

        while (serviceScope.isActive) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break

            val messages = framer.push(buffer, bytesRead)
            for (msg in messages) {
                handleIncomingMessage(msg)
            }
        }
    }

    private fun handleIncomingMessage(msg: Protocol.Message) {
        when (msg) {
            is Protocol.Message.Hello -> {
                currentPeerName = msg.name
                updateNotification("Connected to $currentPeerName")
            }
            is Protocol.Message.Ack -> {
                // Remove acked message from outbox
                synchronized(outbox) {
                    val it = outbox.iterator()
                    while (it.hasNext()) {
                        if (it.next().id == msg.forId) {
                            it.remove()
                            break
                        }
                    }
                }
            }
            is Protocol.Message.Clip -> {
                serviceScope.launch(Dispatchers.IO) {
                    handleRemoteClip(msg)
                }
            }
            is Protocol.Message.Bye -> {
                cleanSocket()
            }
        }
    }

    private suspend fun handleRemoteClip(remoteClip: Protocol.Message.Clip) {
        val config = syncPreferences.getConfig()

        // 1. Send Ack immediately for receipt confirmation
        val ack = Protocol.Message.Ack(
            src = config.deviceId,
            forId = remoteClip.id
        )
        sendPendingFrame(ack)

        // 2. Seen-ID check
        synchronized(seenIds) {
            if (seenIds.contains(remoteClip.id)) return
            if (seenIds.size >= Protocol.SEEN_CACHE_SIZE) {
                val first = seenIds.iterator().next()
                seenIds.remove(first)
            }
            seenIds.add(remoteClip.id)
        }

        // 3. Database existence check
        if (clipRepository.exists(remoteClip.id)) return

        // 4. MIME check
        if (remoteClip.mime != "text/plain") return

        // 5. Conflict Resolution: Check against recent pending local copy
        var isLoser = false
        val local = clipboardCaptureManager.getLastLocalClip()
        if (local != null && Math.abs(remoteClip.ts - local.ts) < Protocol.CONFLICT_WINDOW_MS) {
            val localWon = Protocol.isLocalWinner(local.ts, local.src, remoteClip.ts, remoteClip.src)
            if (localWon) {
                // Local copy won; remote is conflict loser
                isLoser = true
            }
        }

        // 6. Save remote clip to Room
        clipRepository.insertClip(
            ClipEntity(
                id = remoteClip.id,
                text = remoteClip.text,
                ts = remoteClip.ts,
                src = remoteClip.src,
                direction = "remote",
                conflictLoser = isLoser
            )
        )

        // 7. If not a conflict loser, apply to local clipboard with suppression
        if (!isLoser) {
            clipboardCaptureManager.applyRemoteClip(remoteClip.text)
        }
    }

    private fun cleanSocket() {
        try { outputStream?.close() } catch (_: Exception) {}
        try { activeSocket?.close() } catch (_: Exception) {}
        outputStream = null
        activeSocket = null
    }

    private fun updateState(newState: SyncConnectionState) {
        _stateFlow.value = newState
        when (newState) {
            is SyncConnectionState.Connected -> updateNotification("Connected (${newState.peerName})")
            is SyncConnectionState.Retrying -> updateNotification("Retrying in ${newState.nextRetrySec}s (attempt ${newState.attempt})")
            is SyncConnectionState.Offline -> updateNotification("Offline — ${newState.reason}")
            is SyncConnectionState.Syncing -> updateNotification("Syncing clipboard...")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard Sync Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time connection status with PC"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun computeForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0

        val hasBtPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return if (hasBtPermission) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }
    }

    private fun startForegroundWithNotification(statusText: String) {
        val notification = buildNotification(statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = computeForegroundServiceType()
            try {
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } catch (_: SecurityException) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } catch (_: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ClipSyncService::class.java).apply { action = ACTION_SYNC_NOW },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Synqvia")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Sync to PC", syncIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        try {
            contentResolver.unregisterContentObserver(imeSettingsObserver)
        } catch (_: Exception) {}
        clipboardCaptureManager.stopMonitoring()
        clipboardCaptureManager.setOutboundClipListener(null)
        connectionLoopJob?.cancel()
        cleanSocket()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_INJECT = "com.github.premtechworks.synqvia.action.INJECT"
        const val ACTION_SYNC_NOW = "com.github.premtechworks.synqvia.action.SYNC_NOW"
        const val ACTION_RECONNECT = "com.github.premtechworks.synqvia.action.RECONNECT"
        const val EXTRA_TEXT = "extra_text"

        private const val CHANNEL_ID = "synqvia_channel"
        private const val NOTIFICATION_ID = 1001

        private val _stateFlow = MutableStateFlow<SyncConnectionState>(SyncConnectionState.Offline("Initializing..."))
        val connectionState: StateFlow<SyncConnectionState> = _stateFlow.asStateFlow()

        private val _isDefaultImeFlow = MutableStateFlow(false)
        val isDefaultImeState: StateFlow<Boolean> = _isDefaultImeFlow.asStateFlow()
    }
}
