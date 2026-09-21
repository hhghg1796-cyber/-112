package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentByIdFlow(id: String): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE documents SET lastReadPage = :page, lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastReadPage(id: String, page: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET title = :title, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET type = :type, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateType(id: String, type: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET pageCount = :pageCount, thumbnailPath = :thumbnailPath, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePageCountAndThumbnail(id: String, pageCount: Int, thumbnailPath: String?, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchDocuments(query: String): List<DocumentEntity>
}
