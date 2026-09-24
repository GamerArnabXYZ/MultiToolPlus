package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OmniPrimaryDark,
    onPrimary = OmniOnPrimaryDark,
    primaryContainer = OmniPrimaryContainerDark,
    onPrimaryContainer = OmniOnPrimaryContainerDark,
    secondary = OmniSecondaryDark,
    onSecondary = OmniOnSecondaryDark,
    secondaryContainer = OmniSecondaryContainerDark,
    onSecondaryContainer = OmniOnSecondaryContainerDark,
    tertiary = OmniTertiaryDark,
    onTertiary = OmniOnTertiaryDark,
    tertiaryContainer = OmniTertiaryContainerDark,
    onTertiaryContainer = OmniOnTertiaryContainerDark,
    background = OmniBackgroundDark,
    surface = OmniSurfaceDark,
    surfaceVariant = OmniSurfaceVariantDark,
    onSurface = OmniOnSurfaceDark,
    onSurfaceVariant = OmniOnSurfaceVariantDark,
    outline = OmniOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = OmniPrimaryLight,
    onPrimary = OmniOnPrimaryLight,
    primaryContainer = OmniPrimaryContainerLight,
    onPrimaryContainer = OmniOnPrimaryContainerLight,
    secondary = OmniSecondaryLight,
    onSecondary = OmniOnSecondaryLight,
    secondaryContainer = OmniSecondaryContainerLight,
    onSecondaryContainer = OmniOnSecondaryContainerLight,
    tertiary = OmniTertiaryLight,
    onTertiary = OmniOnTertiaryLight,
    tertiaryContainer = OmniTertiaryContainerLight,
    onTertiaryContainer = OmniOnTertiaryContainerLight,
    background = OmniBackgroundLight,
    surface = OmniSurfaceLight,
    surfaceVariant = OmniSurfaceVariantLight,
    onSurface = OmniOnSurfaceLight,
    onSurfaceVariant = OmniOnSurfaceVariantLight,
    outline = OmniOutlineLight
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = ThemeManager.currentThemeMode,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
