package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

enum class UnitType(val displayName: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature"),
    STORAGE("Digital Storage"),
    SPEED("Speed"),
    AREA("Area")
}

data class ConversionUnit(
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

object UnitRegistry {
    val lengthUnits = listOf(
        ConversionUnit("Meter", "m", { it }, { it }),
        ConversionUnit("Kilometer", "km", { it * 1000.0 }, { it / 1000.0 }),
        ConversionUnit("Centimeter", "cm", { it / 100.0 }, { it * 100.0 }),
        ConversionUnit("Millimeter", "mm", { it / 1000.0 }, { it * 1000.0 }),
        ConversionUnit("Mile", "mi", { it * 1609.344 }, { it / 1609.344 }),
        ConversionUnit("Yard", "yd", { it * 0.9144 }, { it / 0.9144 }),
        ConversionUnit("Foot", "ft", { it * 0.3048 }, { it / 0.3048 }),
        ConversionUnit("Inch", "in", { it * 0.0254 }, { it / 0.0254 })
    )

    val weightUnits = listOf(
        ConversionUnit("Kilogram", "kg", { it }, { it }),
        ConversionUnit("Gram", "g", { it / 1000.0 }, { it * 1000.0 }),
        ConversionUnit("Milligram", "mg", { it / 1_000_000.0 }, { it * 1_000_000.0 }),
        ConversionUnit("Pound", "lb", { it * 0.45359237 }, { it / 0.45359237 }),
        ConversionUnit("Ounce", "oz", { it * 0.02834952 }, { it / 0.02834952 }),
        ConversionUnit("Metric Ton", "t", { it * 1000.0 }, { it / 1000.0 })
    )

    val tempUnits = listOf(
        ConversionUnit("Celsius", "°C", { it }, { it }),
        ConversionUnit("Fahrenheit", "°F", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
        ConversionUnit("Kelvin", "K", { it - 273.15 }, { it + 273.15 })
    )

    val storageUnits = listOf(
        ConversionUnit("Megabyte", "MB", { it }, { it }),
        ConversionUnit("Byte", "B", { it / 1_048_576.0 }, { it * 1_048_576.0 }),
        ConversionUnit("Kilobyte", "KB", { it / 1024.0 }, { it * 1024.0 }),
        ConversionUnit("Gigabyte", "GB", { it * 1024.0 }, { it / 1024.0 }),
        ConversionUnit("Terabyte", "TB", { it * 1_048_576.0 }, { it / 1_048_576.0 })
    )

    val speedUnits = listOf(
        ConversionUnit("km/h", "km/h", { it }, { it }),
        ConversionUnit("mph", "mph", { it * 1.60934 }, { it / 1.60934 }),
        ConversionUnit("m/s", "m/s", { it * 3.6 }, { it / 3.6 }),
        ConversionUnit("Knot", "kn", { it * 1.852 }, { it / 1.852 })
    )

    val areaUnits = listOf(
        ConversionUnit("Square Meter", "m²", { it }, { it }),
        ConversionUnit("Square Foot", "ft²", { it * 0.092903 }, { it / 0.092903 }),
        ConversionUnit("Square Km", "km²", { it * 1_000_000.0 }, { it / 1_000_000.0 }),
        ConversionUnit("Acre", "ac", { it * 4046.86 }, { it / 4046.86 }),
        ConversionUnit("Hectare", "ha", { it * 10000.0 }, { it / 10000.0 })
    )

    fun getUnitsForType(type: UnitType): List<ConversionUnit> {
        return when (type) {
            UnitType.LENGTH -> lengthUnits
            UnitType.WEIGHT -> weightUnits
            UnitType.TEMPERATURE -> tempUnits
            UnitType.STORAGE -> storageUnits
            UnitType.SPEED -> speedUnits
            UnitType.AREA -> areaUnits
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedType by remember { mutableStateOf(UnitType.LENGTH) }
    val currentUnits = remember(selectedType) { UnitRegistry.getUnitsForType(selectedType) }

    var fromUnit by remember(selectedType) { mutableStateOf(currentUnits[0]) }
    var toUnit by remember(selectedType) { mutableStateOf(currentUnits.getOrElse(1) { currentUnits[0] }) }

    var inputValue by remember { mutableStateOf("1") }

    val doubleInput = inputValue.toDoubleOrNull() ?: 0.0
    val baseValue = fromUnit.toBase(doubleInput)
    val convertedValue = toUnit.fromBase(baseValue)

    val formatter = remember { DecimalFormat("#,##0.######") }
    val formattedResult = formatter.format(convertedValue)

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("unit_converter_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Unit Converter", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Fast offline measurement conversions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(UnitType.values()) { type ->
                        FilterChip(
                            selected = type == selectedType,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Input & Unit Selection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Input field
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                    inputValue = it
                                }
                            },
                            label = { Text("Enter Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // From Unit Selector
                        UnitSelectorRow(
                            label = "From",
                            selectedUnit = fromUnit,
                            availableUnits = currentUnits,
                            onUnitSelected = { fromUnit = it }
                        )

                        // Swap Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val temp = fromUnit
                                    fromUnit = toUnit
                                    toUnit = temp
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = "Swap Units",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // To Unit Selector
                        UnitSelectorRow(
                            label = "To",
                            selectedUnit = toUnit,
                            availableUnits = currentUnits,
                            onUnitSelected = { toUnit = it }
                        )
                    }
                }
            }

            // Converted Result Big Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${inputValue.ifEmpty { "0" }} ${fromUnit.symbol} =",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$formattedResult ${toUnit.symbol}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("$formattedResult ${toUnit.symbol}"))
                                    Toast.makeText(context, "Copied $formattedResult ${toUnit.symbol}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy Result",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // All Conversions Quick Matrix
            item {
                Text(
                    text = "All ${selectedType.displayName} Equivalents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(currentUnits.filter { it != fromUnit }) { unit ->
                val otherValue = unit.fromBase(baseValue)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(unit.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(unit.symbol, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = formatter.format(otherValue),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnitSelectorRow(
    label: String,
    selectedUnit: ConversionUnit,
    availableUnits: List<ConversionUnit>,
    onUnitSelected: (ConversionUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            Card(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedUnit.name} (${selectedUnit.symbol})",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableUnits.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text("${unit.name} (${unit.symbol})") },
                        onClick = {
                            onUnitSelected(unit)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
