package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.components.FilePickerCard
import com.example.ui.components.OmniTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatBadge
import com.example.utils.ExtractedPage
import com.example.utils.FileUtils
import com.example.utils.PdfPageInfo
import com.example.utils.PdfUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfToImageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf<String?>(null) }
    var pdfFileSize by remember { mutableStateOf<String?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    val previewPages = remember { mutableStateListOf<PdfPageInfo>() }

    var isInspecting by remember { mutableStateOf(false) }
    var isExtracting by remember { mutableStateOf(false) }
    var extractProgress by remember { mutableFloatStateOf(0f) }
    var progressStatusText by remember { mutableStateOf("") }

    // Settings
    var selectedScale by remember { mutableFloatStateOf(2.0f) }
    var selectedFormat by remember { mutableStateOf(Bitmap.CompressFormat.JPEG) }
    var extractAllPages by remember { mutableStateOf(true) }
    val selectedPageIndices = remember { mutableStateListOf<Int>() }

    // Extracted results
    val extractedResults = remember { mutableStateListOf<ExtractedPage>() }
    var fullScreenPreviewBitmap by remember { mutableStateOf<ExtractedPage?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            pdfFileName = FileUtils.getFileName(context, uri)
            pdfFileSize = FileUtils.formatBytes(FileUtils.getFileSize(context, uri))
            extractedResults.clear()
            selectedPageIndices.clear()
            previewPages.clear()

            isInspecting = true
            scope.launch {
                try {
                    val (count, pages) = PdfUtils.inspectPdf(context, uri)
                    totalPages = count
                    previewPages.addAll(pages)
                    // By default select all pages
                    for (i in 0 until count) {
                        selectedPageIndices.add(i)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error reading PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isInspecting = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            OmniTopAppBar(
                title = "PDF to Image",
                subtitle = "Extract pages with lossless quality",
                onBackClick = onBack
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File Picker Card
            item {
                FilePickerCard(
                    title = "Select PDF Document",
                    subtitle = "Tap to choose a .pdf file from your device",
                    icon = Icons.Rounded.PictureAsPdf,
                    accentColor = Color(0xFFEF4444),
                    selectedFileName = pdfFileName,
                    selectedFileSize = if (totalPages > 0) "$pdfFileSize • $totalPages pages" else pdfFileSize,
                    onClick = { pdfPickerLauncher.launch("application/pdf") }
                )
            }

            if (isInspecting) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Inspecting PDF pages...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // PDF Loaded Controls
            if (selectedPdfUri != null && totalPages > 0) {
                // Resolution Selector
                item {
                    SectionHeader(title = "Render Resolution", badgeText = "${selectedScale.toInt()}x Scale")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scales = listOf(
                            Pair("Standard (1x)", 1.0f),
                            Pair("Sharp (2x)", 2.0f),
                            Pair("Ultra (3x)", 3.0f)
                        )
                        scales.forEach { (label, scale) ->
                            FilterChip(
                                selected = selectedScale == scale,
                                onClick = { selectedScale = scale },
                                label = { Text(label) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Format Selector
                item {
                    SectionHeader(title = "Image Format")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val formats = listOf(
                            Pair("JPEG (Smaller)", Bitmap.CompressFormat.JPEG),
                            Pair("PNG (Lossless)", Bitmap.CompressFormat.PNG)
                        )
                        formats.forEach { (label, fmt) ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(label) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Page Selection mode
                item {
                    SectionHeader(
                        title = "Pages to Extract",
                        badgeText = if (extractAllPages) "All $totalPages" else "${selectedPageIndices.size} selected"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = extractAllPages,
                            onClick = {
                                extractAllPages = true
                                selectedPageIndices.clear()
                                for (i in 0 until totalPages) selectedPageIndices.add(i)
                            },
                            label = { Text("All Pages ($totalPages)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !extractAllPages,
                            onClick = { extractAllPages = false },
                            label = { Text("Select Pages") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Custom page selection chips
                if (!extractAllPages && totalPages > 0) {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (p in 0 until totalPages) {
                                val isSelected = selectedPageIndices.contains(p)
                                Surface(
                                    onClick = {
                                        if (isSelected) {
                                            if (selectedPageIndices.size > 1) selectedPageIndices.remove(p)
                                        } else {
                                            selectedPageIndices.add(p)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(width = 44.dp, height = 36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${p + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Extract Button & Progress
                item {
                    if (isExtracting) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { extractProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = progressStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val pagesToProcess = if (extractAllPages) {
                                    (0 until totalPages).toList()
                                } else {
                                    selectedPageIndices.sorted()
                                }

                                if (pagesToProcess.isEmpty()) {
                                    Toast.makeText(context, "Select at least 1 page", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isExtracting = true
                                extractProgress = 0f
                                extractedResults.clear()

                                scope.launch {
                                    try {
                                        val results = PdfUtils.renderPagesToImages(
                                            context = context,
                                            pdfUri = selectedPdfUri!!,
                                            pageIndices = pagesToProcess,
                                            scale = selectedScale,
                                            format = selectedFormat,
                                            quality = 92
                                        ) { current, total ->
                                            extractProgress = current.toFloat() / total
                                            progressStatusText = "Extracting page $current of $total..."
                                        }
                                        extractedResults.addAll(results)
                                        Toast.makeText(context, "Extracted ${results.size} pages successfully!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isExtracting = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("extract_pages_button")
                        ) {
                            Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Extract Images (${if (extractAllPages) totalPages else selectedPageIndices.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Results Gallery
            if (extractedResults.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Extracted Images",
                        badgeText = "${extractedResults.size} Pages Ready"
                    )
                }

                // Global Save / Share Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                var savedCount = 0
                                scope.launch {
                                    val mime = if (selectedFormat == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
                                    extractedResults.forEach { page ->
                                        val savedUri = FileUtils.saveImageFileToGallery(
                                            context = context,
                                            imageFile = page.file,
                                            mimeType = mime,
                                            filename = "Omni_${pdfFileName?.substringBeforeLast('.')}_page_${page.pageIndex + 1}"
                                        )
                                        if (savedUri != null) savedCount++
                                    }
                                    Toast.makeText(context, "Saved $savedCount images to Pictures/OmniTool!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save All")
                        }

                        OutlinedButton(
                            onClick = {
                                if (extractedResults.isNotEmpty()) {
                                    val firstFile = extractedResults.first().file
                                    val mime = if (selectedFormat == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
                                    FileUtils.shareFile(context, firstFile, mime, "Share Extracted Image")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Page")
                        }
                    }
                }

                // Grid of Extracted Images
                items(extractedResults) { page ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .clickable { fullScreenPreviewBitmap = page }
                            ) {
                                AsyncImage(
                                    model = page.file,
                                    contentDescription = "Page ${page.pageIndex + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ZoomIn,
                                        contentDescription = "Preview",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Page ${page.pageIndex + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${page.width} × ${page.height} px • ${FileUtils.formatBytes(page.fileSize)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Actions
                            IconButton(
                                onClick = {
                                    val mime = if (selectedFormat == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
                                    val uri = FileUtils.saveImageFileToGallery(
                                        context = context,
                                        imageFile = page.file,
                                        mimeType = mime,
                                        filename = "Omni_page_${page.pageIndex + 1}"
                                    )
                                    if (uri != null) {
                                        Toast.makeText(context, "Saved page ${page.pageIndex + 1} to Gallery!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Rounded.Download, contentDescription = "Save")
                            }

                            IconButton(
                                onClick = {
                                    val mime = if (selectedFormat == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
                                    FileUtils.shareFile(context, page.file, mime, "Share Page ${page.pageIndex + 1}")
                                }
                            ) {
                                Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share")
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Preview Dialog
    fullScreenPreviewBitmap?.let { page ->
        Dialog(onDismissRequest = { fullScreenPreviewBitmap = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${page.pageIndex + 1} Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { fullScreenPreviewBitmap = null }) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = page.file,
                            contentDescription = "Full preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val bmp = android.graphics.BitmapFactory.decodeFile(page.file.absolutePath)
                                if (bmp != null) {
                                    FileUtils.saveBitmapToGallery(context, bmp, "Omni_page_${page.pageIndex + 1}")
                                    Toast.makeText(context, "Saved to Pictures/OmniTool", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Image")
                        }
                    }
                }
            }
        }
    }
}
