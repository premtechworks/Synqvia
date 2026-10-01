package com.github.premtechworks.synqvia

import android.app.Application
import android.content.Intent
import android.os.Build
import com.github.premtechworks.synqvia.di.AppContainer
import com.github.premtechworks.synqvia.di.DefaultAppContainer
import com.github.premtechworks.synqvia.service.ClipSyncService

class SynqviaApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Automatically start the background sync service if not explicitly stopped
        if (!container.syncPreferences.isUserStopped) {
            val serviceIntent = Intent(this, ClipSyncService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (_: Exception) {
                // Handled when app starts from background restrictions
            }
        }
    }
}
