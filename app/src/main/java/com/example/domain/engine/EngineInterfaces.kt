package com.example.domain.engine

import com.example.domain.model.DocumentPage
import com.example.domain.model.ExportConfig
import com.example.domain.model.LibraryDocument
import com.example.domain.model.OcrStatus

/**
 * Future Offline Arabic OCR Engine Contract
 */
interface OcrEngine {
    data class OcrResult(
        val status: OcrStatus,
        val extractedText: String,
        val confidenceScore: Float,
        val processingDurationMs: Long,
        val detectedLanguage: String = "ar"
    )

    suspend fun extractText(page: DocumentPage): OcrResult
    suspend fun cancelOcr(pageNumber: Int)
}

/**
 * Future Scanner & Frame Detection Engine Contract
 */
interface ScannerEngine {
    data class DetectedFrame(
        val isDocumentDetected: Boolean,
        val cornerPoints: List<Pair<Float, Float>>,
        val lightingCondition: LightingCondition,
        val isSteady: Boolean
    )

    enum class LightingCondition { OPTIMAL, LOW_LIGHT, GLARE }

    fun initialize()
    fun release()
    fun setFlashMode(enabled: Boolean)
}

/**
 * Multi-stage pipeline processor
 */
interface DocumentProcessor {
    enum class PipelineStep(val titleArabic: String, val descriptionArabic: String) {
        DESKEW("تصحيح ميل الصفحة", "محاذاة زوايا المستند وتعديل الانحناء الهندسي"),
        ENHANCEMENT("تحسين الصورة", "إزالة الضوضاء وتعديل التباين الأبيض والأسود"),
        LAYOUT_ANALYSIS("تحليل التخطيط", "اكتشاف الأعمدة والفقرات والجداول التراثية"),
        OCR("استخراج النص (OCR)", "التعرف الضوئي على الحروف والكلمات العربية"),
        SAVE("حفظ في المكتبة", "تشفير الفهرس المحلي وبناء أرشيف المستند")
    }

    data class ProcessingResult(
        val success: Boolean,
        val messageArabic: String,
        val processedPagesCount: Int
    )

    suspend fun executePipeline(
        documentId: String,
        onStepProgress: (PipelineStep, Float) -> Unit
    ): ProcessingResult
}

/**
 * Document & PDF Export Engine Contract
 */
interface DocumentExporter {
    data class ExportResult(
        val success: Boolean,
        val outputFilePath: String?,
        val fileSizeBytes: Long,
        val errorMessageArabic: String? = null
    )

    suspend fun exportDocument(
        document: LibraryDocument,
        config: ExportConfig,
        onProgress: (Float) -> Unit
    ): ExportResult
}
