package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

data class PdfPageInfo(
    val pageIndex: Int,
    val width: Int,
    val height: Int,
    val previewBitmap: Bitmap? = null
)

data class ExtractedPage(
    val pageIndex: Int,
    val file: File,
    val width: Int,
    val height: Int,
    val fileSize: Long
)

object PdfUtils {

    /**
     * Gets total page count and thumbnail previews for PDF
     */
    suspend fun inspectPdf(
        context: Context,
        pdfUri: Uri,
        maxPreviewPages: Int = 10
    ): Pair<Int, List<PdfPageInfo>> = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
            ?: throw IllegalStateException("Could not open PDF file")

        pfd.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            val pageCount = renderer.pageCount
            val pages = mutableListOf<PdfPageInfo>()

            val limit = min(pageCount, maxPreviewPages)
            for (i in 0 until limit) {
                renderer.openPage(i).use { page ->
                    val w = page.width
                    val h = page.height
                    // Generate small thumbnail
                    val thumbScale = 300f / maxOf(w, h)
                    val thumbW = (w * thumbScale).toInt().coerceAtLeast(1)
                    val thumbH = (h * thumbScale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
                    // White background for transparent pages
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    pages.add(PdfPageInfo(i, w, h, bitmap))
                }
            }
            renderer.close()
            Pair(pageCount, pages)
        }
    }

    /**
     * Renders specific pages to image files (JPEG or PNG) with scale multiplier
     */
    suspend fun renderPagesToImages(
        context: Context,
        pdfUri: Uri,
        pageIndices: List<Int>,
        scale: Float = 2.0f,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 92,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<ExtractedPage> = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
            ?: throw IllegalStateException("Could not open PDF file")

        val outputDir = File(context.cacheDir, "pdf_extracted_${System.currentTimeMillis()}").apply { mkdirs() }
        val results = mutableListOf<ExtractedPage>()

        pfd.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            val total = pageIndices.size

            pageIndices.forEachIndexed { idx, pageNum ->
                if (pageNum < renderer.pageCount) {
                    renderer.openPage(pageNum).use { page ->
                        val targetW = (page.width * scale).toInt().coerceAtLeast(1)
                        val targetH = (page.height * scale).toInt().coerceAtLeast(1)

                        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
                        val file = File(outputDir, "page_${pageNum + 1}.$ext")
                        FileOutputStream(file).use { out ->
                            bitmap.compress(format, quality, out)
                        }
                        bitmap.recycle()

                        results.add(
                            ExtractedPage(
                                pageIndex = pageNum,
                                file = file,
                                width = targetW,
                                height = targetH,
                                fileSize = file.length()
                            )
                        )
                    }
                }
                onProgress(idx + 1, total)
            }
            renderer.close()
        }

        results
    }

    /**
     * Converts multiple image Uris into a single A4 or custom sized PDF document
     */
    suspend fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        marginPoints: Int = 20,
        compressQuality: Int = 85,
        onProgress: (current: Int, total: Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val total = imageUris.size
        val a4Width = 595 // Standard A4 points (72 dpi)
        val a4Height = 842

        try {
            imageUris.forEachIndexed { index, uri ->
                // Decode bitmap bounds first to avoid OOM
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                // Sample down if image is huge
                var sampleSize = 1
                val maxDim = 2400
                if (options.outHeight > maxDim || options.outWidth > maxDim) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / sampleSize) >= maxDim && (halfWidth / sampleSize) >= maxDim) {
                        sampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                var bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                } ?: throw IllegalStateException("Failed to decode image at index $index")

                // Handle camera EXIF rotation (Android 7.0+)
                val exifRotation = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        context.contentResolver.openInputStream(uri)?.use { st ->
                            val exif = android.media.ExifInterface(st)
                            when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
                                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                                else -> 0f
                            }
                        } ?: 0f
                    } else 0f
                } catch (_: Exception) { 0f }

                if (exifRotation != 0f) {
                    val matrix = android.graphics.Matrix().apply { postRotate(exifRotation) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotated != bitmap) {
                        bitmap.recycle()
                        bitmap = rotated
                    }
                }

                // Adaptive page orientation: Landscape photo -> Landscape A4; Portrait -> Portrait A4
                val isLandscape = bitmap.width > bitmap.height
                val curPageWidth = if (isLandscape) 842 else 595
                val curPageHeight = if (isLandscape) 595 else 842

                // Page dimensions
                val pageInfo = PdfDocument.PageInfo.Builder(curPageWidth, curPageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Fill background white
                val bgPaint = Paint().apply { color = Color.WHITE }
                canvas.drawRect(0f, 0f, curPageWidth.toFloat(), curPageHeight.toFloat(), bgPaint)

                // Calculate fitted bounds within margin
                val usableWidth = (curPageWidth - marginPoints * 2).toFloat()
                val usableHeight = (curPageHeight - marginPoints * 2).toFloat()

                val imgW = bitmap.width.toFloat()
                val imgH = bitmap.height.toFloat()

                val scale = min(usableWidth / imgW, usableHeight / imgH)
                val destW = imgW * scale
                val destH = imgH * scale

                val left = marginPoints + (usableWidth - destW) / 2f
                val top = marginPoints + (usableHeight - destH) / 2f

                val destRect = Rect(left.toInt(), top.toInt(), (left + destW).toInt(), (top + destH).toInt())
                val paint = Paint().apply { isFilterBitmap = true }
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(page)
                bitmap.recycle()

                onProgress(index + 1, total)
            }

            val outFile = File(context.cacheDir, "omnitool_doc_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }
            outFile
        } finally {
            pdfDocument.close()
        }
    }
}
