package com.github.premtechworks.synqvia

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.clipboard.ClipboardCaptureManager
import com.github.premtechworks.synqvia.clipboard.DefaultSensitiveClassifier
import com.github.premtechworks.synqvia.data.AppDatabase
import com.github.premtechworks.synqvia.data.ClipRepository
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.protocol.Protocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
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
class SettingsPreferencesGatingTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: ClipRepository
    private lateinit var syncPreferences: SyncPreferences
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var captureManager: ClipboardCaptureManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        syncPreferences = SyncPreferences(context)
        repository = ClipRepository(
            clipDao = database.clipDao(),
            syncPreferences = syncPreferences,
            ioDispatcher = Dispatchers.Unconfined
        )
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        captureManager = ClipboardCaptureManager(
            context = context,
            clipRepository = repository,
            syncPreferences = syncPreferences,
            clipboardManager = clipboardManager,
            sensitiveClassifier = DefaultSensitiveClassifier(),
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun syncPreferences_defaultValuesMatchMockupRequirements() {
        assertTrue(syncPreferences.autoSync)
        assertTrue(syncPreferences.syncTextOnly)
        assertFalse(syncPreferences.clearOnDisconnect)
        assertEquals("system", syncPreferences.themeMode)
    }

    @Test
    fun syncPreferences_updatesPersistCorrectly() {
        syncPreferences.autoSync = false
        assertFalse(syncPreferences.autoSync)

        syncPreferences.clearOnDisconnect = true
        assertTrue(syncPreferences.clearOnDisconnect)

        syncPreferences.themeMode = "dark"
        assertEquals("dark", syncPreferences.themeMode)
    }

    @Test
    fun syncPreferences_deletesStaleDynamicColorKeyOnInit() {
        val rawPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        rawPrefs.edit().putBoolean("dynamic_color", true).commit()
        assertTrue(rawPrefs.contains("dynamic_color"))

        // Initializing SyncPreferences should trigger cleanup
        SyncPreferences(context)
        assertFalse(rawPrefs.contains("dynamic_color"))
    }

    @Test
    fun captureLocalClip_whenAutoSyncOn_dispatchesOutbound() = runTest {
        syncPreferences.autoSync = true
        var dispatchedClip: Protocol.Message.Clip? = null
        captureManager.setOutboundClipListener { dispatchedClip = it }

        val entity = captureManager.captureLocalClip("Test text", isExplicit = false)

        assertTrue(entity != null)
        assertEquals("Test text", dispatchedClip?.text)
    }

    @Test
    fun captureLocalClip_whenAutoSyncOff_andAutomaticCapture_doesNotDispatchOutbound() = runTest {
        syncPreferences.autoSync = false
        var dispatchedClip: Protocol.Message.Clip? = null
        captureManager.setOutboundClipListener { dispatchedClip = it }

        val entity = captureManager.captureLocalClip("Automatic text", isExplicit = false)

        // Saved locally to Room database
        assertTrue(entity != null)
        // But NOT dispatched over Bluetooth
        assertEquals(null, dispatchedClip)
    }

    @Test
    fun captureLocalClip_whenAutoSyncOff_andExplicitCapture_dispatchesOutbound() = runTest {
        syncPreferences.autoSync = false
        var dispatchedClip: Protocol.Message.Clip? = null
        captureManager.setOutboundClipListener { dispatchedClip = it }

        val entity = captureManager.captureLocalClip("Explicit sync now", isExplicit = true)

        // Both saved locally AND dispatched over Bluetooth
        assertTrue(entity != null)
        assertEquals("Explicit sync now", dispatchedClip?.text)
    }
}
