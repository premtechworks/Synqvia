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

    val deviceId: String by lazy {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device"
        "android-${androidId.take(16)}"
    }

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<SyncConfig> = _configFlow.asStateFlow()

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

    companion object {
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
