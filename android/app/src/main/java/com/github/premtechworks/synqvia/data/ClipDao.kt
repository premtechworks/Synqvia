package com.github.premtechworks.synqvia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {

    @Query("SELECT * FROM clips ORDER BY ts DESC")
    fun getAllClips(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips ORDER BY ts DESC LIMIT :limit")
    fun getRecentClips(limit: Int): Flow<List<ClipEntity>>

    @Query("""
        SELECT * FROM clips 
        WHERE text LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY ts DESC
    """)
    fun searchClips(query: String): Flow<List<ClipEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(clip: ClipEntity): Long

    @Query("SELECT COUNT(*) FROM clips WHERE id = :id")
    suspend fun countById(id: String): Int

    suspend fun exists(id: String): Boolean = countById(id) > 0

    @Query("UPDATE clips SET conflict_loser = 1 WHERE id = :id")
    suspend fun markLoser(id: String)

    @Query("""
        DELETE FROM clips 
        WHERE id NOT IN (
            SELECT id FROM clips ORDER BY ts DESC, rowid DESC LIMIT :keepCount
        )
    """)
    suspend fun prune(keepCount: Int)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM clips")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM clips")
    fun getClipCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM clips WHERE direction = 'local'")
    fun getLocalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM clips WHERE direction = 'remote'")
    fun getRemoteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM clips WHERE conflict_loser = 1")
    fun getConflictCount(): Flow<Int>

    @Query("""
        SELECT * FROM clips 
        WHERE pinned = 1 OR ts > :expiryThreshold
        ORDER BY pinned DESC, ts DESC 
        LIMIT :limit
    """)
    fun getImeClips(expiryThreshold: Long, limit: Int = 50): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE id = :id LIMIT 1")
    suspend fun getClipById(id: String): ClipEntity?

    @Query("UPDATE clips SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)
}
