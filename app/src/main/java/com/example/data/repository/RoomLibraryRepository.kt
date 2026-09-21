package com.example.data.repository

import android.content.Context
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.PageEntity
import com.example.data.local.entity.ScanSessionEntity
import com.example.data.local.entity.ScanSessionPageEntity
import com.example.data.storage.LocalStorageManager
import com.example.data.engine.TesseractOcrEngine
import com.example.domain.engine.OcrEngine
import com.example.domain.model.Bookmark
import com.example.domain.model.ContentType
import com.example.domain.model.DocumentPage
import com.example.domain.model.FileImportItem
import com.example.domain.model.LibraryDocument
import com.example.domain.model.OcrStatus
import com.example.domain.model.ScannedPage
import com.example.domain.repository.LibraryRepository
import com.example.domain.repository.SearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoomLibraryRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val storageManager: LocalStorageManager = LocalStorageManager(context),
    private val ocrEngine: OcrEngine = TesseractOcrEngine(context)
) : LibraryRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val documentDao = database.documentDao()
    private val pageDao = database.pageDao()
    private val bookmarkDao = database.bookmarkDao()
    private val scanSessionDao = database.scanSessionDao()

    private val _activeScanPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    override val activeScanPages: Flow<List<ScannedPage>> = _activeScanPages.asStateFlow()

    private val _activeImportFiles = MutableStateFlow<List<FileImportItem>>(emptyList())
    override val activeImportFiles: Flow<List<FileImportItem>> = _activeImportFiles.asStateFlow()

    private val _currentSessionId = MutableStateFlow("session_${System.currentTimeMillis()}")

    override val hasUnfinishedSession: Flow<Boolean> = scanSessionDao.getActiveSessionFlow().map { session ->
        if (session != null) {
            val pages = scanSessionDao.getSessionPages(session.id)
            pages.isNotEmpty()
        } else false
    }

    override val documents: Flow<List<LibraryDocument>> = documentDao.getAllDocuments().map { entities ->
        entities.map { docEntity ->
            val pages = pageDao.getPagesForDocumentSync(docEntity.id).map { p ->
                DocumentPage(
                    pageNumber = p.pageNumber,
                    imageUri = p.imagePath,
                    imagePath = p.imagePath,
                    thumbnailPath = p.thumbnailPath,
                    ocrText = p.ocrText,
                    ocrStatus = when (p.processingState) {
                        "READY" -> if (p.ocrText.isNotBlank()) OcrStatus.COMPLETED else OcrStatus.NOT_PROCESSED
                        "PROCESSING" -> OcrStatus.PROCESSING
                        "FAILED" -> OcrStatus.FAILED
                        else -> OcrStatus.NOT_PROCESSED
                    },
                    notes = p.notes
                )
            }
            mapEntityToDocument(docEntity, pages)
        }
    }

    override val bookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks().map { entities ->
        entities.map { bm ->
            val doc = documentDao.getDocumentById(bm.documentId)
            Bookmark(
                id = bm.id,
                documentId = bm.documentId,
                documentTitle = doc?.title ?: "وثيقة",
                pageNumber = bm.pageNumber,
                title = bm.title,
                note = bm.note,
                createdAt = formatTimestampArabic(bm.createdAt)
            )
        }
    }

    init {
        // Check if there is an active unfinished session to restore
        scope.launch {
            val active = scanSessionDao.getActiveSession()
            if (active != null) {
                _currentSessionId.value = active.id
                val pages = scanSessionDao.getSessionPages(active.id)
                if (pages.isNotEmpty()) {
                    _activeScanPages.value = pages.map { p ->
                        ScannedPage(
                            id = p.id,
                            pageNumber = p.pageNumber,
                            imageUri = p.imagePath,
                            imagePath = p.imagePath,
                            thumbnailPath = p.thumbnailPath,
                            rotationDegrees = p.rotationDegrees
                        )
                    }
                }
            }
        }
    }

    override fun getDocumentById(id: String): LibraryDocument? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            val entity = documentDao.getDocumentById(id) ?: return@runBlocking null
            val pages = pageDao.getPagesForDocumentSync(id).map { p ->
                DocumentPage(
                    pageNumber = p.pageNumber,
                    imageUri = p.imagePath,
                    imagePath = p.imagePath,
                    thumbnailPath = p.thumbnailPath,
                    ocrText = p.ocrText,
                    ocrStatus = when (p.processingState) {
                        "READY" -> if (p.ocrText.isNotBlank()) OcrStatus.COMPLETED else OcrStatus.NOT_PROCESSED
                        "PROCESSING" -> OcrStatus.PROCESSING
                        "FAILED" -> OcrStatus.FAILED
                        else -> OcrStatus.NOT_PROCESSED
                    },
                    notes = p.notes
                )
            }
            mapEntityToDocument(entity, pages)
        }
    }

    override fun toggleFavorite(id: String) {
        scope.launch {
            val doc = documentDao.getDocumentById(id) ?: return@launch
            documentDao.setFavorite(id, !doc.isFavorite)
        }
    }

    override fun deleteDocument(id: String) {
        scope.launch {
            documentDao.deleteDocumentById(id)
            pageDao.deletePagesForDocument(id)
            bookmarkDao.deleteBookmarksForDocument(id)
            storageManager.deleteDocumentFiles(id)
        }
    }

    override fun updateDocumentTitle(id: String, newTitle: String) {
        scope.launch {
            documentDao.updateTitle(id, newTitle)
        }
    }

    override fun updateDocumentType(id: String, newType: ContentType) {
        scope.launch {
            documentDao.updateType(id, newType.name)
        }
    }

    override fun updateLastReadPage(id: String, page: Int) {
        scope.launch {
            documentDao.updateLastReadPage(id, page)
        }
    }

    override fun addBookmark(documentId: String, pageNumber: Int, title: String, note: String) {
        scope.launch {
            val bookmarkEntity = BookmarkEntity(
                id = "bm_${System.currentTimeMillis()}",
                documentId = documentId,
                pageNumber = pageNumber,
                title = title,
                note = note,
                createdAt = System.currentTimeMillis()
            )
            bookmarkDao.insertBookmark(bookmarkEntity)
        }
    }

    override fun removeBookmark(bookmarkId: String) {
        scope.launch {
            bookmarkDao.deleteBookmarkById(bookmarkId)
        }
    }

    override fun addScannedPage(page: ScannedPage) {
        val updated = _activeScanPages.value + page
        _activeScanPages.value = updated
        persistCurrentSession(updated)
    }

    override fun removeScannedPage(id: String) {
        val updated = _activeScanPages.value.filterNot { it.id == id }
            .mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }
        _activeScanPages.value = updated
        scope.launch {
            scanSessionDao.deleteSessionPage(id)
            persistCurrentSession(updated)
        }
    }

    override fun rotateScannedPage(id: String) {
        val page = _activeScanPages.value.find { it.id == id } ?: return
        val newRotation = (page.rotationDegrees + 90) % 360
        val updated = _activeScanPages.value.map {
            if (it.id == id) it.copy(rotationDegrees = newRotation) else it
        }
        _activeScanPages.value = updated

        scope.launch {
            page.imagePath?.let { path ->
                storageManager.rotateImageFile(path, 90)
            }
            scanSessionDao.updateSessionPage(
                ScanSessionPageEntity(
                    id = page.id,
                    sessionId = _currentSessionId.value,
                    pageNumber = page.pageNumber,
                    imagePath = page.imagePath ?: "",
                    thumbnailPath = page.thumbnailPath,
                    rotationDegrees = newRotation
                )
            )
        }
    }

    override fun reorderScannedPages(fromIndex: Int, toIndex: Int) {
        val list = _activeScanPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            val updated = list.mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }
            _activeScanPages.value = updated
            persistCurrentSession(updated)
        }
    }

    override fun clearScanSession() {
        val sid = _currentSessionId.value
        _activeScanPages.value = emptyList()
        _currentSessionId.value = "session_${System.currentTimeMillis()}"
        scope.launch {
            scanSessionDao.clearSessionPages(sid)
            scanSessionDao.deleteSession(sid)
            storageManager.deleteSessionFiles(sid)
        }
    }

    override suspend fun saveScannedPagesAsDocument(
        title: String,
        type: ContentType,
        author: String?,
        description: String?
    ): String = withContext(Dispatchers.IO) {
        val docId = "doc_${System.currentTimeMillis()}"
        val sessionId = _currentSessionId.value
        val pagesToPromote = _activeScanPages.value.mapNotNull { p ->
            val path = p.imagePath ?: p.imageUri
            if (path != null) Pair(p.pageNumber, path) else null
        }

        // Promote images from session dir to document dir
        val promoted = storageManager.promoteSessionPagesToDocument(sessionId, docId, pagesToPromote)

        val pageEntities = promoted.map { (pageNumber, imagePath, thumbPath) ->
            PageEntity(
                id = "p_${docId}_$pageNumber",
                documentId = docId,
                pageNumber = pageNumber,
                imagePath = imagePath,
                thumbnailPath = thumbPath,
                createdAt = System.currentTimeMillis(),
                rotation = 0,
                processingState = "READY",
                ocrText = "",
                notes = ""
            )
        }

        pageDao.insertPages(pageEntities)

        val firstThumb = promoted.firstOrNull()?.third ?: promoted.firstOrNull()?.second
        var totalBytes = 0L
        for ((_, imgPath, _) in promoted) {
            val f = File(imgPath)
            if (f.exists()) totalBytes += f.length()
        }
        val sizeFormatted = formatFileSize(totalBytes)

        val docEntity = DocumentEntity(
            id = docId,
            title = title.ifBlank { "وثيقة ممسوحة جديدة" },
            type = type.name,
            author = author?.ifBlank { null },
            description = description?.ifBlank { null },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis(),
            pageCount = pageEntities.size,
            thumbnailPath = firstThumb,
            isFavorite = false,
            lastReadPage = 1,
            ocrStatus = "NOT_PROCESSED",
            fileSizeFormatted = sizeFormatted
        )

        documentDao.insertDocument(docEntity)

        // Complete session
        scanSessionDao.completeSession(sessionId)
        _activeScanPages.value = emptyList()
        _currentSessionId.value = "session_${System.currentTimeMillis()}"

        docId
    }

    override suspend fun cropScannedPage(id: String, left: Float, top: Float, right: Float, bottom: Float): Unit = withContext(Dispatchers.IO) {
        val page = _activeScanPages.value.find { it.id == id } ?: return@withContext
        page.imagePath?.let { path ->
            storageManager.cropImageFile(path, left, top, right, bottom)
        }
        Unit
    }

    override suspend fun enhanceScannedPage(id: String): Unit = withContext(Dispatchers.IO) {
        val page = _activeScanPages.value.find { it.id == id } ?: return@withContext
        page.imagePath?.let { path ->
            storageManager.enhanceImageFile(path)
        }
        Unit
    }

    override suspend fun restoreUnfinishedSession() = withContext(Dispatchers.IO) {
        val session = scanSessionDao.getActiveSession() ?: return@withContext
        _currentSessionId.value = session.id
        val pages = scanSessionDao.getSessionPages(session.id)
        _activeScanPages.value = pages.map { p ->
            ScannedPage(
                id = p.id,
                pageNumber = p.pageNumber,
                imageUri = p.imagePath,
                imagePath = p.imagePath,
                thumbnailPath = p.thumbnailPath,
                rotationDegrees = p.rotationDegrees
            )
        }
    }

    override suspend fun deleteDocumentPage(documentId: String, pageNumber: Int) = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesForDocumentSync(documentId)
        val target = pages.find { it.pageNumber == pageNumber } ?: return@withContext
        pageDao.deletePageById(target.id)

        // Delete page image file
        File(target.imagePath).delete()
        target.thumbnailPath?.let { File(it).delete() }

        // Re-index remaining pages
        val remaining = pages.filterNot { it.id == target.id }
        remaining.forEachIndexed { index, p ->
            pageDao.updatePageNumber(p.id, index + 1)
        }

        val updatedPages = pageDao.getPagesForDocumentSync(documentId)
        val firstThumb = updatedPages.firstOrNull()?.thumbnailPath ?: updatedPages.firstOrNull()?.imagePath
        documentDao.updatePageCountAndThumbnail(documentId, updatedPages.size, firstThumb)
    }

    override suspend fun addPagesToExistingDocument(documentId: String, pages: List<ScannedPage>) = withContext(Dispatchers.IO) {
        val currentPages = pageDao.getPagesForDocumentSync(documentId)
        val startNumber = currentPages.size + 1
        val pagesToPromote = pages.mapIndexedNotNull { idx, p ->
            val path = p.imagePath ?: p.imageUri
            if (path != null) Pair(startNumber + idx, path) else null
        }

        val promoted = storageManager.promoteSessionPagesToDocument(_currentSessionId.value, documentId, pagesToPromote)
        val entities = promoted.map { (pageNumber, imagePath, thumbPath) ->
            PageEntity(
                id = "p_${documentId}_$pageNumber",
                documentId = documentId,
                pageNumber = pageNumber,
                imagePath = imagePath,
                thumbnailPath = thumbPath,
                createdAt = System.currentTimeMillis(),
                rotation = 0,
                processingState = "READY",
                ocrText = "",
                notes = ""
            )
        }

        pageDao.insertPages(entities)
        val allPages = pageDao.getPagesForDocumentSync(documentId)
        val firstThumb = allPages.firstOrNull()?.thumbnailPath ?: allPages.firstOrNull()?.imagePath
        documentDao.updatePageCountAndThumbnail(documentId, allPages.size, firstThumb)
    }

    override fun addImportFiles(files: List<FileImportItem>) {
        _activeImportFiles.value = _activeImportFiles.value + files
    }

    override fun removeImportFile(id: String) {
        _activeImportFiles.value = _activeImportFiles.value.filterNot { it.id == id }
    }

    override fun clearImportFiles() {
        _activeImportFiles.value = emptyList()
    }

    override fun search(query: String, filterType: ContentType?): List<SearchResult> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            val results = mutableListOf<SearchResult>()
            val matchingDocs = documentDao.searchDocuments(q)
            for (docEntity in matchingDocs) {
                val contentType = try { ContentType.valueOf(docEntity.type) } catch (e: Exception) { ContentType.DOCUMENT }
                if (filterType != null && contentType != filterType) continue

                val pages = pageDao.getPagesForDocumentSync(docEntity.id).map { p ->
                    DocumentPage(pageNumber = p.pageNumber, imageUri = p.imagePath, ocrText = p.ocrText)
                }
                val doc = mapEntityToDocument(docEntity, pages)

                if (doc.title.lowercase().contains(q)) {
                    results.add(SearchResult(doc, 1, "تطابق في عنوان الوثيقة: «${doc.title}»", "العنوان"))
                }
                if (doc.author?.lowercase()?.contains(q) == true) {
                    results.add(SearchResult(doc, 1, "تطابق في اسم المؤلف: «${doc.author}»", "المؤلف"))
                }
            }

            val matchingPages = pageDao.searchPageOcrText(q)
            for (p in matchingPages) {
                val docEntity = documentDao.getDocumentById(p.documentId) ?: continue
                val contentType = try { ContentType.valueOf(docEntity.type) } catch (e: Exception) { ContentType.DOCUMENT }
                if (filterType != null && contentType != filterType) continue

                val pages = pageDao.getPagesForDocumentSync(docEntity.id).map {
                    DocumentPage(pageNumber = it.pageNumber, imageUri = it.imagePath, ocrText = it.ocrText)
                }
                val doc = mapEntityToDocument(docEntity, pages)

                val idx = p.ocrText.lowercase().indexOf(q)
                val start = maxOf(0, idx - 30)
                val end = minOf(p.ocrText.length, idx + q.length + 35)
                val snippet = "..." + p.ocrText.substring(start, end).trim() + "..."
                results.add(SearchResult(doc, p.pageNumber, snippet, "نص OCR"))
            }

            results
        }
    }

    override suspend fun performOcrForPage(documentId: String, pageNumber: Int): String = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesForDocumentSync(documentId)
        val targetPage = pages.find { it.pageNumber == pageNumber } ?: return@withContext ""
        val docPage = DocumentPage(
            pageNumber = targetPage.pageNumber,
            imageUri = targetPage.imagePath,
            imagePath = targetPage.imagePath,
            thumbnailPath = targetPage.thumbnailPath,
            ocrText = targetPage.ocrText
        )
        val result = ocrEngine.extractText(docPage)
        if (result.extractedText.isNotBlank()) {
            pageDao.updateOcrText(targetPage.id, result.extractedText)
        }
        result.extractedText
    }

    override fun getStorageManager(): LocalStorageManager = storageManager

    private fun persistCurrentSession(pages: List<ScannedPage>) {
        scope.launch {
            val sid = _currentSessionId.value
            scanSessionDao.insertSession(
                ScanSessionEntity(
                    id = sid,
                    startedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isCompleted = false
                )
            )
            val sessionPages = pages.map { p ->
                ScanSessionPageEntity(
                    id = p.id,
                    sessionId = sid,
                    pageNumber = p.pageNumber,
                    imagePath = p.imagePath ?: p.imageUri ?: "",
                    thumbnailPath = p.thumbnailPath,
                    rotationDegrees = p.rotationDegrees,
                    createdAt = System.currentTimeMillis()
                )
            }
            scanSessionDao.clearSessionPages(sid)
            scanSessionDao.insertSessionPages(sessionPages)
        }
    }

    private fun mapEntityToDocument(entity: DocumentEntity, pages: List<DocumentPage>): LibraryDocument {
        val type = try {
            ContentType.valueOf(entity.type)
        } catch (e: Exception) {
            ContentType.DOCUMENT
        }
        val ocrStatus = when (entity.ocrStatus) {
            "COMPLETED" -> OcrStatus.COMPLETED
            "PROCESSING" -> OcrStatus.PROCESSING
            "FAILED" -> OcrStatus.FAILED
            else -> if (pages.any { it.ocrStatus == OcrStatus.COMPLETED }) OcrStatus.COMPLETED else OcrStatus.NOT_PROCESSED
        }

        return LibraryDocument(
            id = entity.id,
            title = entity.title,
            type = type,
            pageCount = pages.size.coerceAtLeast(entity.pageCount),
            dateAdded = formatTimestampArabic(entity.createdAt),
            lastOpened = formatTimestampArabic(entity.lastOpenedAt),
            ocrStatus = ocrStatus,
            isFavorite = entity.isFavorite,
            author = entity.author,
            subjectCategory = entity.subjectCategory,
            description = entity.description,
            pages = pages,
            lastReadPage = entity.lastReadPage,
            fileSizeFormatted = entity.fileSizeFormatted,
            thumbnailPath = entity.thumbnailPath ?: pages.firstOrNull()?.thumbnailPath ?: pages.firstOrNull()?.imagePath
        )
    }

    private fun formatTimestampArabic(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "الآن"
            diff < 3600_000 -> "منذ ${diff / 60_000} دقيقة"
            diff < 86400_000 -> "اليوم"
            diff < 172800_000 -> "أمس"
            else -> {
                val sdf = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes بايت"
            bytes < 1024 * 1024 -> "${bytes / 1024} كيلوبايت"
            else -> String.format(Locale.US, "%.1f ميجابايت", bytes.toFloat() / (1024 * 1024))
        }
    }
}
