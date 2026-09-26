package com.example

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.clipboard.ClipboardCaptureManager
import com.example.clipboard.DefaultSensitiveClassifier
import com.example.data.AppDatabase
import com.example.data.ClipRepository
import com.example.data.SyncPreferences
import com.example.protocol.Protocol
import kotlinx.coroutines.Dispatchers
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClipboardCaptureManagerTest {

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
    fun testCaptureLocalClip_persistsToRoomAndDispatchesOutbound() = runBlocking {
        val outboundClips = mutableListOf<Protocol.Message.Clip>()
        captureManager.setOutboundClipListener { clip ->
            outboundClips.add(clip)
        }

        val text = "User copied this secret string"
        val entity = captureManager.captureLocalClip(text)

        assertNotNull(entity)
        assertEquals(text, entity?.text)
        assertEquals("local", entity?.direction)
        assertFalse(entity?.conflictLoser ?: true)

        // Verify exactly one Room record
        val clips = repository.allClips.first()
        assertEquals(1, clips.size)
        assertEquals(text, clips[0].text)

        // Verify exactly one outbound sync event
        assertEquals(1, outboundClips.size)
        assertEquals(text, outboundClips[0].text)
        assertEquals(entity?.id, outboundClips[0].id)
    }

    @Test
    fun testEchoSuppression_whenRemoteClipApplied_noLocalEchoProduced() = runBlocking {
        val outboundClips = mutableListOf<Protocol.Message.Clip>()
        captureManager.setOutboundClipListener { clip ->
            outboundClips.add(clip)
        }

        val remoteText = "Hello from Linux PC via RFCOMM"

        // 1. Simulate Linux -> Android remote clip applied
        val applied = captureManager.applyRemoteClip(remoteText)
        assertTrue(applied)

        // Verify primary clipboard received the text
        val clipData = clipboardManager.primaryClip
        assertNotNull(clipData)
        assertEquals(remoteText, clipData?.getItemAt(0)?.text?.toString())

        // 2. Clipboard changed listener fires (whether from IME or service)
        val captured = captureManager.captureLocalClip(remoteText)

        // Must be suppressed!
        assertNull("Echo must be swallowed by SHA-256 suppression", captured)

        // Verify no outbound event was dispatched to Linux
        assertEquals(0, outboundClips.size)
    }

    @Test
    fun testEchoSuppressionWindow_expiresAfterDuration() = runBlocking {
        val text = "Temporary suppression test"
        // Arm for 100ms
        captureManager.armSuppression(text, durationMs = 100L)

        // Immediate check: suppressed
        assertTrue(captureManager.isSuppressed(text, now = System.currentTimeMillis()))

        // Check after 200ms: suppression window expired
        assertFalse(captureManager.isSuppressed(text, now = System.currentTimeMillis() + 200L))
    }

    @Test
    fun testDuplicateObservers_coalescedWithinConflictWindow() = runBlocking {
        val outboundClips = mutableListOf<Protocol.Message.Clip>()
        captureManager.setOutboundClipListener { clip ->
            outboundClips.add(clip)
        }

        val text = "Duplicate callback test"

        // First callback (e.g. from IME)
        val firstResult = captureManager.captureLocalClip(text)
        assertNotNull(firstResult)

        // Second callback (e.g. from accessibility or background listener) within 1500ms
        val secondResult = captureManager.captureLocalClip(text)
        assertNull("Duplicate observer callback within conflict window must be coalesced", secondResult)

        // Exactly one Room record and one outbound transmission
        val clips = repository.allClips.first()
        assertEquals(1, clips.size)
        assertEquals(1, outboundClips.size)
    }

    @Test
    fun testDistinctClips_notCoalesced() = runBlocking {
        val outboundClips = mutableListOf<Protocol.Message.Clip>()
        captureManager.setOutboundClipListener { clip ->
            outboundClips.add(clip)
        }

        val clip1 = captureManager.captureLocalClip("First text")
        val clip2 = captureManager.captureLocalClip("Second text")

        assertNotNull(clip1)
        assertNotNull(clip2)
        assertEquals(2, repository.allClips.first().size)
        assertEquals(2, outboundClips.size)
    }

    @Test
    fun testBlankAndEmptyText_ignored() = runBlocking {
        val resultEmpty = captureManager.captureLocalClip("")
        val resultWhitespace = captureManager.captureLocalClip("   \n\t  ")

        assertNull(resultEmpty)
        assertNull(resultWhitespace)
        assertEquals(0, repository.allClips.first().size)
    }

    @Test
    fun testSensitiveClassification_persistedInEntity() = runBlocking {
        val sensitiveText = "MySuperSecretPassword123!"
        val entity = captureManager.captureLocalClip(sensitiveText, isSensitive = true)

        assertNotNull(entity)
        assertTrue(entity?.sensitive == true)

        val clips = repository.allClips.first()
        assertEquals(1, clips.size)
        assertTrue(clips[0].sensitive)
    }
}
