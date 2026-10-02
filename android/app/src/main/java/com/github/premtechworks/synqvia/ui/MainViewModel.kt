package com.github.premtechworks.synqvia.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.data.ClipRepository
import com.github.premtechworks.synqvia.data.SyncConfig
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.data.SyncStats
import com.github.premtechworks.synqvia.di.AppContainer
import com.github.premtechworks.synqvia.ime.AndroidDefaultImeDetector
import com.github.premtechworks.synqvia.ime.DefaultImeDetector
import com.github.premtechworks.synqvia.service.ClipSyncService
import com.github.premtechworks.synqvia.service.SelectionCache
import com.github.premtechworks.synqvia.service.SyncConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

data class UndoDeleteEvent(
    val clipId: String,
    val message: String = "Clip deleted"
)

enum class ClipFilter {
    ALL, SENT, RECEIVED, CONFLICTS, PINNED
}

enum class MainTab(val title: String) {
    SYNC("Sync"),
    HISTORY("History"),
    SETUP("Setup"),
    SETTINGS("Settings")
}

object AppRoutes {
    const val ONBOARDING = "onboarding"
    const val PAIR = "pair"
    const val MAIN = "main"
    const val IME_SETTINGS = "ime_settings"

    fun resolveStartDestination(onboardingDone: Boolean, pcMac: String?): String {
        return when {
            !onboardingDone -> ONBOARDING
            pcMac.isNullOrBlank() -> PAIR
            else -> MAIN
        }
    }
}

data class LogItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // "INFO", "SUCCESS", "WARN", "FRAME"
    val message: String
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

