package com.example.domain.repository

import com.example.data.storage.LocalStorageManager
import com.example.domain.model.Bookmark
import com.example.domain.model.ContentType
import com.example.domain.model.DocumentPage
import com.example.domain.model.FileImportItem
import com.example.domain.model.LibraryDocument
import com.example.domain.model.OcrStatus
import com.example.domain.model.ScannedPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface LibraryRepository {
    val documents: Flow<List<LibraryDocument>>
    val bookmarks: Flow<List<Bookmark>>
    val activeScanPages: Flow<List<ScannedPage>>
    val activeImportFiles: Flow<List<FileImportItem>>
    val hasUnfinishedSession: Flow<Boolean>

    fun getDocumentById(id: String): LibraryDocument?
    fun toggleFavorite(id: String)
    fun deleteDocument(id: String)
    fun updateDocumentTitle(id: String, newTitle: String)
    fun updateDocumentType(id: String, newType: ContentType)
    fun updateLastReadPage(id: String, page: Int)

    // Bookmarks
    fun addBookmark(documentId: String, pageNumber: Int, title: String, note: String)
    fun removeBookmark(bookmarkId: String)

    // Scan Session
    fun addScannedPage(page: ScannedPage)
    fun removeScannedPage(id: String)
    fun rotateScannedPage(id: String)
    fun reorderScannedPages(fromIndex: Int, toIndex: Int)
    fun clearScanSession()
    suspend fun saveScannedPagesAsDocument(
        title: String,
        type: ContentType,
        author: String?,
        description: String?
    ): String
    suspend fun cropScannedPage(id: String, left: Float, top: Float, right: Float, bottom: Float)
    suspend fun enhanceScannedPage(id: String)
    suspend fun restoreUnfinishedSession()

    // Document Page Management
    suspend fun deleteDocumentPage(documentId: String, pageNumber: Int)
    suspend fun addPagesToExistingDocument(documentId: String, pages: List<ScannedPage>)

    // File/Image Import
    fun addImportFiles(files: List<FileImportItem>)
    fun removeImportFile(id: String)
    fun clearImportFiles()

    // Search
    fun search(query: String, filterType: ContentType? = null): List<SearchResult>

    // OCR Execution
    suspend fun performOcrForPage(documentId: String, pageNumber: Int): String

    fun getStorageManager(): LocalStorageManager?
}

data class SearchResult(
    val document: LibraryDocument,
    val pageNumber: Int,
    val snippetArabic: String,
    val matchType: String
)

/**
 * Empty In-Memory Fallback Repository
 */
