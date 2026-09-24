package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AllToolsScreen
import com.example.ui.screens.ColorExtractorScreen
import com.example.ui.screens.CompassLevelScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageCompressorScreen
import com.example.ui.screens.ImageToPdfScreen
import com.example.ui.screens.NativeSplashScreen
import com.example.ui.screens.OutputsScreen
import com.example.ui.screens.PasswordGenScreen
import com.example.ui.screens.PdfToImageScreen
import com.example.ui.screens.QrStudioScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.UnitConverterScreen
import com.example.ui.screens.StopwatchTimerScreen
import com.example.ui.screens.FlashlightScreen
import com.example.ui.screens.BillSplitterScreen
import com.example.ui.screens.StorageStatsScreen
import com.example.ui.screens.TextToolsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.ToolHistoryManager

enum class MainTab(val title: String) {
    HOME("Home"),
    ALL_TOOLS("All Tools"),
    OUTPUTS("Files"),
    DEVICE("Storage")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.theme.ThemeManager.init(this)
        com.example.utils.ToolHistoryManager.init(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    NativeSplashScreen(onFinish = { showSplash = false })
                } else {
                    OmniToolApp()
                }
            }
        }
    }
}

@Composable
fun OmniToolApp() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var activeToolScreen by remember { mutableStateOf<String?>(null) }

    val openTool: (String) -> Unit = { toolId ->
        ToolHistoryManager.recordToolLaunch(context, toolId)
        activeToolScreen = toolId
    }

    // Intercept back button native Android behavior
    BackHandler(enabled = activeToolScreen != null || currentTab != MainTab.HOME) {
        if (activeToolScreen != null) {
            activeToolScreen = null
        } else if (currentTab != MainTab.HOME) {
            currentTab = MainTab.HOME
        }
    }

    if (activeToolScreen != null) {
        // Render Active Tool fullscreen with its own native Scaffold
        when (activeToolScreen) {
            "pdf_to_image" -> PdfToImageScreen(
                onBack = { activeToolScreen = null }
            )
            "image_compressor" -> ImageCompressorScreen(
                onBack = { activeToolScreen = null }
            )
            "image_to_pdf" -> ImageToPdfScreen(
                onBack = { activeToolScreen = null }
            )
            "qr_studio" -> QrStudioScreen(
                onBack = { activeToolScreen = null }
            )
            "qr_scanner" -> QrScannerScreen(
                onBack = { activeToolScreen = null }
            )
            "unit_converter" -> UnitConverterScreen(
                onBack = { activeToolScreen = null }
            )
            "stopwatch_timer" -> StopwatchTimerScreen(
                onBack = { activeToolScreen = null }
            )
            "flashlight_tool" -> FlashlightScreen(
                onBack = { activeToolScreen = null }
            )
            "bill_splitter" -> BillSplitterScreen(
                onBack = { activeToolScreen = null }
            )
            "compass_level" -> CompassLevelScreen(
                onBack = { activeToolScreen = null }
            )
            "password_vault" -> PasswordGenScreen(
                onBack = { activeToolScreen = null }
            )
            "color_extractor" -> ColorExtractorScreen(
                onBack = { activeToolScreen = null }
            )
            "text_tools" -> TextToolsScreen(
                onBack = { activeToolScreen = null }
            )
            else -> {
                activeToolScreen = null
            }
        }
    } else {
        // Main Tab Shell with Rock-Solid Material 3 Bottom Bar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                    )
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.HOME,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentTab = MainTab.HOME
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.HOME) Icons.Rounded.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontWeight = if (currentTab == MainTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.ALL_TOOLS,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentTab = MainTab.ALL_TOOLS
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.ALL_TOOLS) Icons.Rounded.GridView else Icons.Outlined.GridView,
                                contentDescription = "All Tools"
                            )
                        },
                        label = { Text("All Tools", fontWeight = if (currentTab == MainTab.ALL_TOOLS) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.OUTPUTS,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentTab = MainTab.OUTPUTS
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.OUTPUTS) Icons.Rounded.Folder else Icons.Outlined.Folder,
                                contentDescription = "Outputs"
                            )
                        },
                        label = { Text("Files", fontWeight = if (currentTab == MainTab.OUTPUTS) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.DEVICE,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentTab = MainTab.DEVICE
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.DEVICE) Icons.Rounded.Storage else Icons.Outlined.Storage,
                                contentDescription = "Storage"
                            )
                        },
                        label = { Text("Storage", fontWeight = if (currentTab == MainTab.DEVICE) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors
                    )
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(150),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    MainTab.HOME -> HomeScreen(
                        onSelectTool = openTool,
                        onNavigateToAllTools = { currentTab = MainTab.ALL_TOOLS }
                    )
                    MainTab.ALL_TOOLS -> AllToolsScreen(
                        onSelectTool = openTool
                    )
                    MainTab.OUTPUTS -> OutputsScreen(
                        onOpenTool = openTool
                    )
                    MainTab.DEVICE -> StorageStatsScreen()
                }
            }
        }
    }
}
