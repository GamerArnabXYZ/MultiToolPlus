package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OmniTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatBadge
import com.example.ui.theme.SuccessGreen
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class InstrumentTab(val title: String) {
    COMPASS("Magnetic Compass"),
    LEVEL("2D Spirit Level")
}

@Composable
fun CompassLevelScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedInstrument by remember { mutableStateOf(InstrumentTab.COMPASS) }

    var azimuthDegrees by remember { mutableFloatStateOf(0f) }
    var continuousAzimuth by remember { mutableFloatStateOf(0f) }
    var pitchDegrees by remember { mutableFloatStateOf(0f) }
    var rollDegrees by remember { mutableFloatStateOf(0f) }
    var hasRealSensor by remember { mutableStateOf(false) }
    var simulationMode by remember { mutableStateOf(false) }

    // Register real Android hardware sensors with throttle and continuous angle tracking
    DisposableEffect(simulationMode) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        hasRealSensor = rotationSensor != null || (accelSensor != null && magSensor != null)

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val accelerometerReading = FloatArray(3)
        val magnetometerReading = FloatArray(3)
        var lastUpdateTimestamp = 0L

        fun updateOrientation(rawAz: Float, pitch: Float, roll: Float) {
            if (rawAz.isNaN() || pitch.isNaN() || roll.isNaN() || rawAz.isInfinite()) return

            val now = System.currentTimeMillis()
            if (now - lastUpdateTimestamp < 30) return // Cap at smooth 33 FPS
            lastUpdateTimestamp = now

            val targetAzimuth = (rawAz + 360f) % 360f
            azimuthDegrees = targetAzimuth

            // Shortest path angle tracking so needle never spins 360 degrees across North
            val diff = (targetAzimuth - continuousAzimuth + 540f) % 360f - 180f
            if (!diff.isNaN()) {
                continuousAzimuth += diff
            }

            pitchDegrees = pitch
            rollDegrees = roll
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (simulationMode || event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        try {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val az = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                            val p = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                            val r = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                            updateOrientation(az, p, r)
                        } catch (_: Exception) {}
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                        val success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
                        if (success) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val az = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                            val p = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                            val r = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                            updateOrientation(az, p, r)
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager?.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let { sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magSensor?.let { sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // High performance spring animation with zero lag
    val animatedAzimuth by animateFloatAsState(
        targetValue = continuousAzimuth,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "azimuth_anim"
    )

    val cardinalDirection = when (azimuthDegrees.roundToInt()) {
        in 338..360, in 0..22 -> "North (N)"
        in 23..67 -> "North-East (NE)"
        in 68..112 -> "East (E)"
        in 113..157 -> "South-East (SE)"
        in 158..202 -> "South (S)"
        in 203..247 -> "South-West (SW)"
        in 248..292 -> "West (W)"
        else -> "North-West (NW)"
    }

    val isLevel = abs(pitchDegrees) <= 1.5f && abs(rollDegrees) <= 1.5f

    Scaffold(
        topBar = {
            OmniTopAppBar(
                title = "Compass & Level",
                subtitle = "Hardware magnetic bearing & spirit level",
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instrument Selector Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedInstrument.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    InstrumentTab.values().forEach { tab ->
                        Tab(
                            selected = selectedInstrument == tab,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedInstrument = tab
                            },
                            text = {
                                Text(tab.title, fontWeight = FontWeight.Bold)
                            }
                        )
                    }
                }
            }

            if (selectedInstrument == InstrumentTab.COMPASS) {
                // Heading Telemetry Card
                item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Degree Readout
                        Text(
                            text = "${azimuthDegrees.roundToInt()}°",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = cardinalDirection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Compass Dial Canvas
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val radius = size.minDimension / 2 - 12.dp.toPx()

                                // Outer circle ring
                                drawCircle(
                                    color = Color.LightGray.copy(alpha = 0.4f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Rotating Dial marks
                                rotate(degrees = -animatedAzimuth, pivot = center) {
                                    for (i in 0 until 360 step 15) {
                                        val angleRad = (i * PI / 180f).toFloat()
                                        val isMajor = i % 90 == 0
                                        val isMedium = i % 45 == 0
                                        val tickLength = if (isMajor) 16.dp.toPx() else if (isMedium) 10.dp.toPx() else 6.dp.toPx()
                                        val strokeW = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                                        val tickColor = if (i == 0) Color(0xFFEF4444) else if (isMajor) Color.Gray else Color.LightGray

                                        val startX = center.x + (radius - tickLength) * sin(angleRad)
                                        val startY = center.y - (radius - tickLength) * cos(angleRad)
                                        val endX = center.x + radius * sin(angleRad)
                                        val endY = center.y - radius * cos(angleRad)

                                        drawLine(
                                            color = tickColor,
                                            start = Offset(startX, startY),
                                            end = Offset(endX, endY),
                                            strokeWidth = strokeW
                                        )
                                    }

                                    // North/South Needle
                                    val needlePath = Path().apply {
                                        moveTo(center.x, center.y - radius + 22.dp.toPx())
                                        lineTo(center.x + 8.dp.toPx(), center.y)
                                        lineTo(center.x - 8.dp.toPx(), center.y)
                                        close()
                                    }
                                    drawPath(path = needlePath, color = Color(0xFFEF4444))

                                    val southPath = Path().apply {
                                        moveTo(center.x, center.y + radius - 22.dp.toPx())
                                        lineTo(center.x + 8.dp.toPx(), center.y)
                                        lineTo(center.x - 8.dp.toPx(), center.y)
                                        close()
                                    }
                                    drawPath(path = southPath, color = Color.Gray.copy(alpha = 0.7f))
                                }

                                // Center Pivot Dot
                                drawCircle(color = Color(0xFF0F172A), radius = 6.dp.toPx(), center = center)
                                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        StatBadge(
                            text = if (hasRealSensor) "HARDWARE COMPASS ACTIVE" else "SIMULATION MODE",
                            containerColor = if (hasRealSensor) SuccessGreen.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                            contentColor = if (hasRealSensor) SuccessGreen else Color(0xFFF59E0B)
                        )
                    }
                }
            }
        } else {
            // Spirit Level Bubble Card
                item {
                    val levelBorderColor by animateColorAsState(
                        targetValue = if (isLevel) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        label = "level_border"
                    )

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(if (isLevel) 2.dp else 1.dp, levelBorderColor, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isLevel) "SURFACE IS LEVEL" else "UNLEVEL SURFACE",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (isLevel) SuccessGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Center the green bubble in crosshairs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Level Target Canvas
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    val maxOffsetPx = size.minDimension / 2 - 20.dp.toPx()

                                    // Target concentric circles
                                    drawCircle(color = Color.LightGray.copy(alpha = 0.4f), radius = maxOffsetPx, style = Stroke(1.5.dp.toPx()))
                                    drawCircle(color = if (isLevel) SuccessGreen else Color.LightGray, radius = 24.dp.toPx(), style = Stroke(2.dp.toPx()))

                                    // Crosshairs
                                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(center.x, 10f), end = Offset(center.x, size.height - 10f), strokeWidth = 1.dp.toPx())
                                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(10f, center.y), end = Offset(size.width - 10f, center.y), strokeWidth = 1.dp.toPx())

                                    // Moving Bubble calculation
                                    val bubbleOffsetX = (rollDegrees / 45f).coerceIn(-1f, 1f) * (maxOffsetPx - 16.dp.toPx())
                                    val bubbleOffsetY = (pitchDegrees / 45f).coerceIn(-1f, 1f) * (maxOffsetPx - 16.dp.toPx())

                                    val bubbleColor = if (isLevel) SuccessGreen else Color(0xFF06B6D4)
                                    drawCircle(
                                        color = bubbleColor.copy(alpha = 0.75f),
                                        radius = 16.dp.toPx(),
                                        center = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.6f),
                                        radius = 5.dp.toPx(),
                                        center = Offset(center.x + bubbleOffsetX - 3.dp.toPx(), center.y + bubbleOffsetY - 3.dp.toPx())
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Pitch: ${pitchDegrees.roundToInt()}°",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Roll: ${rollDegrees.roundToInt()}°",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive simulation toggle for testing or emulators
            item {
                SectionHeader(title = "Sensor Controls")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (hasRealSensor) "Hardware Sensors Active" else "No Hardware Compass Found",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            FilterChip(
                                selected = simulationMode,
                                onClick = { simulationMode = !simulationMode },
                                label = { Text(if (simulationMode) "Simulation ON" else "Interactive Dial") }
                            )
                        }

                        AnimatedVisibility(visible = simulationMode || !hasRealSensor) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text("Manual Angle Rotate (${azimuthDegrees.toInt()}°)", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = azimuthDegrees,
                                    onValueChange = { azimuthDegrees = it },
                                    valueRange = 0f..359f
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0f, 90f, 180f, 270f).forEach { angle ->
                                        FilterChip(
                                            selected = (azimuthDegrees - angle) in -5f..5f,
                                            onClick = { azimuthDegrees = angle },
                                            label = { Text(when (angle) { 0f -> "N"; 90f -> "E"; 180f -> "S"; else -> "W" }) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
