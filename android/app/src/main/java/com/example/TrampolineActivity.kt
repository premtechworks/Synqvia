package com.example

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.service.ClipSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val primaryClip = cm.primaryClip
            if (primaryClip != null && primaryClip.itemCount > 0) {
                val text = primaryClip.getItemAt(0)?.coerceToText(this)?.toString() ?: ""
                if (text.isNotBlank()) {
                    val app = application as? MyClipSyncApp
                    val captureManager = app?.container?.clipboardCaptureManager
                    if (captureManager != null) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            captureManager.captureLocalClip(text)
                        }
                    }
                    val serviceIntent = Intent(this, ClipSyncService::class.java).apply {
                        action = ClipSyncService.ACTION_INJECT
                        putExtra(ClipSyncService.EXTRA_TEXT, text)
                    }
                    startService(serviceIntent)
                    Toast.makeText(this, "Synced clipboard to PC ✓", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to read clipboard", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
