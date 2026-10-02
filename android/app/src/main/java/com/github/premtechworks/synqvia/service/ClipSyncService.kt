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
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.SynqviaApp
import com.github.premtechworks.synqvia.clipboard.ClipboardCaptureManager
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.data.ClipRepository
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.ime.DefaultImeDetector
import com.github.premtechworks.synqvia.protocol.Protocol
import com.github.premtechworks.synqvia.receiver.NotificationActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
    data object Stopped : SyncConnectionState()
    data class Connecting(val attempt: Int, val maxAttempts: Int, val nextRetrySec: Int = 0) : SyncConnectionState()
    data class Connected(val peerName: String, val mac: String) : SyncConnectionState()
    data object Failed : SyncConnectionState()
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
    @Volatile
    private var connectingSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // Outbox queue for ack-tracked reliable delivery
    private val outbox = Collections.synchronizedList(LinkedList<Protocol.Message.Clip>())

    // Seen IDs LRU cache
    private val seenIds = Collections.synchronizedSet(LinkedHashSet<String>())

    private var connectionLoopJob: Job? = null
    private var currentPeerName: String = "Linux PC"

    @Volatile
    private var isStoppedByUser = false

    var retryPolicy: RetryPolicy = RetryPolicy()

    private val imeSettingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            evaluateClipboardMonitoring()
        }
    }

    fun evaluateClipboardMonitoring() {
        if (isStoppedByUser || syncPreferences.isUserStopped) {
            clipboardCaptureManager.stopMonitoring()
            _isDefaultImeFlow.value = false
            return
        }
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
        syncPreferences = app.container.syncPreferences

        if (syncPreferences.isUserStopped) {
            _isRunning = false
            setStoppedState()
            stopSelf()
            return
        }

        _isRunning = true
        clipRepository = app.container.clipRepository
        clipboardCaptureManager = app.container.clipboardCaptureManager
        defaultImeDetector = app.container.defaultImeDetector

        // Listen for outbound clips from shared pipeline
        clipboardCaptureManager.setOutboundClipListener { clip ->
            queueAndSendClip(clip)
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        createNotificationChannel()
        startForegroundWithNotification(getString(R.string.status_connecting, 1, retryPolicy.maxAttempts))

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
        if (syncPreferences.isUserStopped && intent?.action != ACTION_START && intent?.action != ACTION_RECONNECT) {
            handleStopAction()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_STOP -> {
                handleStopAction()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                isStoppedByUser = false
                syncPreferences.isUserStopped = false
                evaluateClipboardMonitoring()
                startConnectionLoop()
            }
            ACTION_RECONNECT -> {
                isStoppedByUser = false
                syncPreferences.isUserStopped = false
                evaluateClipboardMonitoring()
                startConnectionLoop()
            }
            ACTION_INJECT -> {
                evaluateClipboardMonitoring()
                val injectedText = intent.getStringExtra(EXTRA_TEXT) ?: ""
                serviceScope.launch(Dispatchers.IO) {
                    clipboardCaptureManager.captureLocalClip(injectedText, isExplicit = true)
                }
            }
            ACTION_SYNC_NOW -> {
                evaluateClipboardMonitoring()
                serviceScope.launch(Dispatchers.IO) {
                    val selection = SelectionCache.getFreshSelection()
                    if (!selection.isNullOrBlank()) {
                        clipboardCaptureManager.captureLocalClip(selection, isExplicit = true)
                    } else {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val text = cm?.primaryClip?.getItemAt(0)?.coerceToText(this@ClipSyncService)?.toString() ?: ""
                        if (text.isNotBlank()) {
                            clipboardCaptureManager.captureLocalClip(text, isExplicit = true)
                        }
                    }
                }
            }
            else -> {
                evaluateClipboardMonitoring()
                if (!isStoppedByUser && !syncPreferences.isUserStopped && connectionLoopJob?.isActive != true) {
                    startConnectionLoop()
                }
            }
        }
        return if (syncPreferences.isUserStopped) START_NOT_STICKY else START_STICKY
    }

    /**
     * Handles ACTION_STOP strictly in the specified order:
     * 1. Set cancelled/stop flag checked by connect loop
     * 2. Cancel coroutine job / interrupt blocking connect and recv
     * 3. Close the RFCOMM socket
     * 4. Unregister ContentObserver and stop clipboard monitoring
     * 5. Call stopForeground(STOP_FOREGROUND_REMOVE)
     * 6. Cancel the notification
     * 7. Call stopSelf()
     */
    private fun handleStopAction() {
        // 1. Set cancelled/stop flag checked by connect loop
        isStoppedByUser = true
        syncPreferences.isUserStopped = true

        // 2. Cancel coroutine job / interrupt blocking connect and recv
        connectionLoopJob?.cancel()

        // 3. Close the RFCOMM socket
        cleanSocket()

        // 4. Unregister ContentObserver and stop clipboard monitoring
        try {
            contentResolver.unregisterContentObserver(imeSettingsObserver)
        } catch (_: Exception) {}
        try {
            clipboardCaptureManager.stopMonitoring()
            clipboardCaptureManager.setOutboundClipListener(null)
        } catch (_: Exception) {}

        // 5. Call stopForeground(STOP_FOREGROUND_REMOVE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        // 6. Cancel the notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)

        // Update state to Stopped
        updateState(SyncConnectionState.Stopped)

        // 7. Call stopSelf()
        _isRunning = false
        stopSelf()
    }

    private fun handleConnectionFailed() {
        cleanSocket()
        updateState(SyncConnectionState.Failed)

        if (isStoppedByUser || syncPreferences.isUserStopped) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
            stopSelf()
            return
        }

        // Convert foreground notification to a non-ongoing, dismissible notification
        val notification = buildFailedNotification()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        nm?.notify(NOTIFICATION_ID, notification)

        // Stop the service until the user acts
        stopSelf()
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
        isStoppedByUser = false
        connectionLoopJob = serviceScope.launch(Dispatchers.IO) {
            var failedAttempts = 0

            while (isActive && !isStoppedByUser && !syncPreferences.isUserStopped) {
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

                if (isStoppedByUser || syncPreferences.isUserStopped || !isActive) {
                    break
                }

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

                if (isStoppedByUser || syncPreferences.isUserStopped || !isActive) {
                    break
                }

                // Count only failed RFCOMM connect attempts; configuration errors are not counted.
                val currentAttemptNumber = failedAttempts + 1
                val delayTime = retryPolicy.computeDelayWithJitter(failedAttempts)
                val delaySeconds = (delayTime / 1000).toInt().coerceAtLeast(1)

                updateState(
                    SyncConnectionState.Connecting(
                        attempt = currentAttemptNumber,
                        maxAttempts = retryPolicy.maxAttempts,
                        nextRetrySec = if (failedAttempts > 0) delaySeconds else 0
                    )
                )

                var connected = false
                try {
                    val device: BluetoothDevice = adapter.getRemoteDevice(pcMac)
                    adapter.cancelDiscovery()

                    var socket: BluetoothSocket? = null
                    try {
                        socket = device.createRfcommSocketToServiceRecord(Protocol.SERVICE_UUID)
                        connectingSocket = socket
                        socket.connect()
                    } catch (_: Exception) {
                        try { socket?.close() } catch (_: Exception) {}
                        connectingSocket = null
                        if (isStoppedByUser || syncPreferences.isUserStopped || !isActive) {
                            break
                        }
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, config.channel) as BluetoothSocket
                        connectingSocket = socket
                        socket.connect()
                    } finally {
                        connectingSocket = null
                    }

                    if (isStoppedByUser || syncPreferences.isUserStopped || !isActive) {
                        try { socket?.close() } catch (_: Exception) {}
                        break
                    }

                    if (socket != null && socket.isConnected) {
                        activeSocket = socket
                        outputStream = socket.outputStream
                        // Reset attempt counter on a successful connection
                        failedAttempts = 0
                        connected = true
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

                        // Listen on socket until disconnected or cancelled
                        runReceiveLoop(socket.inputStream)
                    }
                } catch (_: Exception) {
                    // Disconnected or connection failed
                } finally {
                    cleanSocket()
                }

                if (isStoppedByUser || syncPreferences.isUserStopped || !isActive) {
                    break
                }

                if (connected) {
                    // A connection that drops after having been established starts a fresh retry cycle (attempt = 0)
                    failedAttempts = 0
                    delay(1000L)
                    continue
                } else {
                    failedAttempts++
                    if (failedAttempts >= retryPolicy.maxAttempts) {
                        // Max consecutive failures reached: terminal failed state
                        handleConnectionFailed()
                        break
                    }
                    if (isStoppedByUser || syncPreferences.isUserStopped || !isActive) {
                        break
                    }
                    // Wait with exponential backoff + jitter before next attempt
                    updateState(
                        SyncConnectionState.Connecting(
                            attempt = failedAttempts,
                            maxAttempts = retryPolicy.maxAttempts,
                            nextRetrySec = delaySeconds
                        )
                    )
                    delay(delayTime)
                }
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

        while (serviceScope.isActive && !isStoppedByUser) {
            serviceScope.ensureActive()
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

        // 7. If not a conflict loser, apply to local clipboard with suppression only when autoSync is ON
        if (!isLoser) {
            if (syncPreferences.autoSync) {
                clipboardCaptureManager.applyRemoteClip(remoteClip.text)
                lastAppliedRemoteText = remoteClip.text
            }
        }
    }

    @Volatile
    private var lastAppliedRemoteText: String? = null

    private fun cleanSocket() {
        val hadActiveConnection = activeSocket != null
        try { outputStream?.close() } catch (_: Exception) {}
        try { activeSocket?.close() } catch (_: Exception) {}
        try { connectingSocket?.close() } catch (_: Exception) {}
        outputStream = null
        activeSocket = null
        connectingSocket = null
        if (hadActiveConnection) {
            handleDisconnectClipboardClear()
        }
    }

    private fun handleDisconnectClipboardClear() {
        if (!syncPreferences.clearOnDisconnect) return
        val lastRemote = lastAppliedRemoteText ?: return
        serviceScope.launch(Dispatchers.Main) {
            try {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager ?: return@launch
                val currentText = cm.primaryClip?.getItemAt(0)?.coerceToText(this@ClipSyncService)?.toString()
                if (currentText == lastRemote) {
                    clipboardCaptureManager.copyToClipboardWithoutBroadcast("")
                    lastAppliedRemoteText = null
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateState(newState: SyncConnectionState) {
        _stateFlow.value = newState
        when (newState) {
            is SyncConnectionState.Connected -> updateNotification(getString(R.string.status_connected, newState.peerName))
            is SyncConnectionState.Connecting -> {
                if (newState.nextRetrySec > 0) {
                    updateNotification(getString(R.string.status_retrying_with_delay, newState.nextRetrySec, newState.attempt, newState.maxAttempts))
                } else {
                    updateNotification(getString(R.string.status_connecting, newState.attempt, newState.maxAttempts))
                }
            }
            is SyncConnectionState.Offline -> updateNotification(getString(R.string.status_offline, newState.reason))
            is SyncConnectionState.Syncing -> updateNotification(getString(R.string.status_syncing))
            is SyncConnectionState.Failed -> {
                // Notification handled by handleConnectionFailed
            }
            is SyncConnectionState.Stopped -> {
                // Notification removed on stop
            }
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
        if (isStoppedByUser || syncPreferences.isUserStopped) return
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

        val syncIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_SYNC_NOW },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.action_send_to_pc), syncIntent)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.action_stop), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildFailedNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tryAgainIntent = PendingIntent.getBroadcast(
            this,
            3,
            Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_RECONNECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.connection_failed_try_again))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.action_try_again), tryAgainIntent)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.action_stop), stopIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        isStoppedByUser = true
        try {
            contentResolver.unregisterContentObserver(imeSettingsObserver)
        } catch (_: Exception) {}
        try {
            clipboardCaptureManager.stopMonitoring()
            clipboardCaptureManager.setOutboundClipListener(null)
        } catch (_: Exception) {}
        connectionLoopJob?.cancel()
        cleanSocket()
        serviceJob.cancel()
        _isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
        if (_stateFlow.value !is SyncConnectionState.Failed) {
            _stateFlow.value = SyncConnectionState.Stopped
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.github.premtechworks.synqvia.action.START"
        const val ACTION_STOP = "com.github.premtechworks.synqvia.action.STOP"
        const val ACTION_INJECT = "com.github.premtechworks.synqvia.action.INJECT"
        const val ACTION_SYNC_NOW = "com.github.premtechworks.synqvia.action.SYNC_NOW"
        const val ACTION_RECONNECT = "com.github.premtechworks.synqvia.action.RECONNECT"
        const val EXTRA_TEXT = "extra_text"

        const val CHANNEL_ID = "synqvia_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        private var _isRunning = false
        val isRunning: Boolean get() = _isRunning

        private val _stateFlow = MutableStateFlow<SyncConnectionState>(SyncConnectionState.Stopped)
        val connectionState: StateFlow<SyncConnectionState> = _stateFlow.asStateFlow()

        private val _isDefaultImeFlow = MutableStateFlow(false)
        val isDefaultImeState: StateFlow<Boolean> = _isDefaultImeFlow.asStateFlow()

        fun setStoppedState() {
            _stateFlow.value = SyncConnectionState.Stopped
        }
    }
}
