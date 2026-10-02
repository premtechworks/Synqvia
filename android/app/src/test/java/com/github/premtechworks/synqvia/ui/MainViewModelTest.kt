package com.github.premtechworks.synqvia.ui

import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.SynqviaApp
import com.github.premtechworks.synqvia.data.SyncPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private lateinit var app: SynqviaApp
    private lateinit var syncPreferences: SyncPreferences
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        syncPreferences = app.container.syncPreferences
        viewModel = MainViewModel(
            application = app,
            clipRepository = app.container.clipRepository,
            syncPreferences = syncPreferences,
            defaultImeDetector = app.container.defaultImeDetector
        )
    }

    @Test
    fun stopSync_setsUserStoppedTrue() {
        syncPreferences.isUserStopped = false
        viewModel.stopSync()
        assertTrue(syncPreferences.isUserStopped)
        assertTrue(viewModel.isUserStopped)
    }

    @Test
    fun startSync_setsUserStoppedFalse() {
        syncPreferences.isUserStopped = true
        viewModel.startSync()
        assertFalse(syncPreferences.isUserStopped)
        assertFalse(viewModel.isUserStopped)
    }

    @Test
    fun reconnect_setsUserStoppedFalse() {
        syncPreferences.isUserStopped = true
        viewModel.reconnect()
        assertFalse(syncPreferences.isUserStopped)
        assertFalse(viewModel.isUserStopped)
    }

    @Test
    fun syncNow_whenStopped_setsUserMessageWithoutStartingService() {
        syncPreferences.isUserStopped = true
        viewModel.syncNow()
        assertEquals("Sync service is stopped. Start service first.", viewModel.userMessage.value)
    }

    @Test
    fun snoozeKeyboardWarning_setsTimestampAndHidesWarning() {
        syncPreferences.keyboardWarningSnoozedUntil = 0L
        assertFalse(syncPreferences.isKeyboardWarningSnoozed())
        viewModel.snoozeKeyboardWarning()
        assertTrue(syncPreferences.isKeyboardWarningSnoozed())
        assertTrue(syncPreferences.keyboardWarningSnoozedUntil > System.currentTimeMillis() + 6 * 24 * 3600 * 1000L)
    }
}
