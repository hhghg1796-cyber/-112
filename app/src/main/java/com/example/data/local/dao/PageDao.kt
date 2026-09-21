package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesForDocument(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getPagesForDocumentSync(documentId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :id LIMIT 1")
    suspend fun getPageById(id: String): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Query("UPDATE pages SET rotation = :rotation WHERE id = :id")
    suspend fun updateRotation(id: String, rotation: Int)

    @Query("UPDATE pages SET ocrText = :ocrText, processingState = 'READY' WHERE id = :id")
    suspend fun updateOcrText(id: String, ocrText: String)

    @Query("UPDATE pages SET pageNumber = :newPageNumber WHERE id = :id")
    suspend fun updatePageNumber(id: String, newPageNumber: Int)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deletePageById(id: String)

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: String)

    @Query("SELECT * FROM pages WHERE ocrText LIKE '%' || :query || '%'")
    suspend fun searchPageOcrText(query: String): List<PageEntity>
}
