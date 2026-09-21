package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ScanSessionEntity
import com.example.data.local.entity.ScanSessionPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {

    @Query("SELECT * FROM scan_sessions WHERE isCompleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<ScanSessionEntity?>

    @Query("SELECT * FROM scan_sessions WHERE isCompleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSession(): ScanSessionEntity?

    @Query("SELECT * FROM scan_session_pages WHERE sessionId = :sessionId ORDER BY pageNumber ASC")
    fun getSessionPagesFlow(sessionId: String): Flow<List<ScanSessionPageEntity>>

    @Query("SELECT * FROM scan_session_pages WHERE sessionId = :sessionId ORDER BY pageNumber ASC")
    suspend fun getSessionPages(sessionId: String): List<ScanSessionPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity)

    @Update
    suspend fun updateSession(session: ScanSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionPage(page: ScanSessionPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionPages(pages: List<ScanSessionPageEntity>)

    @Update
    suspend fun updateSessionPage(page: ScanSessionPageEntity)

    @Query("DELETE FROM scan_session_pages WHERE id = :pageId")
    suspend fun deleteSessionPage(pageId: String)

    @Query("DELETE FROM scan_session_pages WHERE sessionId = :sessionId")
    suspend fun clearSessionPages(sessionId: String)

    @Query("DELETE FROM scan_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("UPDATE scan_sessions SET isCompleted = 1 WHERE id = :sessionId")
    suspend fun completeSession(sessionId: String)
}
