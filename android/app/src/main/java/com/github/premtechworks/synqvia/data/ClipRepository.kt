package com.github.premtechworks.synqvia.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

data class SyncStats(
    val totalCount: Int,
    val localSentCount: Int,
    val remoteReceivedCount: Int,
    val conflictCount: Int
)

class ClipRepository(
    private val clipDao: ClipDao,
    private val syncPreferences: SyncPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allClips: Flow<List<ClipEntity>> = clipDao.getAllClips()

    val syncStats: Flow<SyncStats> = combine(
        clipDao.getClipCount(),
        clipDao.getLocalCount(),
        clipDao.getRemoteCount(),
        clipDao.getConflictCount()
    ) { total, local, remote, conflicts ->
        SyncStats(
            totalCount = total,
            localSentCount = local,
            remoteReceivedCount = remote,
            conflictCount = conflicts
        )
    }

    fun searchClips(query: String): Flow<List<ClipEntity>> {
        return if (query.isBlank()) {
            clipDao.getAllClips()
        } else {
            // Escape SQL LIKE special characters
            val escaped = query
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            clipDao.searchClips(escaped)
        }
    }

    suspend fun insertClip(clip: ClipEntity): Boolean = withContext(ioDispatcher) {
        val insertedRow = clipDao.insert(clip)
        if (insertedRow > 0) {
            val cap = syncPreferences.getConfig().historyCap
            if (cap > 0) {
                clipDao.prune(cap)
            }
            true
        } else {
            false
        }
    }

    suspend fun exists(id: String): Boolean = withContext(ioDispatcher) {
        clipDao.exists(id)
    }

    suspend fun markLoser(id: String) = withContext(ioDispatcher) {
        clipDao.markLoser(id)
    }

    suspend fun deleteClip(id: String) = withContext(ioDispatcher) {
        clipDao.deleteById(id)
    }

    suspend fun clearAll() = withContext(ioDispatcher) {
        clipDao.clearAll()
    }

    fun getImeClips(expiryDurationMs: Long = 3600_000L, limit: Int = 50): Flow<List<ClipEntity>> {
        val expiryThreshold = System.currentTimeMillis() - expiryDurationMs
        return clipDao.getImeClips(expiryThreshold, limit)
    }

    suspend fun getClipById(id: String): ClipEntity? = withContext(ioDispatcher) {
        clipDao.getClipById(id)
    }

    suspend fun setPinned(id: String, pinned: Boolean) = withContext(ioDispatcher) {
        clipDao.setPinned(id, pinned)
    }
}
