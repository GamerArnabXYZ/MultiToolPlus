package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

object FileUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups]
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "file"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: "file"
                    }
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment?.let { name = it }
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        return cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        return try {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Saves an existing image file directly to MediaStore / Pictures/OmniTool without re-encoding.
     * Prevents OOM and preserves exact original format (WEBP, PNG, JPG).
     */
    fun saveImageFileToGallery(
        context: Context,
        imageFile: File,
        mimeType: String,
        filename: String
    ): Uri? {
        val extension = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        val fullFilename = if (filename.endsWith(".$extension")) filename else "$filename.$extension"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fullFilename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/OmniTool")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

            return try {
                resolver.openOutputStream(uri)?.use { outStream ->
                    imageFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (e: Exception) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
                null
            }
        } else {
            // Android 6.0 to 9.0
            return try {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val targetDir = File(picturesDir, "OmniTool").apply { mkdirs() }
                val targetFile = File(targetDir, fullFilename)
                imageFile.inputStream().use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                Uri.fromFile(targetFile)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Saves a bitmap directly to MediaStore / Pictures/OmniTool.
     * Fully compatible from Android 6.0 (API 23) up to Android 15/16.
     * Handles both Scoped Storage (API 29+) and legacy public storage (API 23-28).
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95
    ): Uri? {
        val extension = when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            else -> "jpg"
        }
        val mimeType = when (format) {
            Bitmap.CompressFormat.PNG -> "image/png"
            else -> "image/jpeg"
        }
        val fullFilename = if (filename.endsWith(".$extension")) filename else "$filename.$extension"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fullFilename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/OmniTool")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

            return try {
                resolver.openOutputStream(uri)?.use { outStream ->
                    bitmap.compress(format, quality, outStream)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (e: Exception) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
                null
            }
        } else {
            // Android 6.0 to 9.0 (API 23 - 28)
            return try {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val targetDir = File(picturesDir, "OmniTool").apply { mkdirs() }
                val targetFile = File(targetDir, fullFilename)
                FileOutputStream(targetFile).use { outStream ->
                    bitmap.compress(format, quality, outStream)
                }
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                Uri.fromFile(targetFile)
            } catch (e: Exception) {
                // Secondary fallback using ContentResolver insertImage
                try {
                    val fallbackUriStr = MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        fullFilename,
                        "Generated by OmniTool Plus"
                    )
                    if (fallbackUriStr != null) Uri.parse(fallbackUriStr) else null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Saves a PDF or document to Downloads / Documents.
     * Fully compatible from Android 6.0 (API 23) up to Android 15/16.
     */
    fun savePdfToStorage(
        context: Context,
        pdfFile: File,
        desiredName: String
    ): Uri? {
        val fullFilename = if (desiredName.endsWith(".pdf", ignoreCase = true)) desiredName else "$desiredName.pdf"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fullFilename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/OmniTool")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: resolver.insert(MediaStore.Files.getContentUri("external"), values)
                ?: return null

            return try {
                resolver.openOutputStream(uri)?.use { outStream ->
                    pdfFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (e: Exception) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
                null
            }
        } else {
            // Android 6.0 to 9.0 (API 23 - 28)
            return try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "OmniTool").apply { mkdirs() }
                val targetFile = File(targetDir, fullFilename)
                pdfFile.copyTo(targetFile, overwrite = true)
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
                Uri.fromFile(targetFile)
            } catch (e: Exception) {
                try {
                    val appDocsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OmniTool").apply { mkdirs() }
                    val targetFile = File(appDocsDir, fullFilename)
                    pdfFile.copyTo(targetFile, overwrite = true)
                    Uri.fromFile(targetFile)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Shares a file via Intent with FileProvider
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /**
     * Opens a file using default system app via FileProvider
     */
    fun openFile(context: Context, file: File, mimeType: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "No app available to open this file", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves a bitmap temporarily to cache to share it
     */
    fun shareBitmap(context: Context, bitmap: Bitmap, filename: String, title: String) {
        try {
            val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(cacheDir, "$filename.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            shareFile(context, file, "image/png", title)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