class MainViewModel(
    application: Application,
    private val clipRepository: ClipRepository,
    private val syncPreferences: SyncPreferences,
    private val defaultImeDetector: DefaultImeDetector = AndroidDefaultImeDetector(application)
) : AndroidViewModel(application) {

    val connectionState: StateFlow<SyncConnectionState> = ClipSyncService.connectionState
    val isUserStopped: Boolean get() = syncPreferences.isUserStopped
    val isServiceEnabled: StateFlow<Boolean> = connectionState
        .map { it !is SyncConnectionState.Stopped && !syncPreferences.isUserStopped }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = !syncPreferences.isUserStopped && connectionState.value !is SyncConnectionState.Stopped
        )
    val config: StateFlow<SyncConfig> = syncPreferences.configFlow
    val appIcon: StateFlow<String> = syncPreferences.appIconFlow

    val autoSync: StateFlow<Boolean> = syncPreferences.autoSyncFlow
    val syncTextOnly: StateFlow<Boolean> = syncPreferences.syncTextOnlyFlow
    val clearOnDisconnect: StateFlow<Boolean> = syncPreferences.clearOnDisconnectFlow
    val themeMode: StateFlow<String> = syncPreferences.themeModeFlow
    val dynamicColor: StateFlow<Boolean> = syncPreferences.dynamicColorFlow
    val reduceMotionFollowSystem: StateFlow<Boolean> = syncPreferences.reduceMotionFollowSystemFlow
    val hapticFeedback: StateFlow<Boolean> = syncPreferences.hapticFeedbackFlow

    private val _selectedClipForDetail = MutableStateFlow<ClipEntity?>(null)
    val selectedClipForDetail: StateFlow<ClipEntity?> = _selectedClipForDetail.asStateFlow()

    fun selectClipForDetail(clip: ClipEntity?) {
        _selectedClipForDetail.value = clip
    }

    val onboardingDone: StateFlow<Boolean> = syncPreferences.onboardingDoneFlow

    private val _currentRoute = MutableStateFlow(
        AppRoutes.resolveStartDestination(
            onboardingDone = syncPreferences.onboardingDone,
            pcMac = syncPreferences.getConfig().pcMac
        )
    )
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private var previousRoute: String? = null

    fun navigateTo(route: String) {
        previousRoute = _currentRoute.value
        _currentRoute.value = route
    }

    fun navigateBack() {
        val prev = previousRoute
        if (prev != null && prev != _currentRoute.value) {
            _currentRoute.value = prev
        } else {
            if (!syncPreferences.onboardingDone) {
                _currentRoute.value = AppRoutes.ONBOARDING
            } else {
                _currentRoute.value = AppRoutes.MAIN
            }
        }
    }

    fun setOnboardingDone(done: Boolean = true) {
        syncPreferences.onboardingDone = done
    }

    fun setAutoSync(enabled: Boolean) {
        syncPreferences.autoSync = enabled
        addLog("INFO", "Auto Sync ${if (enabled) "enabled" else "disabled"}")
    }

    fun setSyncTextOnly(enabled: Boolean) {
        if (!enabled) {
            syncPreferences.syncTextOnly = true
            _userMessage.value = "Non-text sync coming in a future update"
        } else {
            syncPreferences.syncTextOnly = true
        }
    }

    fun setClearOnDisconnect(enabled: Boolean) {
        syncPreferences.clearOnDisconnect = enabled
        addLog("INFO", "Clear on Device Disconnect ${if (enabled) "enabled" else "disabled"}")
    }

    fun setThemeMode(mode: String) {
        syncPreferences.themeMode = mode
        addLog("INFO", "Theme mode set to: $mode")
    }

    fun setDynamicColor(enabled: Boolean) {
        syncPreferences.dynamicColor = enabled
        addLog("INFO", "Dynamic color ${if (enabled) "enabled" else "disabled"}")
    }

    fun setReduceMotionFollowSystem(enabled: Boolean) {
        syncPreferences.reduceMotionFollowSystem = enabled
        addLog("INFO", "Reduce Motion (follow system) ${if (enabled) "enabled" else "disabled"}")
    }

    fun setHapticFeedback(enabled: Boolean) {
        syncPreferences.hapticFeedback = enabled
        addLog("INFO", "Haptic Feedback ${if (enabled) "enabled" else "disabled"}")
    }

    private var channelDebounceJob: Job? = null

    fun updateChannel(newChannel: Int) {
        val currentConfig = config.value
        val clamped = newChannel.coerceIn(1, 30)
        if (clamped == currentConfig.channel) return
        syncPreferences.updateConfig(
            pcMac = currentConfig.pcMac,
            channel = clamped,
            historyCap = currentConfig.historyCap,
            deviceName = currentConfig.deviceName
        )
        channelDebounceJob?.cancel()
        channelDebounceJob = viewModelScope.launch {
            delay(600L)
            _userMessage.value = "Saved. Reconnecting…"
            addLog("INFO", "RFCOMM Channel changed to $clamped. Reconnecting…")
            reconnect()
        }
    }

    fun updateMac(newMac: String): Boolean {
        val cleanMac = newMac.trim().uppercase(Locale.ROOT)
        if (!SyncPreferences.isValidMac(cleanMac) && cleanMac.isNotBlank()) {
            return false
        }
        val currentConfig = config.value
        val macChanged = currentConfig.pcMac != cleanMac
        syncPreferences.updateConfig(
            pcMac = cleanMac,
            channel = currentConfig.channel,
            historyCap = currentConfig.historyCap,
            deviceName = currentConfig.deviceName
        )
        if (macChanged) {
            _userMessage.value = "Saved. Reconnecting…"
            addLog("INFO", "PC MAC updated to $cleanMac. Reconnecting…")
            reconnect()
        }
        return true
    }

    fun updateDeviceName(newName: String) {
        val currentConfig = config.value
        val trimmed = newName.trim().take(48)
        if (trimmed == currentConfig.deviceName) return
        syncPreferences.updateConfig(
            pcMac = currentConfig.pcMac,
            channel = currentConfig.channel,
            historyCap = currentConfig.historyCap,
            deviceName = trimmed
        )
        addLog("INFO", "Device broadcast name updated to: $trimmed")
    }

    fun updateHistoryCapacity(newCap: Int) {
        val currentConfig = config.value
        val clamped = newCap.coerceAtLeast(0)
        if (clamped == currentConfig.historyCap) return
        syncPreferences.updateConfig(
            pcMac = currentConfig.pcMac,
            channel = currentConfig.channel,
            historyCap = clamped,
            deviceName = currentConfig.deviceName
        )
        addLog("INFO", "History capacity updated to: ${if (clamped == 0) "Unlimited" else "$clamped items"}")
    }

    fun setAppIcon(icon: String) {
        syncPreferences.appIcon = icon
        AppIconManager.setAppIcon(getApplication(), icon)
        val iconLabel = if (icon == AppIconManager.ICON_THEMED) "Themed / Dynamic" else "Classic"
        _userMessage.value = "Launcher icon switched to $iconLabel"
        addLog("INFO", "Switched app launcher icon to: $iconLabel")
    }

    private var lastToggleTime = 0L
    private val toggleDebounceMs = 500L

    private val _isDefaultIme = MutableStateFlow(defaultImeDetector.isSynqviaDefaultIme())
    val isDefaultIme: StateFlow<Boolean> = _isDefaultIme.asStateFlow()

    val isKeyboardWarningVisible: StateFlow<Boolean> = combine(
        _isDefaultIme,
        syncPreferences.keyboardWarningSnoozedUntilFlow
    ) { isDefault, snoozedUntil ->
        !isDefault && System.currentTimeMillis() >= snoozedUntil
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = !_isDefaultIme.value && !syncPreferences.isKeyboardWarningSnoozed()
    )

    fun snoozeKeyboardWarning() {
        syncPreferences.snoozeKeyboardWarning(days = 7)
    }

    private val imeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            refreshImeStatus()
        }
    }

    init {
        try {
            application.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
                false,
                imeObserver
            )
        } catch (_: Exception) {}
    }

    fun refreshImeStatus() {
        _isDefaultIme.value = defaultImeDetector.isSynqviaDefaultIme()
    }

    override fun onCleared() {
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(imeObserver)
        } catch (_: Exception) {}
        super.onCleared()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(ClipFilter.ALL)
    val filter: StateFlow<ClipFilter> = _filter.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.SYNC)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<LogItem>>(
        listOf(
            LogItem(level = "INFO", message = "Synqvia engine initialized"),
            LogItem(level = "INFO", message = "Using SPP UUID 7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002")
        )
    )
    val diagnosticLogs: StateFlow<List<LogItem>> = _diagnosticLogs.asStateFlow()

    // Soft-delete tracking & Undo state
    private val _pendingDeletedClipIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingDeletedClipIds: StateFlow<Set<String>> = _pendingDeletedClipIds.asStateFlow()

    private val _undoDeleteEvent = MutableStateFlow<UndoDeleteEvent?>(null)
    val undoDeleteEvent: StateFlow<UndoDeleteEvent?> = _undoDeleteEvent.asStateFlow()

    private val pendingDeleteJobs = ConcurrentHashMap<String, Job>()

    // Loading state for skeleton placeholder on first composition
    val isClipsLoading: StateFlow<Boolean> = _searchQuery.flatMapLatest { query ->
        clipRepository.searchClips(query)
    }.map { false }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = true
    )

    // Reactive clips stream with real-time search & filter (excluding soft-deleted clips)
    val filteredClips: StateFlow<List<ClipEntity>> = combine(
        _searchQuery.flatMapLatest { query -> clipRepository.searchClips(query) },
        _filter,
        _pendingDeletedClipIds
    ) { clips, currentFilter, pendingDeleted ->
        val visibleClips = if (pendingDeleted.isEmpty()) clips else clips.filter { it.id !in pendingDeleted }
        when (currentFilter) {
            ClipFilter.ALL -> visibleClips
            ClipFilter.SENT -> visibleClips.filter { it.isLocal }
            ClipFilter.RECEIVED -> visibleClips.filter { it.isRemote }
            ClipFilter.CONFLICTS -> visibleClips.filter { it.conflictLoser }
            ClipFilter.PINNED -> visibleClips.filter { it.pinned }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _imeExpiryHours = MutableStateFlow(syncPreferences.imeExpiryHours)
    val imeExpiryHours: StateFlow<Int> = _imeExpiryHours.asStateFlow()

    fun updateImeExpiryHours(hours: Int) {
        val validated = hours.coerceAtLeast(0)
        syncPreferences.imeExpiryHours = validated
        _imeExpiryHours.value = validated
        addLog("INFO", "IME history expiry set to ${if (validated == 0) "Never" else "$validated hour(s)"}")
    }

    val syncStats: StateFlow<SyncStats> = clipRepository.syncStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SyncStats(0, 0, 0, 0)
    )

    fun setTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(newFilter: ClipFilter) {
        _filter.value = newFilter
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun addLog(level: String, message: String) {
        val newItem = LogItem(level = level, message = message)
        val current = _diagnosticLogs.value.toMutableList()
        if (current.size > 200) current.removeAt(0)
        current.add(newItem)
        _diagnosticLogs.value = current
    }

    fun clearLogs() {
        _diagnosticLogs.value = emptyList()
    }

    fun syncNow() {
        if (syncPreferences.isUserStopped) {
            _userMessage.value = "Sync service is stopped. Start service first."
            return
        }
        val app = getApplication<Application>()
        val selection = SelectionCache.getFreshSelection()
        val textToSync = if (!selection.isNullOrBlank()) {
            selection
        } else {
            val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.primaryClip?.getItemAt(0)?.coerceToText(app)?.toString() ?: ""
        }

        if (textToSync.isNotBlank()) {
            val intent = Intent(app, ClipSyncService::class.java).apply {
                action = ClipSyncService.ACTION_INJECT
                putExtra(ClipSyncService.EXTRA_TEXT, textToSync)
            }
            try {
                app.startService(intent)
            } catch (_: Exception) {}
            _userMessage.value = "Broadcasting current clipboard to PC..."
            addLog("SUCCESS", "Manual sync triggered: ${textToSync.take(30)}...")
        } else {
            _userMessage.value = "Clipboard is currently empty"
        }
    }

    fun sendTestClip() {
        if (syncPreferences.isUserStopped) {
            _userMessage.value = "Sync service is stopped. Start service first."
            return
        }
        val app = getApplication<Application>()
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val testText = "Hello from Android! Sync test at $timeStr ✓"

        // Set to local clipboard and trigger service
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Synqvia", testText))

        val intent = Intent(app, ClipSyncService::class.java).apply {
            action = ClipSyncService.ACTION_INJECT
            putExtra(ClipSyncService.EXTRA_TEXT, testText)
        }
        try {
            app.startService(intent)
        } catch (_: Exception) {}

        _userMessage.value = "Sent test message to PC!"
        addLog("FRAME", "Sent test frame [len=${testText.length}]")
    }

    fun resendClip(clip: ClipEntity) {
        if (syncPreferences.isUserStopped) {
            _userMessage.value = "Sync service is stopped. Start service first."
            return
        }
        val app = getApplication<Application>()
        // Re-copy to local clipboard
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Synqvia", clip.text))

        // Rebroadcast to PC
        val intent = Intent(app, ClipSyncService::class.java).apply {
            action = ClipSyncService.ACTION_INJECT
            putExtra(ClipSyncService.EXTRA_TEXT, clip.text)
        }
        try {
            app.startService(intent)
        } catch (_: Exception) {}

        _userMessage.value = "Copied to clipboard and re-sent to PC!"
        addLog("INFO", "Re-broadcasted clip ${clip.id.take(8)}")
    }

    fun copyToClipboardOnly(text: String) {
        val app = getApplication<Application>()
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Synqvia", text))
        _userMessage.value = "Copied to clipboard"
    }

    fun togglePin(id: String, pinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            clipRepository.setPinned(id, pinned)
            addLog("INFO", "Toggled pin for $id to $pinned")
        }
    }

    fun deleteClip(id: String, timeoutMillis: Long = 6000L) {
        // Cancel existing pending job for this clip if any
        pendingDeleteJobs.remove(id)?.cancel()
        _pendingDeletedClipIds.update { it + id }
        _undoDeleteEvent.value = UndoDeleteEvent(clipId = id, message = "Clip deleted")
        addLog("INFO", "Soft-deleted clip $id (undo window active)")

        val job = viewModelScope.launch(Dispatchers.IO) {
            delay(timeoutMillis)
            // Timeout expired, commit deletion to repository
            try {
                clipRepository.deleteClip(id)
                addLog("INFO", "Committed deletion of clip $id")
            } finally {
                _pendingDeletedClipIds.update { it - id }
                pendingDeleteJobs.remove(id)
                if (_undoDeleteEvent.value?.clipId == id) {
                    _undoDeleteEvent.value = null
                }
            }
        }
        pendingDeleteJobs[id] = job
    }

    fun undoDelete(id: String? = null) {
        val targetId = id ?: _undoDeleteEvent.value?.clipId
        if (targetId != null) {
            val job = pendingDeleteJobs.remove(targetId)
            job?.cancel()
            _pendingDeletedClipIds.update { it - targetId }
            if (_undoDeleteEvent.value?.clipId == targetId) {
                _undoDeleteEvent.value = null
            }
            addLog("INFO", "Restored clip $targetId via Undo")
        }
    }

    fun clearUndoDeleteEvent() {
        _undoDeleteEvent.value = null
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            clipRepository.clearAll()
            _userMessage.value = "Clipboard history cleared"
            addLog("WARN", "History database cleared")
        }
    }

    fun saveSettings(pcMac: String, channel: Int, historyCap: Int, deviceName: String): Boolean {
        val saved = syncPreferences.updateConfig(
            pcMac = pcMac,
            channel = channel,
            historyCap = historyCap,
            deviceName = deviceName
        )
        if (saved) {
            _userMessage.value = "Settings saved. Reconnecting..."
            addLog("INFO", "Updated config: PC MAC=$pcMac, Channel=$channel, Cap=$historyCap")
            reconnect()
        }
        return saved
    }

    fun reconnect() {
        syncPreferences.isUserStopped = false
        val app = getApplication<Application>()
        val intent = Intent(app, ClipSyncService::class.java).apply {
            action = ClipSyncService.ACTION_RECONNECT
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(app, intent)
            } else {
                app.startService(intent)
            }
        } catch (_: Exception) {
            app.startService(intent)
        }
        addLog("INFO", "Forced connection restart requested")
    }

    fun toggleService(enabled: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastToggleTime < toggleDebounceMs) return
        lastToggleTime = now

        if (enabled) {
            startSync()
        } else {
            stopSync()
        }
    }

    fun startSync() {
        syncPreferences.isUserStopped = false
        val app = getApplication<Application>()
        val intent = Intent(app, ClipSyncService::class.java).apply {
            action = ClipSyncService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(app, intent)
            } else {
                app.startService(intent)
            }
        } catch (_: Exception) {
            app.startService(intent)
        }
        _userMessage.value = "Starting sync service..."
        addLog("INFO", "Sync service started by user")
    }

    fun stopSync() {
        syncPreferences.isUserStopped = true
        ClipSyncService.setStoppedState()
        val app = getApplication<Application>()
        val intent = Intent(app, ClipSyncService::class.java).apply {
            action = ClipSyncService.ACTION_STOP
        }
        try {
            app.startService(intent)
        } catch (_: Exception) {}
        _userMessage.value = "Sync service stopped"
        addLog("INFO", "Sync service stopped by user")
    }

    fun resyncServiceState() {
        if (!ClipSyncService.isRunning) {
            if (syncPreferences.isUserStopped) {
                ClipSyncService.setStoppedState()
            } else if (connectionState.value !is SyncConnectionState.Failed) {
                ClipSyncService.setStoppedState()
            }
        }
    }

    class Factory(
        private val application: Application,
        private val container: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(
                    application = application,
                    clipRepository = container.clipRepository,
                    syncPreferences = container.syncPreferences,
                    defaultImeDetector = container.defaultImeDetector
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
