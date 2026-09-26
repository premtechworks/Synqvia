package com.github.premtechworks.synqvia

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.data.AppDatabase
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.data.ClipRepository
import com.github.premtechworks.synqvia.data.SyncPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class ClipRepositoryImeTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: ClipRepository
    private lateinit var syncPreferences: SyncPreferences

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testImeExpiration_unpinnedExpires_pinnedPersists() = runBlocking {
        val now = System.currentTimeMillis()
        val oneHourMs = 3600_000L

        // 1. Old unpinned clip (copied 2 hours ago)
        val oldUnpinned = ClipEntity(
            id = "clip-old-unpinned",
            text = "Old unpinned clip (should be hidden in IME)",
            ts = now - (2 * oneHourMs),
            src = "android",
            direction = "local",
            pinned = false
        )

        // 2. Old pinned clip (copied 5 hours ago, but pinned!)
        val oldPinned = ClipEntity(
            id = "clip-old-pinned",
            text = "Old pinned clip (must remain visible in IME)",
            ts = now - (5 * oneHourMs),
            src = "android",
            direction = "local",
            pinned = true
        )

        // 3. Fresh unpinned clip (copied 10 minutes ago)
        val freshUnpinned = ClipEntity(
            id = "clip-fresh-unpinned",
            text = "Fresh unpinned clip",
            ts = now - (10 * 60_000L),
            src = "android",
            direction = "local",
            pinned = false
        )

        repository.insertClip(oldUnpinned)
        repository.insertClip(oldPinned)
        repository.insertClip(freshUnpinned)

        // Query IME history with 1-hour expiration
        val imeClips = repository.getImeClips(expiryDurationMs = oneHourMs).first()

        // Verify: only the pinned clip and the fresh clip appear in IME
        assertEquals(2, imeClips.size)
        // Pinned clip is ordered first
        assertEquals(oldPinned.id, imeClips[0].id)
        assertTrue(imeClips[0].pinned)
        assertEquals(freshUnpinned.id, imeClips[1].id)

        // Crucial requirement: Synqvia sync history MUST remain unaffected!
        val allSyncClips = repository.allClips.first()
        assertEquals(3, allSyncClips.size)
        assertTrue(allSyncClips.any { it.id == oldUnpinned.id })
    }

    @Test
    fun testTogglePin() = runBlocking {
        val clip = ClipEntity(
            id = "clip-to-pin",
            text = "Test pin toggling",
            ts = System.currentTimeMillis(),
            src = "android",
            direction = "local",
            pinned = false
        )
        repository.insertClip(clip)

        val initial = repository.getClipById("clip-to-pin")
        assertFalse(initial?.pinned ?: true)

        // Pin the clip
        repository.setPinned("clip-to-pin", true)
        val pinned = repository.getClipById("clip-to-pin")
        assertTrue(pinned?.pinned ?: false)

        // Unpin the clip
        repository.setPinned("clip-to-pin", false)
        val unpinned = repository.getClipById("clip-to-pin")
        assertFalse(unpinned?.pinned ?: true)
    }

    @Test
    fun testDeleteClip() = runBlocking {
        val clip = ClipEntity(
            id = "clip-to-delete",
            text = "Will be deleted",
            ts = System.currentTimeMillis(),
            src = "android",
            direction = "local"
        )
        repository.insertClip(clip)
        assertEquals(1, repository.allClips.first().size)

        repository.deleteClip("clip-to-delete")
        assertEquals(0, repository.allClips.first().size)
    }
}
