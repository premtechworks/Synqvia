package com.github.premtechworks.synqvia.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.regex.Pattern

data class SyncConfig(
    val pcMac: String,
    val channel: Int,
    val historyCap: Int,
    val deviceName: String,
    val deviceId: String
)

class SyncPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    init {
        // One-time cleanup: delete stale dynamic_color preference key so old installs don't keep it
        if (prefs.contains("dynamic_color")) {
            prefs.edit().remove("dynamic_color").apply()
        }
    }

    val deviceId: String by lazy {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device"
        "android-${androidId.take(16)}"
    }

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<SyncConfig> = _configFlow.asStateFlow()

    private val _appIconFlow = MutableStateFlow(prefs.getString(KEY_APP_ICON, "classic") ?: "classic")
    val appIconFlow: StateFlow<String> = _appIconFlow.asStateFlow()

    var appIcon: String
        get() = prefs.getString(KEY_APP_ICON, "classic") ?: "classic"
        set(value) {
            prefs.edit().putString(KEY_APP_ICON, value).apply()
            _appIconFlow.value = value
        }

    // Auto Sync (default ON)
    private val _autoSyncFlow = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SYNC, true))
    val autoSyncFlow: StateFlow<Boolean> = _autoSyncFlow.asStateFlow()

    var autoSync: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()
            _autoSyncFlow.value = value
        }

    // Sync Text Only (default ON)
    private val _syncTextOnlyFlow = MutableStateFlow(prefs.getBoolean(KEY_SYNC_TEXT_ONLY, true))
    val syncTextOnlyFlow: StateFlow<Boolean> = _syncTextOnlyFlow.asStateFlow()

    var syncTextOnly: Boolean
        get() = prefs.getBoolean(KEY_SYNC_TEXT_ONLY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SYNC_TEXT_ONLY, value).apply()
            _syncTextOnlyFlow.value = value
        }

    // Clear on Device Disconnect (default OFF)
    private val _clearOnDisconnectFlow = MutableStateFlow(prefs.getBoolean(KEY_CLEAR_ON_DISCONNECT, false))
    val clearOnDisconnectFlow: StateFlow<Boolean> = _clearOnDisconnectFlow.asStateFlow()

    var clearOnDisconnect: Boolean
        get() = prefs.getBoolean(KEY_CLEAR_ON_DISCONNECT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CLEAR_ON_DISCONNECT, value).apply()
            _clearOnDisconnectFlow.value = value
        }

    // Theme Mode ("system" | "light" | "dark", default "system")
    private val _themeModeFlow = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "system") ?: "system")
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
            _themeModeFlow.value = value
        }

    // Onboarding Done (default false)
    private val _onboardingDoneFlow = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingDoneFlow: StateFlow<Boolean> = _onboardingDoneFlow.asStateFlow()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
            _onboardingDoneFlow.value = value
        }

    // Reduce Motion: follow system (default ON)
    private val _reduceMotionFollowSystemFlow = MutableStateFlow(prefs.getBoolean(KEY_REDUCE_MOTION, true))
    val reduceMotionFollowSystemFlow: StateFlow<Boolean> = _reduceMotionFollowSystemFlow.asStateFlow()

    var reduceMotionFollowSystem: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_MOTION, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REDUCE_MOTION, value).apply()
            _reduceMotionFollowSystemFlow.value = value
        }

    // Haptic Feedback (default ON)
    private val _hapticFeedbackFlow = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true))
    val hapticFeedbackFlow: StateFlow<Boolean> = _hapticFeedbackFlow.asStateFlow()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()
            _hapticFeedbackFlow.value = value
        }

    // Keyboard warning snooze timestamp (ms) - hides the warning for 7 days when 'Later' is clicked
    private val _keyboardWarningSnoozedUntilFlow = MutableStateFlow(prefs.getLong(KEY_KEYBOARD_WARNING_SNOOZED_UNTIL, 0L))
    val keyboardWarningSnoozedUntilFlow: StateFlow<Long> = _keyboardWarningSnoozedUntilFlow.asStateFlow()

    var keyboardWarningSnoozedUntil: Long
        get() = prefs.getLong(KEY_KEYBOARD_WARNING_SNOOZED_UNTIL, 0L)
        set(value) {
            prefs.edit().putLong(KEY_KEYBOARD_WARNING_SNOOZED_UNTIL, value).apply()
            _keyboardWarningSnoozedUntilFlow.value = value
        }

    fun snoozeKeyboardWarning(days: Int = 7) {
        val until = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L
        keyboardWarningSnoozedUntil = until
    }

    fun isKeyboardWarningSnoozed(now: Long = System.currentTimeMillis()): Boolean {
        return now < keyboardWarningSnoozedUntil
    }

    fun getConfig(): SyncConfig = _configFlow.value

    fun updateConfig(
        pcMac: String,
        channel: Int = getConfig().channel,
        historyCap: Int = getConfig().historyCap,
        deviceName: String = getConfig().deviceName
    ): Boolean {
        val cleanMac = pcMac.trim().uppercase(Locale.ROOT)
        prefs.edit()
            .putString(KEY_PC_MAC, cleanMac)
            .putInt(KEY_CHANNEL, channel.coerceIn(1, 30))
            .putInt(KEY_CAP, historyCap.coerceAtLeast(0))
            .putString(KEY_DEVICE_NAME, deviceName.take(48))
            .apply()

        _configFlow.value = loadConfig()
        return true
    }

    fun updateConfig(config: SyncConfig): Boolean =
        updateConfig(config.pcMac, config.channel, config.historyCap, config.deviceName)

    private fun loadConfig(): SyncConfig {
        val pcMac = prefs.getString(KEY_PC_MAC, "") ?: ""
        val channel = prefs.getInt(KEY_CHANNEL, 1).coerceIn(1, 30)
        val historyCap = prefs.getInt(KEY_CAP, 500).coerceAtLeast(0)
        val defaultName = "Android (${deviceId.takeLast(6)})"
        val deviceName = prefs.getString(KEY_DEVICE_NAME, defaultName) ?: defaultName

        return SyncConfig(
            pcMac = pcMac,
            channel = channel,
            historyCap = historyCap,
            deviceName = deviceName,
            deviceId = deviceId
        )
    }

    var imeExpiryHours: Int
        get() = prefs.getInt(KEY_IME_EXPIRY_HOURS, 1)
        set(value) = prefs.edit().putInt(KEY_IME_EXPIRY_HOURS, value.coerceAtLeast(0)).apply()

    var isUserStopped: Boolean
        get() = prefs.getBoolean(KEY_USER_STOPPED, false)
        set(value) = prefs.edit().putBoolean(KEY_USER_STOPPED, value).apply()

    companion object {
        const val KEY_USER_STOPPED = "user_stopped"
        const val KEY_APP_ICON = "app_icon"
        const val KEY_AUTO_SYNC = "auto_sync"
        const val KEY_SYNC_TEXT_ONLY = "sync_text_only"
        const val KEY_CLEAR_ON_DISCONNECT = "clear_on_disconnect"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_REDUCE_MOTION = "reduce_motion"
        const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        const val KEY_KEYBOARD_WARNING_SNOOZED_UNTIL = "keyboard_warning_snoozed_until"

        private const val KEY_PC_MAC = "pc_mac"
        private const val KEY_CHANNEL = "channel"
        private const val KEY_CAP = "history_cap"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_IME_EXPIRY_HOURS = "ime_expiry_hours"

        private val MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")

        fun isValidMac(mac: String): Boolean {
            return MAC_PATTERN.matcher(mac.trim()).matches()
        }
    }
}
