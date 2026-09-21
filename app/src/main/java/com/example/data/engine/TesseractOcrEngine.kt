package com.example.data.engine

import android.content.Context
import android.graphics.BitmapFactory
import com.example.domain.engine.OcrEngine
import com.example.domain.model.DocumentPage
import com.example.domain.model.OcrStatus
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TesseractOcrEngine(
    private val context: Context
) : OcrEngine {

    @Volatile
    private var isInitialized = false
    private var tessBaseApi: TessBaseAPI? = null
    private val initLock = Any()

    /**
     * Ensures traineddata models (Arabic and English) exist in app internal storage tessdata directory.
     */
    private fun ensureTrainedData(): File {
        val tessDir = File(context.filesDir, "tessdata").apply {
            if (!exists()) mkdirs()
        }
        val models = listOf("ara.traineddata", "eng.traineddata")

        for (modelName in models) {
            val destFile = File(tessDir, modelName)
            if (!destFile.exists() || destFile.length() == 0L) {
                try {
                    context.assets.open("tessdata/$modelName").use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return tessDir
    }

    private fun initTessApi(): Boolean {
        if (isInitialized && tessBaseApi != null) return true
        synchronized(initLock) {
            if (isInitialized && tessBaseApi != null) return true
            return try {
                ensureTrainedData()
                val api = TessBaseAPI()
                // context.filesDir is the parent directory containing the "tessdata" folder
                val dataPath = context.filesDir.absolutePath
                val araExists = File(context.filesDir, "tessdata/ara.traineddata").exists()
                val engExists = File(context.filesDir, "tessdata/eng.traineddata").exists()

                val lang = when {
                    araExists && engExists -> "ara+eng"
                    araExists -> "ara"
                    engExists -> "eng"
                    else -> "eng"
                }

                val ok = api.init(dataPath, lang)
                if (ok) {
                    api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                    tessBaseApi = api
                    isInitialized = true
                    true
                } else {
                    false
                }
            } catch (t: Throwable) {
                // Handle JVM / link issues gracefully in environments without Android C++ runtime
                t.printStackTrace()
                false
            }
        }
    }

    override suspend fun extractText(page: DocumentPage): OcrEngine.OcrResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val path = page.imagePath ?: page.imageUri
        if (path.isNullOrBlank()) {
            return@withContext OcrEngine.OcrResult(
                status = OcrStatus.FAILED,
                extractedText = "",
                confidenceScore = 0f,
                processingDurationMs = 0L,
                detectedLanguage = "ar"
            )
        }

        val file = File(path)
        if (!file.exists()) {
            return@withContext OcrEngine.OcrResult(
                status = OcrStatus.FAILED,
                extractedText = "",
                confidenceScore = 0f,
                processingDurationMs = 0L,
                detectedLanguage = "ar"
            )
        }

        try {
            val initialized = initTessApi()
            if (!initialized) {
                return@withContext OcrEngine.OcrResult(
                    status = OcrStatus.FAILED,
                    extractedText = "",
                    confidenceScore = 0f,
                    processingDurationMs = System.currentTimeMillis() - startTime,
                    detectedLanguage = "ar"
                )
            }

            val api = tessBaseApi ?: return@withContext OcrEngine.OcrResult(
                status = OcrStatus.FAILED,
                extractedText = "",
                confidenceScore = 0f,
                processingDurationMs = System.currentTimeMillis() - startTime,
                detectedLanguage = "ar"
            )

            val text: String
            val confidence: Float
            synchronized(api) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    api.setImage(bitmap)
                    text = api.getUTF8Text() ?: ""
                    confidence = api.meanConfidence().toFloat()
                    api.clear()
                } else {
                    text = ""
                    confidence = 0f
                }
            }

            val duration = System.currentTimeMillis() - startTime
            val status = if (text.isNotBlank()) OcrStatus.COMPLETED else OcrStatus.NOT_PROCESSED

            OcrEngine.OcrResult(
                status = status,
                extractedText = text.trim(),
                confidenceScore = confidence,
                processingDurationMs = duration,
                detectedLanguage = if (containsArabic(text)) "ar" else "en"
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            OcrEngine.OcrResult(
                status = OcrStatus.FAILED,
                extractedText = "",
                confidenceScore = 0f,
                processingDurationMs = System.currentTimeMillis() - startTime,
                detectedLanguage = "ar"
            )
        }
    }

    override suspend fun cancelOcr(pageNumber: Int) {
        try {
            tessBaseApi?.stop()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun containsArabic(text: String): Boolean {
        for (char in text) {
            val ub = Character.UnicodeBlock.of(char)
            if (ub == Character.UnicodeBlock.ARABIC ||
                ub == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
                ub == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
                ub == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
            ) {
                return true
            }
        }
        return false
    }

    fun release() {
        synchronized(initLock) {
            try {
                tessBaseApi?.recycle()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            tessBaseApi = null
            isInitialized = false
        }
    }
}
