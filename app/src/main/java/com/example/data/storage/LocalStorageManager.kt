package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class LocalStorageManager(private val context: Context) {

    private val baseDocumentsDir: File
        get() = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    private val baseSessionsDir: File
        get() = File(context.filesDir, "sessions").apply { if (!exists()) mkdirs() }

    fun getDocumentPagesDir(documentId: String): File {
        return File(File(baseDocumentsDir, documentId), "pages").apply { if (!exists()) mkdirs() }
    }

    fun getDocumentThumbnailsDir(documentId: String): File {
        return File(File(baseDocumentsDir, documentId), "thumbnails").apply { if (!exists()) mkdirs() }
    }

    fun getSessionPagesDir(sessionId: String): File {
        return File(File(baseSessionsDir, sessionId), "pages").apply { if (!exists()) mkdirs() }
    }

    fun getSessionThumbnailsDir(sessionId: String): File {
        return File(File(baseSessionsDir, sessionId), "thumbnails").apply { if (!exists()) mkdirs() }
    }

    suspend fun saveBitmapToSession(
        sessionId: String,
        bitmap: Bitmap,
        pageNumber: Int,
        rotationDegrees: Int = 0
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val pagesDir = getSessionPagesDir(sessionId)
        val thumbsDir = getSessionThumbnailsDir(sessionId)

        val timestamp = System.currentTimeMillis()
        val imageFile = File(pagesDir, "scan_p${pageNumber}_$timestamp.jpg")
        val thumbFile = File(thumbsDir, "thumb_p${pageNumber}_$timestamp.jpg")

        val finalBitmap = if (rotationDegrees != 0) {
            rotateBitmap(bitmap, rotationDegrees)
        } else {
            bitmap
        }

        FileOutputStream(imageFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        val thumbBitmap = createThumbnailBitmap(finalBitmap, 320)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        Pair(imageFile.absolutePath, thumbFile.absolutePath)
    }

    suspend fun saveImageFileToSession(
        sessionId: String,
        file: File,
        pageNumber: Int
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
            saveBitmapToSession(sessionId, bitmap, pageNumber)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importFromUri(
        sessionId: String,
        uri: Uri,
        pageNumber: Int
    ): Pair<String, String>? = saveUriToSession(sessionId, uri, pageNumber)

    suspend fun saveUriToSession(
        sessionId: String,
        uri: Uri,
        pageNumber: Int
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeSampledBitmapFromUri(uri, 1800, 2400) ?: return@withContext null
            saveBitmapToSession(sessionId, bitmap, pageNumber)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun promoteSessionPagesToDocument(
        sessionId: String,
        documentId: String,
        pages: List<Pair<Int, String>> // list of (pageNumber, currentSessionImagePath)
    ): List<Triple<Int, String, String>> = withContext(Dispatchers.IO) {
        val docPagesDir = getDocumentPagesDir(documentId)
        val docThumbsDir = getDocumentThumbnailsDir(documentId)
        val result = mutableListOf<Triple<Int, String, String>>()

        for ((pageNumber, sessionImagePath) in pages) {
            val sourceFile = File(sessionImagePath)
            val timestamp = System.currentTimeMillis()
            val destImageFile = File(docPagesDir, "doc_p${pageNumber}_$timestamp.jpg")
            val destThumbFile = File(docThumbsDir, "thumb_p${pageNumber}_$timestamp.jpg")

            if (sourceFile.exists()) {
                sourceFile.copyTo(destImageFile, overwrite = true)
                val bitmap = decodeSampledBitmapFromFile(destImageFile.absolutePath, 400, 400)
                if (bitmap != null) {
                    val thumb = createThumbnailBitmap(bitmap, 320)
                    FileOutputStream(destThumbFile).use { out ->
                        thumb.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                }
            }

            result.add(
                Triple(
                    pageNumber,
                    destImageFile.absolutePath,
                    if (destThumbFile.exists()) destThumbFile.absolutePath else destImageFile.absolutePath
                )
            )
        }

        // Clean up session files
        deleteSessionFiles(sessionId)

        result
    }

    suspend fun rotateImageFile(imagePath: String, degrees: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) return@withContext false

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
            val rotated = rotateBitmap(bitmap, degrees)

            FileOutputStream(file).use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun enhanceImageFile(imagePath: String, contrast: Float = 1.25f, brightness: Float = 10f): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) return@withContext false

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
            val enhanced = applyEnhancement(bitmap, contrast, brightness)

            FileOutputStream(file).use { out ->
                enhanced.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun cropImageFile(imagePath: String, leftRatio: Float, topRatio: Float, rightRatio: Float, bottomRatio: Float): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) return@withContext false

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
            val width = bitmap.width
            val height = bitmap.height

            val left = (width * leftRatio.coerceIn(0f, 0.4f)).toInt()
            val top = (height * topRatio.coerceIn(0f, 0.4f)).toInt()
            val right = (width * rightRatio.coerceIn(0.6f, 1f)).toInt()
            val bottom = (height * bottomRatio.coerceIn(0.6f, 1f)).toInt()

            val cropWidth = (right - left).coerceAtLeast(100)
            val cropHeight = (bottom - top).coerceAtLeast(100)

            val cropped = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)

            FileOutputStream(file).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteDocumentFiles(documentId: String) {
        try {
            val docDir = File(baseDocumentsDir, documentId)
            if (docDir.exists()) {
                docDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteSessionFiles(sessionId: String) {
        try {
            val sessionDir = File(baseSessionsDir, sessionId)
            if (sessionDir.exists()) {
                sessionDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(angle.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun createThumbnailBitmap(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        val ratio = width.toFloat() / height.toFloat()

        val (targetWidth, targetHeight) = if (ratio > 1) {
            Pair(maxDimension, (maxDimension / ratio).toInt())
        } else {
            Pair((maxDimension * ratio).toInt(), maxDimension)
        }

        return Bitmap.createScaledBitmap(source, targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1), true)
    }

    private fun applyEnhancement(src: Bitmap, contrast: Float, brightness: Float): Bitmap {
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val ret = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(ret)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return ret
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
