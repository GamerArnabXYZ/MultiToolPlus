package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.FilePickerCard
import com.example.ui.components.OmniTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatBadge
import com.example.ui.theme.SuccessGreen
import com.example.utils.CompressionResult
import com.example.utils.FileUtils
import com.example.utils.ImageCompressorUtils
import com.example.utils.ImageOutputFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ImageCompressorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalFileName by remember { mutableStateOf<String?>(null) }
    var originalFileSize by remember { mutableStateOf<String?>(null) }

    // Compressor parameters
    var quality by remember { mutableFloatStateOf(75f) }
    var scalePercent by remember { mutableIntStateOf(100) }
    var selectedFormat by remember { mutableStateOf(ImageOutputFormat.JPEG) }

    // State
    var isCompressing by remember { mutableStateOf(false) }
    var compressionResult by remember { mutableStateOf<CompressionResult?>(null) }
    var compressJob by remember { mutableStateOf<Job?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            originalFileName = FileUtils.getFileName(context, uri)
            originalFileSize = FileUtils.formatBytes(FileUtils.getFileSize(context, uri))
        }
    }

    // Trigger compression whenever settings or image change
    LaunchedEffect(selectedImageUri, quality, scalePercent, selectedFormat) {
        val uri = selectedImageUri ?: return@LaunchedEffect
        compressJob?.cancel()
        compressJob = scope.launch {
            // Small debounce for slider dragging
            delay(150)
            isCompressing = true
            try {
                val res = ImageCompressorUtils.compressImage(
                    context = context,
                    imageUri = uri,
                    quality = quality.toInt(),
                    scalePercent = scalePercent,
                    outputFormat = selectedFormat
                )
                compressionResult = res
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isCompressing = false
            }
        }
    }

    Scaffold(
        topBar = {
            OmniTopAppBar(
                title = "Image Compressor",
                subtitle = "Lossless & lossy image reduction",
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
            // Image Picker Card
            item {
                FilePickerCard(
                    title = "Choose Photo to Compress",
                    subtitle = "Select any JPG, PNG, or WEBP photo",
                    icon = Icons.Rounded.Photo,
                    accentColor = Color(0xFF0D9488),
                    selectedFileName = originalFileName,
                    selectedFileSize = originalFileSize,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            if (selectedImageUri != null) {
                // Before & After Stats Card
                item {
                    ComparisonStatsCard(
                        result = compressionResult,
                        originalUri = selectedImageUri!!,
                        isProcessing = isCompressing
                    )
                }

                // Quality Slider
                item {
                    SectionHeader(
                        title = "Compression Quality",
                        badgeText = "${quality.toInt()}%"
                    )
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Maximum Compression", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Highest Quality", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = quality,
                                onValueChange = { quality = it },
                                valueRange = 10f..100f,
                                steps = 17,
                                modifier = Modifier.testTag("quality_slider")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val label = when {
                                    quality >= 85 -> "Near Lossless (Recommended for photos)"
                                    quality >= 60 -> "Balanced (Great size & clarity)"
                                    else -> "Aggressive (Smallest possible file)"
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Resolution Scaling Chips
                item {
                    SectionHeader(title = "Resolution Scaling", badgeText = "$scalePercent%")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 50, 75, 100).forEach { pct ->
                            FilterChip(
                                selected = scalePercent == pct,
                                onClick = { scalePercent = pct },
                                label = { Text(if (pct == 100) "100% (Original)" else "$pct%") },
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

                // Output Format Selection
                item {
                    SectionHeader(title = "Output Format")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImageOutputFormat.values().forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt.displayName) },
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

                // Actions: Save & Share
                item {
                    val result = compressionResult
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (result != null) {
                                    val saved = FileUtils.saveImageFileToGallery(
                                        context = context,
                                        imageFile = result.compressedFile,
                                        mimeType = selectedFormat.mimeType,
                                        filename = "Omni_${originalFileName?.substringBeforeLast('.') ?: "image"}_comp"
                                    )
                                    if (saved != null) {
                                        Toast.makeText(context, "Saved to Pictures/OmniTool!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save to gallery", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = result != null && !isCompressing,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("save_compressed_button")
                        ) {
                            Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Image", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (result != null) {
                                    FileUtils.shareFile(
                                        context = context,
                                        file = result.compressedFile,
                                        mimeType = selectedFormat.mimeType,
                                        title = "Share Compressed Image"
                                    )
                                }
                            },
                            enabled = result != null && !isCompressing,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("share_compressed_button")
                        ) {
                            Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonStatsCard(
    result: CompressionResult?,
    originalUri: Uri,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compression Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                } else if (result != null && result.compressionRatioPercent > 0) {
                    StatBadge(
                        text = "-${result.compressionRatioPercent}% REDUCTION",
                        containerColor = SuccessGreen.copy(alpha = 0.15f),
                        contentColor = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-column comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Original Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Original",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                    ) {
                        AsyncImage(
                            model = originalUri,
                            contentDescription = "Original",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (result != null) FileUtils.formatBytes(result.originalSize) else "...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (result != null) "${result.originalWidth} × ${result.originalHeight}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Compressed Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Compressed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                    ) {
                        if (result != null) {
                            Image(
                                bitmap = result.compressedBitmap.asImageBitmap(),
                                contentDescription = "Compressed",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (result != null) FileUtils.formatBytes(result.newSize) else "Calculating...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (result != null && result.newSize < result.originalSize) SuccessGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (result != null) "${result.newWidth} × ${result.newHeight} (${result.format.displayName})" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
