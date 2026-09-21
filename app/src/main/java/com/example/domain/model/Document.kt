package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.OcrError
import com.example.ui.theme.OcrNotStarted
import com.example.ui.theme.OcrProcessing
import com.example.ui.theme.OcrSuccess

/**
 * OCR Processing Status states
 */
enum class OcrStatus(
    val labelArabic: String,
    val color: Color,
    val descriptionArabic: String
) {
    NOT_PROCESSED("لم تتم المعالجة", OcrNotStarted, "لم يتم استخراج النص بعد"),
    PROCESSING("جارٍ المعالجة", OcrProcessing, "جارٍ استخراج النص والتعرف الضوئي…"),
    COMPLETED("اكتملت", OcrSuccess, "تم استخراج النص بالكامل بنجاح"),
    FAILED("فشل", OcrError, "تعذر استخراج النص — يلزم إعادة المحاولة")
}

/**
 * Detected block or article inside a Magazine/Newspaper layout
 */
data class DetectedSection(
    val id: String,
    val title: String,
    val boundingDescription: String,
    val order: Int,
    val ocrText: String,
    val isSelected: Boolean = true
)

/**
 * Single document page representation
 */
data class DocumentPage(
    val pageNumber: Int,
    val imageUri: String? = null,
    val imagePath: String? = imageUri,
    val thumbnailPath: String? = null,
    val ocrText: String = "",
    val ocrStatus: OcrStatus = OcrStatus.NOT_PROCESSED,
    val detectedSections: List<DetectedSection> = emptyList(),
    val notes: String = ""
)

/**
 * Bookmark referencing a specific page inside a document with optional note
 */
data class Bookmark(
    val id: String,
    val documentId: String,
    val documentTitle: String,
    val pageNumber: Int,
    val title: String,
    val note: String = "",
    val createdAt: String
)

/**
 * Main digital library document model
 */
data class LibraryDocument(
    val id: String,
    val title: String,
    val type: ContentType,
    val pageCount: Int,
    val dateAdded: String,
    val lastOpened: String,
    val ocrStatus: OcrStatus,
    val isFavorite: Boolean = false,
    val author: String? = null,
    val subjectCategory: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val lastReadPage: Int = 1,
    val pages: List<DocumentPage> = emptyList(),
    val fileSizeFormatted: String = "4.2 ميجابايت",
    val thumbnailPath: String? = null
)
