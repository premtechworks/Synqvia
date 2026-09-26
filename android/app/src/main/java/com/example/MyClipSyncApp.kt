package com.example

import android.app.Application
import android.content.Intent
import android.os.Build
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer
import com.example.service.ClipSyncService

class MyClipSyncApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Automatically start the background sync service
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
