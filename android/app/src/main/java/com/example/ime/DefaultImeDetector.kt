package com.example.ime

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * Abstraction for detecting whether MyClipSync is currently configured as the
 * platform default Input Method Editor (IME).
 */
interface DefaultImeDetector {
    /**
     * Returns true if MyClipSync is the active default IME selected by the user,
     * false otherwise (e.g. if Gboard or another keyboard is default, or if MyClipSync
     * is merely enabled in system settings but not selected as default).
     */
    fun isMyClipSyncDefaultIme(): Boolean
}

/**
 * Android implementation querying [Settings.Secure.DEFAULT_INPUT_METHOD].
 */
class AndroidDefaultImeDetector(
    private val context: Context,
    private val targetPackageName: String = context.packageName
) : DefaultImeDetector {

    override fun isMyClipSyncDefaultIme(): Boolean {
        return try {
            val defaultIme = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            ) ?: return false

            val defaultImePackage = ComponentName.unflattenFromString(defaultIme)?.packageName
                ?: defaultIme.substringBefore('/')

            defaultImePackage.isNotBlank() && defaultImePackage == targetPackageName
        } catch (_: Exception) {
            false
        }
    }
}
