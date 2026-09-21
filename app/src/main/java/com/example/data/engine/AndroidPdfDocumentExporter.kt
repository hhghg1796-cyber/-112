package com.example.data.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.example.domain.engine.DocumentExporter
import com.example.domain.model.ExportConfig
import com.example.domain.model.LibraryDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidPdfDocumentExporter(
    private val context: Context
) : DocumentExporter {

    override suspend fun exportDocument(
        document: LibraryDocument,
        config: ExportConfig,
        onProgress: (Float) -> Unit
    ): DocumentExporter.ExportResult = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
            val sanitizedTitle = document.title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val outputFile = File(exportDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

            val totalPages = document.pages.size
            if (totalPages == 0) {
                // If no pages, create a single informational page
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard pt
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 24f
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(document.title, 550f, 100f, paint)
                paint.textSize = 14f
                canvas.drawText("النوع: ${document.type.titleArabic}", 550f, 140f, paint)
                pdfDoc.finishPage(page)
            } else {
                for ((index, docPage) in document.pages.withIndex()) {
                    onProgress((index.toFloat()) / totalPages.toFloat())

                    val pageWidth = 595
                    val pageHeight = 842
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    // Draw page background
                    canvas.drawColor(Color.WHITE)

                    // Draw image if available
                    val imageFile = docPage.imagePath?.let { File(it) }
                    if (imageFile != null && imageFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                        if (bitmap != null) {
                            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                            val destRect = Rect(20, 20, pageWidth - 20, pageHeight - 50)
                            canvas.drawBitmap(bitmap, srcRect, destRect, null)
                        }
                    } else {
                        // Draw clean document representation
                        val borderPaint = Paint().apply {
                            color = Color.LTGRAY
                            style = Paint.Style.STROKE
                            strokeWidth = 2f
                        }
                        canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), (pageHeight - 50).toFloat(), borderPaint)

                        val textPaint = Paint().apply {
                            color = Color.DKGRAY
                            textSize = 18f
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.drawText("صفحة ${docPage.pageNumber}", (pageWidth / 2).toFloat(), 200f, textPaint)

                        if (docPage.ocrText.isNotBlank()) {
                            val ocrPaint = Paint().apply {
                                color = Color.BLACK
                                textSize = 12f
                                textAlign = Paint.Align.RIGHT
                            }
                            var y = 250f
                            val lines = docPage.ocrText.lines()
                            for (line in lines.take(20)) {
                                canvas.drawText(line, (pageWidth - 40).toFloat(), y, ocrPaint)
                                y += 18f
                            }
                        }
                    }

                    // Watermark / Header & Footer
                    val footerPaint = Paint().apply {
                        color = Color.GRAY
                        textSize = 10f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("${document.title} — صفحة ${docPage.pageNumber} من $totalPages", (pageWidth / 2).toFloat(), (pageHeight - 20).toFloat(), footerPaint)

                    pdfDoc.finishPage(page)
                }
            }

            onProgress(0.95f)

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()

            onProgress(1f)

            DocumentExporter.ExportResult(
                success = true,
                outputFilePath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                errorMessageArabic = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DocumentExporter.ExportResult(
                success = false,
                outputFilePath = null,
                fileSizeBytes = 0L,
                errorMessageArabic = "فشل تصدير المستند: ${e.localizedMessage}"
            )
        }
    }
}