class MockLibraryRepository(
    initialDocuments: List<LibraryDocument> = emptyList()
) : LibraryRepository {

    private val _documents = MutableStateFlow<List<LibraryDocument>>(initialDocuments)
    override val documents = _documents.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    override val bookmarks = _bookmarks.asStateFlow()

    private val _activeScanPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    override val activeScanPages = _activeScanPages.asStateFlow()

    private val _activeImportFiles = MutableStateFlow<List<FileImportItem>>(emptyList())
    override val activeImportFiles = _activeImportFiles.asStateFlow()

    private val _hasUnfinishedSession = MutableStateFlow(false)
    override val hasUnfinishedSession = _hasUnfinishedSession.asStateFlow()

    override fun getDocumentById(id: String): LibraryDocument? {
        return _documents.value.find { it.id == id }
    }

    override fun toggleFavorite(id: String) {
        _documents.value = _documents.value.map { doc ->
            if (doc.id == id) doc.copy(isFavorite = !doc.isFavorite) else doc
        }
    }

    override fun deleteDocument(id: String) {
        _documents.value = _documents.value.filterNot { it.id == id }
        _bookmarks.value = _bookmarks.value.filterNot { it.documentId == id }
    }

    override fun updateDocumentTitle(id: String, newTitle: String) {
        _documents.value = _documents.value.map { doc ->
            if (doc.id == id) doc.copy(title = newTitle) else doc
        }
    }

    override fun updateDocumentType(id: String, newType: ContentType) {
        _documents.value = _documents.value.map { doc ->
            if (doc.id == id) doc.copy(type = newType) else doc
        }
    }

    override fun updateLastReadPage(id: String, page: Int) {
        _documents.value = _documents.value.map { doc ->
            if (doc.id == id) doc.copy(lastReadPage = page) else doc
        }
    }

    override fun addBookmark(documentId: String, pageNumber: Int, title: String, note: String) {
        val doc = getDocumentById(documentId) ?: return
        val newBookmark = Bookmark(
            id = "bm_${System.currentTimeMillis()}",
            documentId = documentId,
            documentTitle = doc.title,
            pageNumber = pageNumber,
            title = title,
            note = note,
            createdAt = "الآن"
        )
        _bookmarks.value = listOf(newBookmark) + _bookmarks.value
    }

    override fun removeBookmark(bookmarkId: String) {
        _bookmarks.value = _bookmarks.value.filterNot { it.id == bookmarkId }
    }

    override fun addScannedPage(page: ScannedPage) {
        _activeScanPages.value = _activeScanPages.value + page
        _hasUnfinishedSession.value = true
    }

    override fun removeScannedPage(id: String) {
        _activeScanPages.value = _activeScanPages.value.filterNot { it.id == id }
        _hasUnfinishedSession.value = _activeScanPages.value.isNotEmpty()
    }

    override fun rotateScannedPage(id: String) {
        _activeScanPages.value = _activeScanPages.value.map { page ->
            if (page.id == id) {
                page.copy(rotationDegrees = (page.rotationDegrees + 90) % 360)
            } else page
        }
    }

    override fun reorderScannedPages(fromIndex: Int, toIndex: Int) {
        val list = _activeScanPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _activeScanPages.value = list.mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }
        }
    }

    override fun clearScanSession() {
        _activeScanPages.value = emptyList()
        _hasUnfinishedSession.value = false
    }

    override suspend fun saveScannedPagesAsDocument(
        title: String,
        type: ContentType,
        author: String?,
        description: String?
    ): String {
        val docId = "doc_${System.currentTimeMillis()}"
        val pages = _activeScanPages.value.mapIndexed { index, scannedPage ->
            DocumentPage(
                pageNumber = index + 1,
                imageUri = scannedPage.imageUri,
                imagePath = scannedPage.imagePath,
                thumbnailPath = scannedPage.thumbnailPath,
                ocrStatus = OcrStatus.NOT_PROCESSED,
                ocrText = ""
            )
        }
        val newDoc = LibraryDocument(
            id = docId,
            title = title.ifBlank { "وثيقة ممسوحة جديدة" },
            type = type,
            pageCount = pages.size,
            dateAdded = "الآن",
            lastOpened = "الآن",
            ocrStatus = OcrStatus.NOT_PROCESSED,
            isFavorite = false,
            author = author,
            description = description,
            pages = pages,
            fileSizeFormatted = "${pages.size * 500} كيلوبايت",
            thumbnailPath = pages.firstOrNull()?.thumbnailPath ?: pages.firstOrNull()?.imagePath
        )
        _documents.value = listOf(newDoc) + _documents.value
        clearScanSession()
        return docId
    }

    override suspend fun cropScannedPage(id: String, left: Float, top: Float, right: Float, bottom: Float) {}

    override suspend fun enhanceScannedPage(id: String) {}

    override suspend fun restoreUnfinishedSession() {}

    override suspend fun deleteDocumentPage(documentId: String, pageNumber: Int) {
        val doc = getDocumentById(documentId) ?: return
        val newPages = doc.pages.filterNot { it.pageNumber == pageNumber }
            .mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }
        _documents.value = _documents.value.map {
            if (it.id == documentId) it.copy(pages = newPages, pageCount = newPages.size) else it
        }
    }

    override suspend fun addPagesToExistingDocument(documentId: String, pages: List<ScannedPage>) {
        val doc = getDocumentById(documentId) ?: return
        val startPage = doc.pages.size + 1
        val newDocPages = pages.mapIndexed { idx, sp ->
            DocumentPage(
                pageNumber = startPage + idx,
                imageUri = sp.imageUri,
                imagePath = sp.imagePath,
                thumbnailPath = sp.thumbnailPath,
                ocrStatus = OcrStatus.NOT_PROCESSED,
                ocrText = ""
            )
        }
        val combined = doc.pages + newDocPages
        _documents.value = _documents.value.map {
            if (it.id == documentId) it.copy(pages = combined, pageCount = combined.size) else it
        }
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

        val results = mutableListOf<SearchResult>()
        for (doc in _documents.value) {
            if (filterType != null && doc.type != filterType) continue
            if (doc.title.lowercase().contains(q)) {
                results.add(SearchResult(doc, 1, "تطابق في عنوان الوثيقة: «${doc.title}»", "العنوان"))
            }
            if (doc.author?.lowercase()?.contains(q) == true) {
                results.add(SearchResult(doc, 1, "تطابق في اسم المؤلف: «${doc.author}»", "المؤلف"))
            }
            for (page in doc.pages) {
                if (page.ocrText.lowercase().contains(q)) {
                    val idx = page.ocrText.lowercase().indexOf(q)
                    val start = maxOf(0, idx - 30)
                    val end = minOf(page.ocrText.length, idx + q.length + 35)
                    val snippet = "..." + page.ocrText.substring(start, end).trim() + "..."
                    results.add(SearchResult(doc, page.pageNumber, snippet, "نص OCR"))
                }
            }
        }
        return results
    }

    override suspend fun performOcrForPage(documentId: String, pageNumber: Int): String {
        return getDocumentById(documentId)?.pages?.find { it.pageNumber == pageNumber }?.ocrText ?: ""
    }

    override fun getStorageManager(): LocalStorageManager? = null
}
