package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.OmniTopAppBar
import com.example.ui.components.SectionHeader
import com.example.utils.FileUtils
import com.example.utils.QrCodeGenerator

enum class QrType(val title: String) {
    URL("Link"),
    TEXT("Text"),
    WIFI("Wi-Fi"),
    PHONE("Phone")
}

@Composable
fun QrStudioScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(QrType.URL) }

    // Input values
    var urlInput by remember { mutableStateOf("https://google.com") }
    var textInput by remember { mutableStateOf("Hello from OmniTool!") }
    var wifiSsid by remember { mutableStateOf("MyHomeNetwork") }
    var wifiPassword by remember { mutableStateOf("SecretPassword123") }
    var wifiAuth by remember { mutableStateOf("WPA") }
    var phoneInput by remember { mutableStateOf("+1 555 123 4567") }

    // Styling
    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var roundCorners by remember { mutableStateOf(true) }

    val qrColors = listOf(
        Pair("Midnight", Color(0xFF0F172A)),
        Pair("Blue", Color(0xFF2563EB)),
        Pair("Teal", Color(0xFF0D9488)),
        Pair("Purple", Color(0xFF7C3AED)),
        Pair("Crimson", Color(0xFFDC2626))
    )

    // Helper to escape Wi-Fi special chars
    fun escapeWifi(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(":", "\\:").replace(",", "\\,").replace("\"", "\\\"")

    // Compute raw QR string
    val qrRawString = remember(selectedTab, urlInput, textInput, wifiSsid, wifiPassword, wifiAuth, phoneInput) {
        when (selectedTab) {
            QrType.URL -> if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) urlInput else "https://$urlInput"
            QrType.TEXT -> textInput
            QrType.WIFI -> "WIFI:T:$wifiAuth;S:${escapeWifi(wifiSsid)};P:${escapeWifi(wifiPassword)};;"
            QrType.PHONE -> "tel:${phoneInput.replace(" ", "")}"
        }
    }

    // Generated QR Bitmap
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(qrRawString, selectedColor, roundCorners) {
        qrBitmap = QrCodeGenerator.generateQrBitmap(
            content = qrRawString,
            sizePx = 512,
            foregroundColor = selectedColor,
            backgroundColor = Color.White,
            roundCorners = roundCorners
        )
    }

    Scaffold(
        topBar = {
            OmniTopAppBar(
                title = "QR Generator",
                subtitle = "Generate custom offline QR codes for any purpose",
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
            // Live QR Preview Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            qrBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "QR Code Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = when (selectedTab) {
                                QrType.URL -> urlInput
                                QrType.WIFI -> "Wi-Fi: $wifiSsid"
                                QrType.PHONE -> "Call: $phoneInput"
                                QrType.TEXT -> if (textInput.length > 30) textInput.take(30) + "..." else textInput
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Quick Actions: Save, Share & Copy
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            qrBitmap?.let { bmp ->
                                val uri = FileUtils.saveBitmapToGallery(
                                    context = context,
                                    bitmap = bmp,
                                    filename = "Omni_QR_${System.currentTimeMillis()}",
                                    format = Bitmap.CompressFormat.PNG
                                )
                                if (uri != null) {
                                    Toast.makeText(context, "Saved QR to Pictures/OmniTool!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save PNG", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            qrBitmap?.let { bmp ->
                                FileUtils.shareBitmap(
                                    context = context,
                                    bitmap = bmp,
                                    filename = "Omni_QR_${System.currentTimeMillis()}",
                                    title = "Share QR Code"
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(qrRawString))
                            Toast.makeText(context, "Copied QR payload to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Text", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Type Tabs
            item {
                SectionHeader(title = "QR Content Type")
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    QrType.values().forEach { type ->
                        Tab(
                            selected = selectedTab == type,
                            onClick = { selectedTab = type },
                            text = { Text(type.title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            // Content Fields based on type
            item {
                when (selectedTab) {
                    QrType.URL -> {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Website or Link URL") },
                            placeholder = { Text("https://example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    QrType.TEXT -> {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Text or Message") },
                            placeholder = { Text("Enter plain text or note") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    QrType.WIFI -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("Network Name (SSID)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("Wi-Fi Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("WPA", "WEP", "nopass").forEach { auth ->
                                    FilterChip(
                                        selected = wifiAuth == auth,
                                        onClick = { wifiAuth = auth },
                                        label = { Text(if (auth == "nopass") "Open" else auth) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    QrType.PHONE -> {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+1 555 123 4567") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            // Customization: Colors & Shapes
            item {
                SectionHeader(title = "Color Style")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    qrColors.forEach { (name, col) ->
                        val isSelected = selectedColor == col
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = col }
                        )
                    }
                }
            }

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
                            text = "Soft Rounded Dots",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Smooth curves for a modern look",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = roundCorners,
                        onCheckedChange = { roundCorners = it }
                    )
                }
            }
        }
    }
}
