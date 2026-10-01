package com.github.premtechworks.synqvia.receiver

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.data.SyncPreferences
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var syncPreferences: SyncPreferences
    private lateinit var receiver: BootReceiver

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).clearStartedServices()
        syncPreferences = SyncPreferences(context)
        receiver = BootReceiver()
    }

    @Test
    fun whenUserStoppedIsTrue_bootReceiverDoesNotStartService() {
        syncPreferences.isUserStopped = true
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)

        val nextStartedService = shadowOf(context as Application).nextStartedService
        assertNull("Service should not be started when isUserStopped is true", nextStartedService)
    }

    @Test
    fun whenUserStoppedIsFalse_bootReceiverStartsService() {
        syncPreferences.isUserStopped = false
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)

        val nextStartedService = shadowOf(context as Application).nextStartedService
        assertNotNull("Service should be started when isUserStopped is false", nextStartedService)
    }
}
