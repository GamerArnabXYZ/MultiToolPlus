package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val title: String) {
    ALL("All Tools"),
    DAILY("Daily Essentials"),
    DOCUMENTS("PDF & Docs"),
    MEDIA("Images & Photos"),
    UTILITIES("Sensors & Security")
}

data class ToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val badge: String? = null,
    val accentColor: Color = Color(0xFF2563EB),
    val isFeatured: Boolean = false
)

object ToolsRegistry {
    val tools = listOf(
        // Daily Essentials
        ToolItem(
            id = "qr_scanner",
            title = "QR Code Scanner",
            subtitle = "Live camera scanner & decode QR/barcodes from photos",
            category = ToolCategory.DAILY,
            icon = Icons.Rounded.QrCodeScanner,
            badge = "Scanner",
            accentColor = Color(0xFF0284C7),
            isFeatured = true
        ),
        ToolItem(
            id = "qr_studio",
            title = "QR Generator",
            subtitle = "Generate custom Wi-Fi, URL, and Text QR codes",
            category = ToolCategory.DAILY,
            icon = Icons.Rounded.QrCode2,
            badge = "Offline",
            accentColor = Color(0xFF7C3AED),
            isFeatured = true
        ),
        ToolItem(
            id = "unit_converter",
            title = "Unit Converter",
            subtitle = "Fast conversions for Length, Weight, Temp & Speed",
            category = ToolCategory.DAILY,
            icon = Icons.Rounded.Transform,
            badge = "Daily",
            accentColor = Color(0xFFD97706),
            isFeatured = true
        ),
        ToolItem(
            id = "stopwatch_timer",
            title = "Stopwatch & Timer",
            subtitle = "Precision millisecond laps and countdown intervals",
            category = ToolCategory.DAILY,
            icon = Icons.Rounded.Timer,
            badge = "Timer",
            accentColor = Color(0xFF059669),
            isFeatured = true
        ),
        ToolItem(
            id = "flashlight_tool",
            title = "Flashlight & Torch",
            subtitle = "Camera LED torch, SOS flasher & soft screen glow",
            category = ToolCategory.DAILY,
            icon = Icons.Rounded.FlashlightOn,
            accentColor = Color(0xFFEAB308),
            isFeatured = false
        ),
        ToolItem(
            id = "bill_splitter",
            title = "Bill & Tip Splitter",
            subtitle = "Split group restaurant tabs, tips & calculate shares",
            category = ToolCategory.DAILY,
            icon = Icons.Rounded.ReceiptLong,
            accentColor = Color(0xFF8B5CF6),
            isFeatured = false
        ),

        // Documents & PDF
        ToolItem(
            id = "pdf_to_image",
            title = "PDF to Image",
            subtitle = "Render and extract high-res pages as JPEG or PNG",
            category = ToolCategory.DOCUMENTS,
            icon = Icons.Rounded.PictureAsPdf,
            badge = "Popular",
            accentColor = Color(0xFFEF4444),
            isFeatured = true
        ),
        ToolItem(
            id = "image_to_pdf",
            title = "Image to PDF",
            subtitle = "Combine multiple pictures into an organized PDF",
            category = ToolCategory.DOCUMENTS,
            icon = Icons.Rounded.AutoFixHigh,
            badge = "Multi",
            accentColor = Color(0xFF2563EB),
            isFeatured = false
        ),

        // Media & Photos
        ToolItem(
            id = "image_compressor",
            title = "Image Compressor",
            subtitle = "Shrink file size up to 90% with live quality controls",
            category = ToolCategory.MEDIA,
            icon = Icons.Rounded.Compress,
            badge = "Fast",
            accentColor = Color(0xFF0D9488),
            isFeatured = true
        ),
        ToolItem(
            id = "color_extractor",
            title = "Color Extractor",
            subtitle = "Extract dominant palettes and HEX codes from photos",
            category = ToolCategory.MEDIA,
            icon = Icons.Rounded.FormatColorFill,
            accentColor = Color(0xFFF59E0B),
            isFeatured = false
        ),

        // Utilities & Sensors
        ToolItem(
            id = "compass_level",
            title = "Compass & Level",
            subtitle = "Precision digital compass with 360° bubble level",
            category = ToolCategory.UTILITIES,
            icon = Icons.Rounded.Explore,
            badge = "Sensors",
            accentColor = Color(0xFF06B6D4),
            isFeatured = true
        ),
        ToolItem(
            id = "password_vault",
            title = "Password Gen",
            subtitle = "Cryptographically secure passwords & PINs",
            category = ToolCategory.UTILITIES,
            icon = Icons.Rounded.Password,
            badge = "Secure",
            accentColor = Color(0xFF10B981),
            isFeatured = true
        ),
        ToolItem(
            id = "text_tools",
            title = "Text Utilities",
            subtitle = "Word stats, Case changer, Base64 & Hash generators",
            category = ToolCategory.UTILITIES,
            icon = Icons.Rounded.TextFields,
            accentColor = Color(0xFF6366F1),
            isFeatured = false
        )
    )

    fun findTool(id: String): ToolItem? = tools.find { it.id == id }
    val featuredTools: List<ToolItem> = tools.filter { it.isFeatured }
}
