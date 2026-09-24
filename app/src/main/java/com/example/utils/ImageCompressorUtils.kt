package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class ImageOutputFormat(val displayName: String, val extension: String, val mimeType: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    WEBP("WEBP", "webp", "image/webp"),
    PNG("PNG", "png", "image/png")
}

data class CompressionResult(
    val compressedFile: File,
    val compressedBitmap: Bitmap,
    val originalWidth: Int,
    val originalHeight: Int,
    val originalSize: Long,
    val newWidth: Int,
    val newHeight: Int,
    val newSize: Long,
    val compressionRatioPercent: Int,
    val format: ImageOutputFormat
)

object ImageCompressorUtils {

    suspend fun compressImage(
        context: Context,
        imageUri: Uri,
        quality: Int, // 10..100
        scalePercent: Int, // 25..100
        outputFormat: ImageOutputFormat
    ): CompressionResult = withContext(Dispatchers.IO) {
        val originalSize = FileUtils.getFileSize(context, imageUri)

        // Decode bounds
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }
        val origW = boundsOptions.outWidth
        val origH = boundsOptions.outHeight

        // Target scaled dimensions
        val scaleFactor = scalePercent / 100f
        val targetW = (origW * scaleFactor).roundToInt().coerceAtLeast(1)
        val targetH = (origH * scaleFactor).roundToInt().coerceAtLeast(1)

        // Compute inSampleSize
        var sampleSize = 1
        while ((origW / (sampleSize * 2)) >= targetW && (origH / (sampleSize * 2)) >= targetH) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var sampledBitmap = context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalStateException("Could not load image")

        // Correct EXIF orientation for camera photos
        val exifRotation = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.contentResolver.openInputStream(imageUri)?.use { st ->
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
            val rotated = Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true)
            if (rotated != sampledBitmap) {
                sampledBitmap.recycle()
                sampledBitmap = rotated
            }
        }

        // Fine scale if needed
        val scaledBitmap = if (sampledBitmap.width != targetW || sampledBitmap.height != targetH) {
            val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
            if (scaled != sampledBitmap) sampledBitmap.recycle()
            scaled
        } else {
            sampledBitmap
        }

        // Compress
        val compressFormat = when (outputFormat) {
            ImageOutputFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageOutputFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            ImageOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
        }

        val byteStream = ByteArrayOutputStream()
        scaledBitmap.compress(compressFormat, quality, byteStream)
        val compressedBytes = byteStream.toByteArray()

        val cacheDir = File(context.cacheDir, "compressed_images").apply { mkdirs() }
        val outFile = File(cacheDir, "comp_${System.currentTimeMillis()}.${outputFormat.extension}")
        FileOutputStream(outFile).use { it.write(compressedBytes) }

        val newSize = outFile.length()
        val ratio = if (originalSize > 0) {
            val reduction = ((originalSize - newSize).toFloat() / originalSize * 100f).roundToInt()
            reduction.coerceIn(-100, 99)
        } else {
            0
        }

        CompressionResult(
            compressedFile = outFile,
            compressedBitmap = scaledBitmap,
            originalWidth = origW,
            originalHeight = origH,
            originalSize = originalSize,
            newWidth = targetW,
            newHeight = targetH,
            newSize = newSize,
            compressionRatioPercent = ratio,
            format = outputFormat
        )
    }

    /**
     * Extracts top 6 dominant colors from an image
     */
    suspend fun extractColors(
        context: Context,
        imageUri: Uri
    ): List<Int> = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 8
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@withContext emptyList()

        val colorBuckets = mutableMapOf<Int, Int>()
        val width = bitmap.width
        val height = bitmap.height

        // Sample pixels with quantization (group similar colors)
        val step = 4
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = (android.graphics.Color.red(pixel) / 32) * 32
                val g = (android.graphics.Color.green(pixel) / 32) * 32
                val b = (android.graphics.Color.blue(pixel) / 32) * 32
                val quantized = android.graphics.Color.rgb(r, g, b)
                colorBuckets[quantized] = (colorBuckets[quantized] ?: 0) + 1
            }
        }
        bitmap.recycle()

        // Return top 6 distinct colors with minimum distance between each other
        val sortedEntries = colorBuckets.entries.sortedByDescending { it.value }
        val distinctColors = mutableListOf<Int>()

        fun colorDistanceSq(c1: Int, c2: Int): Int {
            val dr = android.graphics.Color.red(c1) - android.graphics.Color.red(c2)
            val dg = android.graphics.Color.green(c1) - android.graphics.Color.green(c2)
            val db = android.graphics.Color.blue(c1) - android.graphics.Color.blue(c2)
            return dr * dr + dg * dg + db * db
        }

        val minDistanceSq = 32 * 32 // Min distance threshold

        for (entry in sortedEntries) {
            val color = entry.key
            val isFarEnough = distinctColors.none { existing -> colorDistanceSq(existing, color) < minDistanceSq }
            if (isFarEnough) {
                distinctColors.add(color)
                if (distinctColors.size >= 6) break
            }
        }

        // If very few distinct colors found (e.g. monotone image), backfill with top entries
        if (distinctColors.size < 4) {
            for (entry in sortedEntries) {
                if (!distinctColors.contains(entry.key)) {
                    distinctColors.add(entry.key)
                    if (distinctColors.size >= 6) break
                }
            }
        }

        distinctColors
    }
}
