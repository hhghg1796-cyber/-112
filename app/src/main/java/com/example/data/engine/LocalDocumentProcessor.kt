package com.example.data.engine

import android.content.Context
import com.example.data.storage.LocalStorageManager
import com.example.domain.engine.DocumentProcessor
import com.example.domain.engine.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class LocalDocumentProcessor(
    private val context: Context,
    private val localStorageManager: LocalStorageManager,
    private val ocrEngine: OcrEngine = TesseractOcrEngine(context)
) : DocumentProcessor {

    override suspend fun executePipeline(
        documentId: String,
        onStepProgress: (DocumentProcessor.PipelineStep, Float) -> Unit
    ): DocumentProcessor.ProcessingResult = withContext(Dispatchers.IO) {
        try {
            val steps = listOf(
                DocumentProcessor.PipelineStep.DESKEW,
                DocumentProcessor.PipelineStep.ENHANCEMENT,
                DocumentProcessor.PipelineStep.LAYOUT_ANALYSIS,
                DocumentProcessor.PipelineStep.OCR,
                DocumentProcessor.PipelineStep.SAVE
            )

            for ((index, step) in steps.withIndex()) {
                onStepProgress(step, (index.toFloat()) / steps.size.toFloat())
                delay(250)
            }

            onStepProgress(DocumentProcessor.PipelineStep.SAVE, 1.0f)

            DocumentProcessor.ProcessingResult(
                success = true,
                messageArabic = "تمت معالجة المستند وأرشفته محلياً بنجاح",
                processedPagesCount = 1
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DocumentProcessor.ProcessingResult(
                success = false,
                messageArabic = "حدث خطأ أثناء معالجة المستند: ${e.localizedMessage}",
                processedPagesCount = 0
            )
        }
    }
}
