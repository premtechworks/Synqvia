package com.github.premtechworks.synqvia.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.SynqviaApp
import com.github.premtechworks.synqvia.data.SyncPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClipSyncServiceTest {

    private lateinit var app: SynqviaApp
    private lateinit var syncPreferences: SyncPreferences

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        syncPreferences = app.container.syncPreferences
    }

    @Test
    fun onCreate_whenUserStoppedIsTrue_stopsImmediatelyWithoutRunning() {
        syncPreferences.isUserStopped = true
        val controller = Robolectric.buildService(ClipSyncService::class.java)
        val service = controller.create().get()

        assertFalse(ClipSyncService.isRunning)
        assertEquals(SyncConnectionState.Stopped, ClipSyncService.connectionState.value)
    }

    @Test
    fun onStartCommand_actionStop_setsUserStoppedAndStoppedState() {
        syncPreferences.isUserStopped = false
        val controller = Robolectric.buildService(ClipSyncService::class.java)
        val service = controller.create().get()

        val stopIntent = Intent(ClipSyncService.ACTION_STOP)
        service.onStartCommand(stopIntent, 0, 1)

        assertTrue(syncPreferences.isUserStopped)
        assertEquals(SyncConnectionState.Stopped, ClipSyncService.connectionState.value)
    }
}
