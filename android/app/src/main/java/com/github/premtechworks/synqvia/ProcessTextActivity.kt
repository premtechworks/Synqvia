package com.github.premtechworks.synqvia

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.github.premtechworks.synqvia.service.ClipSyncService

class ProcessTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        } else {
            ""
        }

        if (selectedText.isNotBlank()) {
            val app = application as? com.github.premtechworks.synqvia.SynqviaApp
            val syncPreferences = app?.container?.syncPreferences ?: com.github.premtechworks.synqvia.data.SyncPreferences(this)

            if (syncPreferences.isUserStopped) {
                Toast.makeText(this, "Sync service is stopped. Start in app.", Toast.LENGTH_SHORT).show()
            } else {
                val serviceIntent = Intent(this, ClipSyncService::class.java).apply {
                    action = ClipSyncService.ACTION_INJECT
                    putExtra(ClipSyncService.EXTRA_TEXT, selectedText)
                }
                try {
                    startService(serviceIntent)
                    Toast.makeText(this, "Sent selection to PC ✓", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }
        }

        finish()
    }
}
