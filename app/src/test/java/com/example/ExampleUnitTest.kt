package com.example

import com.example.model.ToolsRegistry
import com.example.ui.screens.UnitRegistry
import com.example.ui.screens.UnitType
import com.example.utils.QrScannerUtils
import com.example.utils.ScanResultType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testToolsRegistryContainsAllTools() {
    assertNotNull(ToolsRegistry.findTool("qr_studio"))
    assertEquals("QR Generator", ToolsRegistry.findTool("qr_studio")?.title)
    assertNotNull(ToolsRegistry.findTool("qr_scanner"))
    assertNotNull(ToolsRegistry.findTool("unit_converter"))
    assertNotNull(ToolsRegistry.findTool("stopwatch_timer"))
    assertNotNull(ToolsRegistry.findTool("flashlight_tool"))
    assertNotNull(ToolsRegistry.findTool("bill_splitter"))
    assertTrue(ToolsRegistry.tools.size >= 13)
  }

  @Test
  fun testQrScannerParsing() {
    val urlResult = QrScannerUtils.parseScanResult("https://example.com")
    assertEquals(ScanResultType.URL, urlResult.type)
    assertEquals("https://example.com", urlResult.rawText)

    val wifiResult = QrScannerUtils.parseScanResult("WIFI:S:MyNetwork;T:WPA;P:Secret123;;")
    assertEquals(ScanResultType.WIFI, wifiResult.type)
    assertEquals("MyNetwork", wifiResult.extraDetails["SSID"])
    assertEquals("Secret123", wifiResult.extraDetails["Password"])

    val phoneResult = QrScannerUtils.parseScanResult("tel:+1234567890")
    assertEquals(ScanResultType.PHONE, phoneResult.type)
    assertEquals("+1234567890", phoneResult.rawText)

    val emailResult = QrScannerUtils.parseScanResult("mailto:test@example.com")
    assertEquals(ScanResultType.EMAIL, emailResult.type)
    assertEquals("test@example.com", emailResult.rawText)
  }

  @Test
  fun testUnitConversions() {
    val lengthUnits = UnitRegistry.getUnitsForType(UnitType.LENGTH)
    val meter = lengthUnits.first { it.symbol == "m" }
    val km = lengthUnits.first { it.symbol == "km" }

    // 1 km to meters
    val baseKm = km.toBase(1.0)
    assertEquals(1000.0, baseKm, 0.001)
    val kmInMeter = meter.fromBase(baseKm)
    assertEquals(1000.0, kmInMeter, 0.001)

    // 100 Celsius to Fahrenheit
    val tempUnits = UnitRegistry.getUnitsForType(UnitType.TEMPERATURE)
    val celsius = tempUnits.first { it.symbol == "°C" }
    val fahrenheit = tempUnits.first { it.symbol == "°F" }
    val baseC = celsius.toBase(100.0)
    val fVal = fahrenheit.fromBase(baseC)
    assertEquals(212.0, fVal, 0.01)
  }
}

