package com.github.premtechworks.synqvia.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.github.premtechworks.synqvia.SynqviaApp
import com.github.premtechworks.synqvia.TrampolineActivity
import com.github.premtechworks.synqvia.data.SyncPreferences

class SyncTileService : TileService() {

    private fun getSyncPreferences(): SyncPreferences {
        return (application as? SynqviaApp)?.container?.syncPreferences ?: SyncPreferences(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        val userStopped = getSyncPreferences().isUserStopped
        qsTile?.apply {
            state = if (userStopped) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            label = "Sync to PC"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        val syncPreferences = getSyncPreferences()
        if (syncPreferences.isUserStopped) {
            Toast.makeText(this, "Sync service is stopped. Enable in app.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Check if we have a fresh text selection from accessibility service
        val freshSelection = SelectionCache.getFreshSelection()
        if (!freshSelection.isNullOrBlank()) {
            val injectIntent = Intent(this, ClipSyncService::class.java).apply {
                action = ClipSyncService.ACTION_INJECT
                putExtra(ClipSyncService.EXTRA_TEXT, freshSelection)
            }
            startService(injectIntent)
            Toast.makeText(this, "Syncing selection to PC...", Toast.LENGTH_SHORT).show()
        } else {
            // 2. Launch TrampolineActivity to gain brief foreground focus and read system clipboard
            val trampolineIntent = Intent(this, TrampolineActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivityAndCollapse(trampolineIntent)
        }
    }
}
