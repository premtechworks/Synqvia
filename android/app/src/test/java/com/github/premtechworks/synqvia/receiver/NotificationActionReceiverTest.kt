package com.github.premtechworks.synqvia.receiver

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.service.ClipSyncService
import com.github.premtechworks.synqvia.service.SyncConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationActionReceiverTest {

    private lateinit var context: Context
    private lateinit var syncPreferences: SyncPreferences
    private lateinit var receiver: NotificationActionReceiver
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        syncPreferences = SyncPreferences(context)
        receiver = NotificationActionReceiver()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun onReceiveStop_setsUserStoppedAndCancelsNotification() {
        syncPreferences.isUserStopped = false

        val intent = Intent(ClipSyncService.ACTION_STOP)
        receiver.onReceive(context, intent)

        assertTrue(syncPreferences.isUserStopped)
        assertEquals(SyncConnectionState.Stopped, ClipSyncService.connectionState.value)
    }

    @Test
    fun onReceiveReconnect_clearsUserStopped() {
        syncPreferences.isUserStopped = true

        val intent = Intent(ClipSyncService.ACTION_RECONNECT)
        receiver.onReceive(context, intent)

        assertEquals(false, syncPreferences.isUserStopped)
    }
}
