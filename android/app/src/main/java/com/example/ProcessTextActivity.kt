package com.example

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.example.service.ClipSyncService

class ProcessTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        } else {
            ""
        }

        if (selectedText.isNotBlank()) {
            val serviceIntent = Intent(this, ClipSyncService::class.java).apply {
                action = ClipSyncService.ACTION_INJECT
                putExtra(ClipSyncService.EXTRA_TEXT, selectedText)
            }
            startService(serviceIntent)
            Toast.makeText(this, "Sent selection to PC ✓", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
