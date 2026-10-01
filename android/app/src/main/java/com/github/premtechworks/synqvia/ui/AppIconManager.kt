package com.github.premtechworks.synqvia.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconManager {
    const val ICON_CLASSIC = "classic"
    const val ICON_THEMED = "themed"

    private const val TAG = "AppIconManager"

    fun setAppIcon(context: Context, icon: String) {
        try {
            val pm = context.packageManager
            val defaultAlias = ComponentName(context.packageName, "${context.packageName}.MainActivityDefault")
            val themedAlias = ComponentName(context.packageName, "${context.packageName}.MainActivityThemed")

            val isThemed = (icon == ICON_THEMED)

            if (isThemed) {
                pm.setComponentEnabledSetting(
                    themedAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    defaultAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    defaultAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    themedAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            Log.d(TAG, "Switched app launcher icon to: $icon")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch launcher icon to $icon", e)
        }
    }

    fun getActiveIcon(context: Context): String {
        return try {
            val pm = context.packageManager
            val themedAlias = ComponentName(context.packageName, "${context.packageName}.MainActivityThemed")
            val state = pm.getComponentEnabledSetting(themedAlias)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) ICON_THEMED else ICON_CLASSIC
        } catch (e: Exception) {
            ICON_CLASSIC
        }
    }
}
