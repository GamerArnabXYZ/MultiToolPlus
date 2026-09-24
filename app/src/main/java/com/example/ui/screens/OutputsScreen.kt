package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.StatBadge
import com.example.utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalOutputFile(
    val file: File,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val isPdf: Boolean
)

@Composable
fun OutputsScreen(
    onOpenTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val outputFiles = remember { mutableStateListOf<LocalOutputFile>() }
    var totalSizeFormatted by remember { mutableStateOf("0 B") }

    fun refreshFiles() {
        val list = mutableListOf<LocalOutputFile>()
        try {
            // Check cache files
            val baseCache = context.cacheDir

            // PDFs directly in cache
            baseCache.listFiles()?.forEach { f ->
                if (f.isFile && f.name.endsWith(".pdf", ignoreCase = true)) {
                    list.add(LocalOutputFile(f, f.name, f.length(), f.lastModified(), "application/pdf", true))
                }
            }

            // Compressed images
            File(baseCache, "compressed_images").listFiles()?.forEach { f ->
                if (f.isFile) {
                    val isPng = f.name.endsWith(".png", true)
                    list.add(LocalOutputFile(f, f.name, f.length(), f.lastModified(), if (isPng) "image/png" else "image/jpeg", false))
                }
            }

            // Shared images / QRs
            File(baseCache, "shared_images").listFiles()?.forEach { f ->
                if (f.isFile) {
                    list.add(LocalOutputFile(f, f.name, f.length(), f.lastModified(), "image/png", false))
                }
            }

            // Extracted PDF folders
            baseCache.listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.name.startsWith("pdf_extracted_")) {
                    dir.listFiles()?.forEach { f ->
                        if (f.isFile) {
                            val isPng = f.name.endsWith(".png", true)
                            list.add(LocalOutputFile(f, "${dir.name.takeLast(6)}_${f.name}", f.length(), f.lastModified(), if (isPng) "image/png" else "image/jpeg", false))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        outputFiles.clear()
        outputFiles.addAll(list.sortedByDescending { it.lastModified })
        val totalBytes = list.sumOf { it.size }
        totalSizeFormatted = FileUtils.formatBytes(totalBytes)
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Native Android Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${outputFiles.size} generated files • $totalSizeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (outputFiles.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            outputFiles.forEach { it.file.delete() }
                            refreshFiles()
                            Toast.makeText(context, "Cleared output cache", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Clear All", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (outputFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No outputs yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Files you compress, convert, or generate will appear here for easy access and sharing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(outputFiles, key = { it.file.absolutePath }) { item ->
                Card(
                    onClick = {
                        FileUtils.openFile(context, item.file, item.mimeType)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thumbnail or Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (item.isPdf) Color(0xFFEF4444).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.isPdf) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureAsPdf,
                                    contentDescription = "PDF Document",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                AsyncImage(
                                    model = item.file,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.lastModified))
                            Text(
                                text = "${FileUtils.formatBytes(item.size)} • $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Share action
                        IconButton(
                            onClick = {
                                FileUtils.shareFile(context, item.file, item.mimeType, "Share ${item.name}")
                            }
                        ) {
                            Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Delete action
                        IconButton(
                            onClick = {
                                item.file.delete()
                                refreshFiles()
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
