package com.github.premtechworks.synqvia.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.github.premtechworks.synqvia.SynqviaApp
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.service.ClipSyncService

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val syncPreferences = (context.applicationContext as? SynqviaApp)?.container?.syncPreferences
            ?: SyncPreferences(context)

        when (action) {
            ClipSyncService.ACTION_STOP -> {
                // 1. Dismiss notification immediately
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(ClipSyncService.NOTIFICATION_ID)

                // 2. Mark stopped in preferences and state flow
                syncPreferences.isUserStopped = true
                ClipSyncService.setStoppedState()

                // 3. Stop service if running
                if (ClipSyncService.isRunning) {
                    val serviceIntent = Intent(context, ClipSyncService::class.java).apply {
                        this.action = ClipSyncService.ACTION_STOP
                    }
                    try {
                        context.startService(serviceIntent)
                    } catch (_: Exception) {
                        try {
                            context.stopService(Intent(context, ClipSyncService::class.java))
                        } catch (_: Exception) {}
                    }
                }
            }
            ClipSyncService.ACTION_RECONNECT -> {
                syncPreferences.isUserStopped = false
                val serviceIntent = Intent(context, ClipSyncService::class.java).apply {
                    this.action = ClipSyncService.ACTION_RECONNECT
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (_: Exception) {
                    try { context.startService(serviceIntent) } catch (_: Exception) {}
                }
            }
            ClipSyncService.ACTION_SYNC_NOW -> {
                if (syncPreferences.isUserStopped) return
                val serviceIntent = Intent(context, ClipSyncService::class.java).apply {
                    this.action = ClipSyncService.ACTION_SYNC_NOW
                }
                try {
                    context.startService(serviceIntent)
                } catch (_: Exception) {}
            }
        }
    }
}
