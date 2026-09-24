package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OmniTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ"
private val UPPERCASE_ALL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private val LOWERCASE = "abcdefghijkmnopqrstuvwxyz"
private val LOWERCASE_ALL = "abcdefghijklmnopqrstuvwxyz"
private val DIGITS = "23456789"
private val DIGITS_ALL = "0123456789"
private val SYMBOLS = "!@#\$%^&*()-_=+[]{}|;:,.<>?"

private val WORDS = listOf(
    "nebula", "cobalt", "quantum", "crystal", "apex", "zenith", "vortex",
    "cascade", "falcon", "phantom", "echo", "frost", "solaris", "titan",
    "breeze", "beacon", "horizon", "shadow", "aurora", "blaze", "orbit",
    "vector", "matrix", "canyon", "velvet", "prism", "glacier", "pulsar"
)

enum class PassMode(val title: String) {
    RANDOM("Password"),
    PIN("PIN Code"),
    PASSPHRASE("Memorable Passphrase")
}

@Composable
fun PasswordGenScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var mode by remember { mutableStateOf(PassMode.RANDOM) }
    var length by remember { mutableFloatStateOf(16f) }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var avoidAmbiguous by remember { mutableStateOf(true) }

    var generatedResult by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }

    fun generate() {
        val random = SecureRandom()
        val result = when (mode) {
            PassMode.PIN -> {
                val len = length.roundToInt().coerceIn(4, 12)
                (1..len).map { random.nextInt(10).toString() }.joinToString("")
            }
            PassMode.PASSPHRASE -> {
                val wordCount = (length / 4).roundToInt().coerceIn(3, 7)
                (1..wordCount).map { WORDS[random.nextInt(WORDS.size)] }
                    .joinToString("-") { it.replaceFirstChar { c -> c.uppercase() } } + (10 + random.nextInt(90))
            }
            PassMode.RANDOM -> {
                val charset = StringBuilder()
                if (useUpper) charset.append(if (avoidAmbiguous) UPPERCASE else UPPERCASE_ALL)
                if (useLower) charset.append(if (avoidAmbiguous) LOWERCASE else LOWERCASE_ALL)
                if (useDigits) charset.append(if (avoidAmbiguous) DIGITS else DIGITS_ALL)
                if (useSymbols) charset.append(SYMBOLS)

                val chars = if (charset.isNotEmpty()) charset.toString() else "abcdefghjkmnpqrstuvwxyz23456789"
                val len = length.roundToInt().coerceIn(4, 64)
                (1..len).map { chars[random.nextInt(chars.length)] }.joinToString("")
            }
        }
        generatedResult = result
        if (result.isNotEmpty() && (history.isEmpty() || history.first() != result)) {
            history.add(0, result)
            if (history.size > 8) history.removeAt(history.lastIndex)
        }
    }

    LaunchedEffect(mode, length, useUpper, useLower, useDigits, useSymbols, avoidAmbiguous) {
        generate()
    }

    // Entropy estimation
    val charsetSize = when (mode) {
        PassMode.PIN -> 10.0
        PassMode.PASSPHRASE -> WORDS.size.toDouble()
        PassMode.RANDOM -> {
            var s = 0.0
            if (useUpper) s += 26.0
            if (useLower) s += 26.0
            if (useDigits) s += 10.0
            if (useSymbols) s += 30.0
            if (s == 0.0) 26.0 else s
        }
    }

    val entropyBits = (length * log2(charsetSize)).coerceIn(10.0, 160.0)
    val strengthPercent = (entropyBits / 100.0).coerceIn(0.1, 1.0).toFloat()
    val strengthLabel = when {
        entropyBits < 36 -> "Very Weak (Crackable in seconds)"
        entropyBits < 56 -> "Moderate (Crackable in days)"
        entropyBits < 80 -> "Strong (Crackable in centuries)"
        else -> "Military Grade (Uncrackable)"
    }
    val strengthColor by animateColorAsState(
        targetValue = when {
            entropyBits < 36 -> DangerRed
            entropyBits < 56 -> WarningAmber
            entropyBits < 80 -> Color(0xFF10B981)
            else -> Color(0xFF06B6D4)
        },
        label = "strength_color"
    )

    fun copyToClipboard(text: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Generated Password", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            OmniTopAppBar(
                title = "Password Gen",
                subtitle = "Cryptographically secure offline generator",
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Password Display Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${generatedResult.length} Characters",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatBadge(
                                text = "OFFLINE SECURE",
                                containerColor = SuccessGreen.copy(alpha = 0.15f),
                                contentColor = SuccessGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Generated String Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { copyToClipboard(generatedResult) }
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = generatedResult.ifEmpty { "Generating..." },
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Strength Meter Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strengthLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = strengthColor
                                )
                                Text(
                                    text = "${entropyBits.roundToInt()} bits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { strengthPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = strengthColor,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    generate()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Regenerate", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = { copyToClipboard(generatedResult) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy")
                            }
                        }
                    }
                }
            }

            // Mode Selector
            item {
                SectionHeader(title = "Type Presets")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PassMode.values().forEach { m ->
                        val isSelected = mode == m
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                mode = m
                                length = when (m) {
                                    PassMode.PIN -> 6f
                                    PassMode.PASSPHRASE -> 16f
                                    PassMode.RANDOM -> 16f
                                }
                            },
                            label = { Text(m.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Customization Options
            item {
                SectionHeader(title = "Length & Rules")
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Length", fontWeight = FontWeight.Bold)
                            Text("${length.roundToInt()}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = length,
                            onValueChange = { length = it },
                            valueRange = if (mode == PassMode.PIN) 4f..12f else 6f..64f,
                            steps = if (mode == PassMode.PIN) 7 else 57
                        )

                        if (mode == PassMode.RANDOM) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SwitchRow("Include Uppercase (A-Z)", useUpper) {
                                val count = (if (it) 1 else 0) + (if (useLower) 1 else 0) + (if (useDigits) 1 else 0) + (if (useSymbols) 1 else 0)
                                if (count > 0) useUpper = it
                            }
                            SwitchRow("Include Lowercase (a-z)", useLower) {
                                val count = (if (useUpper) 1 else 0) + (if (it) 1 else 0) + (if (useDigits) 1 else 0) + (if (useSymbols) 1 else 0)
                                if (count > 0) useLower = it
                            }
                            SwitchRow("Include Numbers (0-9)", useDigits) {
                                val count = (if (useUpper) 1 else 0) + (if (useLower) 1 else 0) + (if (it) 1 else 0) + (if (useSymbols) 1 else 0)
                                if (count > 0) useDigits = it
                            }
                            SwitchRow("Include Symbols (!@#\$%)", useSymbols) {
                                val count = (if (useUpper) 1 else 0) + (if (useLower) 1 else 0) + (if (useDigits) 1 else 0) + (if (it) 1 else 0)
                                if (count > 0) useSymbols = it
                            }
                            SwitchRow("Avoid Ambiguous (1, l, 0, O)", avoidAmbiguous) { avoidAmbiguous = it }
                        }
                    }
                }
            }

            // Session History Card
            if (history.size > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Recent Generated")
                        IconButton(onClick = { history.clear() }) {
                            Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Clear history", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                items(history.drop(1)) { pass ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyToClipboard(pass) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pass,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
