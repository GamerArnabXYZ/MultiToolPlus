package com.example.ui.screens

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class FlashlightMode {
    STEADY,
    STROBE,
    SOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: LED Flashlight, 1: Screen Glow

    // CameraManager Hardware control
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    val cameraId = remember {
        try {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(FlashlightMode.STEADY) }
    var strobeFrequencyHz by remember { mutableFloatStateOf(4f) }

    fun setHardwareTorch(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraId != null) {
            try {
                cameraManager?.setTorchMode(cameraId, on)
            } catch (_: Exception) {}
        }
    }

    // Handle Strobe and SOS blinking loops
    LaunchedEffect(isTorchOn, currentMode, strobeFrequencyHz) {
        if (isTorchOn) {
            when (currentMode) {
                FlashlightMode.STEADY -> {
                    setHardwareTorch(true)
                }
                FlashlightMode.STROBE -> {
                    val delayMs = (1000f / (strobeFrequencyHz * 2)).toLong().coerceAtLeast(30L)
                    while (isTorchOn && currentMode == FlashlightMode.STROBE) {
                        setHardwareTorch(true)
                        delay(delayMs)
                        setHardwareTorch(false)
                        delay(delayMs)
                    }
                }
                FlashlightMode.SOS -> {
                    // SOS in Morse: ... --- ...
                    val dot = 150L
                    val dash = 450L
                    val intraChar = 150L
                    val interChar = 450L
                    val interWord = 1200L

                    while (isTorchOn && currentMode == FlashlightMode.SOS) {
                        // S (...)
                        repeat(3) {
                            setHardwareTorch(true); delay(dot)
                            setHardwareTorch(false); delay(intraChar)
                        }
                        delay(interChar)

                        // O (---)
                        repeat(3) {
                            setHardwareTorch(true); delay(dash)
                            setHardwareTorch(false); delay(intraChar)
                        }
                        delay(interChar)

                        // S (...)
                        repeat(3) {
                            setHardwareTorch(true); delay(dot)
                            setHardwareTorch(false); delay(intraChar)
                        }
                        delay(interWord)
                    }
                }
            }
        } else {
            setHardwareTorch(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            setHardwareTorch(false)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("flashlight_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Flashlight & Torch", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Camera LED, Strobe, SOS & Screen Light", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Camera Torch") },
                    icon = { Icon(imageVector = Icons.Rounded.FlashlightOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Screen Light") },
                    icon = { Icon(imageVector = Icons.Rounded.LightMode, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                // LED Torch Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mode Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        FilterChip(
                            selected = currentMode == FlashlightMode.STEADY,
                            onClick = { currentMode = FlashlightMode.STEADY },
                            label = { Text("Steady Torch") }
                        )
                        FilterChip(
                            selected = currentMode == FlashlightMode.STROBE,
                            onClick = { currentMode = FlashlightMode.STROBE },
                            label = { Text("Strobe") }
                        )
                        FilterChip(
                            selected = currentMode == FlashlightMode.SOS,
                            onClick = { currentMode = FlashlightMode.SOS },
                            label = { Text("SOS Beacon") }
                        )
                    }

                    // Massive Tactile Power Toggle
                    val animatedGlowColor by animateColorAsState(
                        targetValue = if (isTorchOn) Color(0xFFFBBF24) else MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(300),
                        label = "torch_glow"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(animatedGlowColor.copy(alpha = if (isTorchOn) 0.25f else 0.4f))
                            .clickable {
                                isTorchOn = !isTorchOn
                                if (cameraId == null) {
                                    Toast.makeText(context, "Hardware flash not detected. Screen light mode available!", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(animatedGlowColor)
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                contentDescription = if (isTorchOn) "Turn Off" else "Turn On",
                                tint = if (isTorchOn) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    // Mode Details / Slider if Strobe
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentMode == FlashlightMode.STROBE) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Strobe Speed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${strobeFrequencyHz.toInt()} Hz", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = strobeFrequencyHz,
                                        onValueChange = { strobeFrequencyHz = it },
                                        valueRange = 1f..10f,
                                        steps = 8
                                    )
                                }
                            }
                        } else if (currentMode == FlashlightMode.SOS) {
                            Text(
                                text = "Emergency Morse Signal (... --- ...)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = if (isTorchOn) "Torch is ON" else "Tap circle to turn ON",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isTorchOn) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Screen Light Mode (Soft Ambient Light with Color & Brightness)
                ScreenLightContent()
            }
        }
    }
}

@Composable
fun ScreenLightContent() {
    val lightColors = listOf(
        Pair("Pure White", Color.White),
        Pair("Warm Gold", Color(0xFFFEF3C7)),
        Pair("Candle Amber", Color(0xFFFDE68A)),
        Pair("Soft Cyan", Color(0xFFCFFAFE)),
        Pair("Night Red", Color(0xFFFCA5A5)),
        Pair("Neon Green", Color(0xFFBBF7D0))
    )

    var selectedColor by remember { mutableStateOf(lightColors[0].second) }
    var brightnessFactor by remember { mutableFloatStateOf(1f) }
    var isFullscreenActive by remember { mutableStateOf(false) }

    if (isFullscreenActive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(selectedColor.copy(alpha = brightnessFactor))
                .clickable { isFullscreenActive = false }
        ) {
            Text(
                text = "Tap anywhere to exit",
                color = Color.Black.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Glow Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(selectedColor.copy(alpha = brightnessFactor))
                    .clickable { isFullscreenActive = true },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Tap for Fullscreen Glow",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Brightness Slider
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Screen Glow Intensity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("${(brightnessFactor * 100).toInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = brightnessFactor,
                        onValueChange = { brightnessFactor = it },
                        valueRange = 0.2f..1f
                    )
                }
            }

            // Color Swatches
            Text("Select Glow Tone", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lightColors) { (name, col) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedColor = col }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(col)
                                .then(
                                    if (selectedColor == col) Modifier.clip(CircleShape) else Modifier
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
