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

enum class ClipFilter {
    ALL, SENT, RECEIVED, CONFLICTS, PINNED
}

enum class MainTab(val title: String) {
    SYNC("Sync"),
    HISTORY("History"),
    SETUP("Setup"),
    SETTINGS("Settings")
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
    val config: StateFlow<SyncConfig> = syncPreferences.configFlow

    private val _isDefaultIme = MutableStateFlow(defaultImeDetector.isSynqviaDefaultIme())
    val isDefaultIme: StateFlow<Boolean> = _isDefaultIme.asStateFlow()

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

    // Reactive clips stream with real-time search & filter
    val filteredClips: StateFlow<List<ClipEntity>> = combine(
        _searchQuery.flatMapLatest { query -> clipRepository.searchClips(query) },
        _filter
    ) { clips, currentFilter ->
        when (currentFilter) {
            ClipFilter.ALL -> clips
            ClipFilter.SENT -> clips.filter { it.isLocal }
            ClipFilter.RECEIVED -> clips.filter { it.isRemote }
            ClipFilter.CONFLICTS -> clips.filter { it.conflictLoser }
            ClipFilter.PINNED -> clips.filter { it.pinned }
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

    fun addLog(level: String, message: String) {
        val newItem = LogItem(level = level, message = message)
        val current = _diagnosticLogs.value.toMutableList()
        if (current.size > 200) current.removeAt(0)
        current.add(newItem)
        _diagnosticLogs.value = current
    }

    fun syncNow() {
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
            app.startService(intent)
            _userMessage.value = "Broadcasting current clipboard to PC..."
            addLog("SUCCESS", "Manual sync triggered: ${textToSync.take(30)}...")
        } else {
            _userMessage.value = "Clipboard is currently empty"
        }
    }

    fun sendTestClip() {
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
        app.startService(intent)

        _userMessage.value = "Sent test message to PC!"
        addLog("FRAME", "Sent test frame [len=${testText.length}]")
    }

    fun resendClip(clip: ClipEntity) {
        val app = getApplication<Application>()
        // Re-copy to local clipboard
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Synqvia", clip.text))

        // Rebroadcast to PC
        val intent = Intent(app, ClipSyncService::class.java).apply {
            action = ClipSyncService.ACTION_INJECT
            putExtra(ClipSyncService.EXTRA_TEXT, clip.text)
        }
        app.startService(intent)

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

    fun deleteClip(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            clipRepository.deleteClip(id)
            addLog("INFO", "Deleted clip $id")
        }
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
        val app = getApplication<Application>()
        val intent = Intent(app, ClipSyncService::class.java).apply {
            action = ClipSyncService.ACTION_RECONNECT
        }
        app.startService(intent)
        addLog("INFO", "Forced connection restart requested")
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
