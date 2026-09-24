package com.example.utils

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.EnumMap

enum class ScanResultType {
    URL,
    WIFI,
    PHONE,
    EMAIL,
    TEXT,
    BARCODE
}

data class ParsedScanResult(
    val rawText: String,
    val type: ScanResultType,
    val format: String,
    val title: String,
    val extraDetails: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

object QrScannerUtils {

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(
                DecodeHintType.POSSIBLE_FORMATS,
                listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.AZTEC,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.ITF
                )
            )
            put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
        }
        setHints(hints)
    }

    /**
     * Decodes a Bitmap into a ZXing Result
     */
    fun decodeBitmap(bitmap: Bitmap): Result? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            reader.decodeWithState(binaryBitmap)
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    /**
     * Decodes CameraX ImageProxy frame (YUV_420_888)
     */
    fun decodeImageProxy(imageProxy: ImageProxy): Result? {
        return try {
            val plane = imageProxy.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val width = imageProxy.width
            val height = imageProxy.height

            val source = PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            reader.decodeWithState(binaryBitmap)
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    /**
     * Parses raw scan string into structured object
     */
    fun parseScanResult(raw: String, format: BarcodeFormat? = null): ParsedScanResult {
        val formatName = format?.name?.replace("_", " ") ?: "QR Code"
        val trimmed = raw.trim()

        return when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("www.", ignoreCase = true) -> {
                val fullUrl = if (trimmed.startsWith("www.", ignoreCase = true)) "https://$trimmed" else trimmed
                ParsedScanResult(
                    rawText = fullUrl,
                    type = ScanResultType.URL,
                    format = formatName,
                    title = "Website Link"
                )
            }
            trimmed.startsWith("WIFI:", ignoreCase = true) -> {
                val details = parseWifiString(trimmed)
                ParsedScanResult(
                    rawText = trimmed,
                    type = ScanResultType.WIFI,
                    format = formatName,
                    title = "Wi-Fi Network (${details["SSID"] ?: "Network"})",
                    extraDetails = details
                )
            }
            trimmed.startsWith("tel:", ignoreCase = true) || (trimmed.startsWith("+") && trimmed.length in 8..16) -> {
                val phone = trimmed.removePrefix("tel:").trim()
                ParsedScanResult(
                    rawText = phone,
                    type = ScanResultType.PHONE,
                    format = formatName,
                    title = "Phone Number ($phone)"
                )
            }
            trimmed.startsWith("mailto:", ignoreCase = true) || (trimmed.contains("@") && trimmed.contains(".") && !trimmed.contains(" ")) -> {
                val email = trimmed.removePrefix("mailto:").substringBefore("?").trim()
                ParsedScanResult(
                    rawText = email,
                    type = ScanResultType.EMAIL,
                    format = formatName,
                    title = "Email Address ($email)"
                )
            }
            format != null && format != BarcodeFormat.QR_CODE -> {
                ParsedScanResult(
                    rawText = trimmed,
                    type = ScanResultType.BARCODE,
                    format = formatName,
                    title = "Product Barcode ($formatName)"
                )
            }
            else -> {
                ParsedScanResult(
                    rawText = trimmed,
                    type = ScanResultType.TEXT,
                    format = formatName,
                    title = "Plain Text"
                )
            }
        }
    }

    private fun parseWifiString(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val content = raw.removePrefix("WIFI:").removePrefix("wifi:").removeSuffix(";;").removeSuffix(";")
        val tokens = content.split(";")
        for (token in tokens) {
            val parts = token.split(":", limit = 2)
            if (parts.size == 2) {
                when (parts[0].uppercase()) {
                    "S" -> map["SSID"] = parts[1]
                    "P" -> map["Password"] = parts[1]
                    "T" -> map["Security"] = parts[1]
                    "H" -> map["Hidden"] = parts[1]
                }
            }
        }
        return map
    }
}
