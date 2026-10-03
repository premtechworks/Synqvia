package com.github.premtechworks.synqvia.ui.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.github.premtechworks.synqvia.BuildConfig

object AppVersionHelper {
    const val LICENSE: String = "GPL-3.0"
    const val REPOSITORY: String = "premtechworks/Synqvia"

    fun getVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    fun getFormattedVersion(context: Context): String {
        return "v${getVersionName(context)}"
    }

    fun getSettingsFooterText(context: Context): String {
        return "Synqvia ${getVersionName(context)} · $LICENSE"
    }
}
