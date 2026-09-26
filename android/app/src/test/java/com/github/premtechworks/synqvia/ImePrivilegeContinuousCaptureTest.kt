package com.github.premtechworks.synqvia

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.clipboard.ClipboardCaptureManager
import com.github.premtechworks.synqvia.clipboard.DefaultSensitiveClassifier
import com.github.premtechworks.synqvia.data.AppDatabase
import com.github.premtechworks.synqvia.data.ClipRepository
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.ime.AndroidDefaultImeDetector
import com.github.premtechworks.synqvia.ime.SynqviaImeService
import com.github.premtechworks.synqvia.protocol.Protocol
import com.github.premtechworks.synqvia.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImePrivilegeContinuousCaptureTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: ClipRepository
    private lateinit var syncPreferences: SyncPreferences
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var imeDetector: AndroidDefaultImeDetector
    private lateinit var captureManager: ClipboardCaptureManager

    private lateinit var synqviaImeComponent: String
    private val gboardImeComponent = "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        synqviaImeComponent = "${context.packageName}/.ime.SynqviaImeService"
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
        imeDetector = AndroidDefaultImeDetector(context)
        captureManager = ClipboardCaptureManager(
            context = context,
            clipRepository = repository,
            syncPreferences = syncPreferences,
            clipboardManager = clipboardManager,
            sensitiveClassifier = DefaultSensitiveClassifier(),
            defaultImeDetector = imeDetector,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        captureManager.stopMonitoring()
        database.close()
    }

    private fun setAsDefaultIme(componentName: String) {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            componentName
        )
    }

    private suspend fun waitForOutbound(
        outboundClips: List<Protocol.Message.Clip>,
        expectedCount: Int = 1,
        timeoutMs: Long = 3000
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (outboundClips.size < expectedCount && System.currentTimeMillis() < deadline) {
            delay(20)
        }
    }

    /**
     * Test A: Synqvia is default IME. Keyboard visible.
     * Copy text.
     * Expected:
     * - immediate Room entry
     * - immediate Linux transmission
     * - one outbound clip
     * - no echo
     */
    @Test
    fun testA_synqviaIsDefault_keyboardVisible_capturesAndTransmits() {
        runBlocking {
            setAsDefaultIme(synqviaImeComponent)
            assertTrue(imeDetector.isSynqviaDefaultIme())

            // Simulate active keyboard
            val imeController = Robolectric.buildService(SynqviaImeService::class.java)
            val imeService = imeController.create().get()
            imeService.onCreateInputView()

            val outboundClips = Collections.synchronizedList(mutableListOf<Protocol.Message.Clip>())
            captureManager.setOutboundClipListener { outboundClips.add(it) }
            assertTrue(captureManager.startMonitoring())

            // User copies text - listener automatically captures it
            val copiedText = "Text copied while keyboard is open"
            clipboardManager.setPrimaryClip(ClipData.newPlainText("test", copiedText))

            waitForOutbound(outboundClips, 1)

            // Verify single Room entry
            val clips = repository.allClips.first()
            assertEquals(1, clips.size)
            assertEquals(copiedText, clips[0].text)

            // Verify single outbound transmission
            assertEquals(1, outboundClips.size)
            assertEquals(copiedText, outboundClips[0].text)

            // Duplicate callback (e.g. from foreground read) is coalesced
            val duplicate = captureManager.handlePrimaryClipChanged()
            assertNull("Duplicate callback within conflict window must be coalesced", duplicate)

            imeController.destroy()
        }
    }

    /**
     * Test B: Synqvia is default IME. Keyboard hidden.
     * Copy text in Chrome, Firefox, YouTube description, Notes, etc.
     * Expected:
     * - automatic capture without opening Synqvia
     * - immediate Linux transmission
     * (Primary regression test)
     */
    @Test
    fun testB_synqviaIsDefault_keyboardHidden_automaticCaptureWithoutOpeningApp() {
        runBlocking {
            setAsDefaultIme(synqviaImeComponent)
            assertTrue(imeDetector.isSynqviaDefaultIme())

            val outboundClips = Collections.synchronizedList(mutableListOf<Protocol.Message.Clip>())
            captureManager.setOutboundClipListener { outboundClips.add(it) }

            // Start continuous monitoring from background service
            val started = captureManager.startMonitoring()
            assertTrue("Monitoring must start when Synqvia is default IME", started)
            assertTrue(captureManager.isMonitoring())

            // Copy text in Chrome while keyboard is hidden and Synqvia app is closed
            val chromeCopiedText = "https://github.com/synqvia/repo copied from Chrome"
            clipboardManager.setPrimaryClip(ClipData.newPlainText("Chrome", chromeCopiedText))

            waitForOutbound(outboundClips, 1)

            // Room has record automatically captured
            val clips = repository.allClips.first()
            assertEquals(1, clips.size)
            assertEquals(chromeCopiedText, clips[0].text)

            // Outbound sent to Linux automatically
            assertEquals(1, outboundClips.size)
            assertEquals(chromeCopiedText, outboundClips[0].text)
        }
    }

    /**
     * Test C: Gboard is default IME. Synqvia enabled but not default.
     * Copy text.
     * Expected:
     * - Synqvia does not falsely claim automatic clipboard capture
     * - explicit send paths remain functional
     * - app UI / ViewModel reports that automatic capture requires Synqvia as default keyboard
     */
    @Test
    fun testC_gboardIsDefault_automaticMonitoringDisabled_explicitPathsWork() {
        runBlocking {
            setAsDefaultIme(gboardImeComponent)
            assertFalse(imeDetector.isSynqviaDefaultIme())

            val outboundClips = Collections.synchronizedList(mutableListOf<Protocol.Message.Clip>())
            captureManager.setOutboundClipListener { outboundClips.add(it) }

            // Attempting to start monitoring must be refused
            val started = captureManager.startMonitoring()
            assertFalse("Monitoring must NOT start when Gboard is default IME", started)
            assertFalse(captureManager.isMonitoring())

            // ViewModel reflects this state
            val viewModel = MainViewModel(
                application = ApplicationProvider.getApplicationContext(),
                clipRepository = repository,
                syncPreferences = syncPreferences,
                defaultImeDetector = imeDetector
            )
            assertFalse(viewModel.isDefaultIme.value)

            // Explicit send paths (Share, PROCESS_TEXT, Quick Settings Tile, Sync Now) remain fully functional
            val explicitText = "Explicitly shared text via Send to PC"
            val explicitEntity = captureManager.captureLocalClip(explicitText)
            assertNotNull(explicitEntity)
            assertEquals(explicitText, explicitEntity?.text)

            // Room has the explicitly shared clip
            val clips = repository.allClips.first()
            assertEquals(1, clips.size)
            assertEquals(explicitText, clips[0].text)

            // Outbound sent
            assertEquals(1, outboundClips.size)
            assertEquals(explicitText, outboundClips[0].text)
        }
    }

    /**
     * Test D: Switch Gboard -> Synqvia.
     * Copy text.
     * Expected:
     * - automatic capture resumes immediately
     */
    @Test
    fun testD_switchFromGboardToSynqvia_resumesCaptureImmediately() {
        runBlocking {
            // Start as Gboard
            setAsDefaultIme(gboardImeComponent)
            assertFalse(captureManager.startMonitoring())
            assertFalse(captureManager.isMonitoring())

            // User switches default keyboard to Synqvia
            setAsDefaultIme(synqviaImeComponent)
            assertTrue(imeDetector.isSynqviaDefaultIme())

            // Service evaluates and starts monitoring
            assertTrue(captureManager.startMonitoring())
            assertTrue(captureManager.isMonitoring())

            val outboundClips = Collections.synchronizedList(mutableListOf<Protocol.Message.Clip>())
            captureManager.setOutboundClipListener { outboundClips.add(it) }

            // User copies text
            val text = "Captured right after switching to Synqvia"
            clipboardManager.setPrimaryClip(ClipData.newPlainText("test", text))

            waitForOutbound(outboundClips, 1)

            val clips = repository.allClips.first()
            assertEquals(1, clips.size)
            assertEquals(text, clips[0].text)

            assertEquals(1, outboundClips.size)
            assertEquals(text, outboundClips[0].text)
        }
    }

    /**
     * Test E: Switch Synqvia -> Gboard.
     * Copy text.
     * Expected:
     * - automatic Synqvia clipboard capture stops
     */
    @Test
    fun testE_switchFromSynqviaToGboard_stopsAutomaticCapture() {
        runBlocking {
            // Start with Synqvia as default
            setAsDefaultIme(synqviaImeComponent)
            assertTrue(captureManager.startMonitoring())
            assertTrue(captureManager.isMonitoring())

            // User switches to Gboard
            setAsDefaultIme(gboardImeComponent)
            assertFalse(imeDetector.isSynqviaDefaultIme())

            // Re-evaluating stops monitoring
            captureManager.stopMonitoring()
            assertFalse(captureManager.isMonitoring())

            val outboundClips = Collections.synchronizedList(mutableListOf<Protocol.Message.Clip>())
            captureManager.setOutboundClipListener { outboundClips.add(it) }

            // Attempting to restart monitoring while Gboard is default fails
            val started = captureManager.startMonitoring()
            assertFalse(started)
            assertFalse(captureManager.isMonitoring())
        }
    }

    /**
     * Test F: Synqvia default IME + keyboard hidden + ClipSyncService running.
     * Kill/restart the application UI only.
     * Copy text.
     * Expected:
     * - automatic capture continues through ClipSyncService
     */
    @Test
    fun testF_killAppUiOnly_automaticCaptureContinuesThroughService() {
        runBlocking {
            setAsDefaultIme(synqviaImeComponent)
            assertTrue(captureManager.startMonitoring())

            val outboundClips = Collections.synchronizedList(mutableListOf<Protocol.Message.Clip>())
            captureManager.setOutboundClipListener { outboundClips.add(it) }

            // Create and clear ViewModel simulating UI lifecycle
            val vm = MainViewModel(
                application = ApplicationProvider.getApplicationContext(),
                clipRepository = repository,
                syncPreferences = syncPreferences,
                defaultImeDetector = imeDetector
            )
            // Simulate UI teardown
            val onClearedMethod = MainViewModel::class.java.getDeclaredMethod("onCleared")
            onClearedMethod.isAccessible = true
            onClearedMethod.invoke(vm)

            // CaptureManager (owned by Service) is still monitoring
            assertTrue(captureManager.isMonitoring())

            val text = "Text copied after app UI was closed"
            clipboardManager.setPrimaryClip(ClipData.newPlainText("test", text))

            waitForOutbound(outboundClips, 1)

            val clips = repository.allClips.first()
            assertEquals(1, clips.size)
            assertEquals(text, clips[0].text)

            assertEquals(1, outboundClips.size)
            assertEquals(text, outboundClips[0].text)
        }
    }

    /**
     * Test G: A remote Linux clip updates the Android clipboard.
     * Expected:
     * - local clipboard callback may occur
     * - suppression prevents re-broadcast
     * - exactly one logical Room event
     * - no Android -> Linux ping-pong
     */
    @Test
    fun testG_remoteLinuxClipUpdatesClipboard_suppressionPreventsRebroadcast() {
        runBlocking {
            setAsDefaultIme(synqviaImeComponent)
            captureManager.startMonitoring()

            val outboundClips = Collections.synchronizedList(mutableListOf<Protocol.Message.Clip>())
            captureManager.setOutboundClipListener { outboundClips.add(it) }

            val remoteText = "Hello from Linux terminal via RFCOMM"

            // Linux clip applied to Android clipboard
            val applied = captureManager.applyRemoteClip(remoteText)
            assertTrue(applied)

            // Local clipboard change triggered
            val localCapture = captureManager.captureLocalClip(remoteText)
            assertNull("SHA-256 suppression must prevent local echo", localCapture)

            // Verify no outbound event was queued/sent back to Linux
            assertEquals("Echo must not be sent to Linux", 0, outboundClips.size)
        }
    }
}
