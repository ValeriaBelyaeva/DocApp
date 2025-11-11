package com.example.docapp.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.example.docapp.core.ThemeManager
import com.example.docapp.core.DarkGlassColors
import com.example.docapp.core.DarkThemeColors

// Light theme with updated warm pink-brown palette
private val LightBackground = Color(0xFFF5F0EB)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF0E3D8)
private val LightOutline = Color(0xFF7A4A33)
private val LightOutlineSoft = Color(0xFFE0D2C6)

private val TextPrimaryLight = Color(0xFF2B2220)
private val TextSecondaryLight = Color(0xFF6F6058)
private val TextDisabledLight = Color(0xFFB5A7A0)

private val AccentPink = Color(0xFFD2667A)
private val AccentPinkDark = Color(0xFFB04E63)
private val AccentPinkSoft = Color(0xFFF7DFE5)

private val AccentBrown = Color(0xFF9C5A3C)
private val AccentBrownDark = Color(0xFF6D3C28)
private val AccentBrownSoft = Color(0xFFF2E1D7)

private val SuccessLight = Color(0xFF3B8F6A)
private val WarningLight = Color(0xFFD98A32)
private val ErrorLight = Color(0xFFD64545)

// Glassmorphism colors - enhanced for better visibility
private val GlassTintTop = Color(0xFFFFFFFF)
private val GlassTintBottom = LightSurfaceVariant
private val GlassHighlight = Color(0x66FFFFFF)
private val GlassShadow = Color(0x33000000)

val LightGlassColorScheme = lightColorScheme(
    primary = AccentPink,
    onPrimary = Color.White,
    primaryContainer = AccentPinkSoft,
    onPrimaryContainer = AccentPinkDark,
    secondary = AccentBrown,
    onSecondary = Color.White,
    secondaryContainer = AccentBrownSoft,
    onSecondaryContainer = AccentBrownDark,
    tertiary = AccentPinkDark,
    onTertiary = Color.White,
    tertiaryContainer = AccentPinkSoft,
    onTertiaryContainer = AccentPinkDark,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    outlineVariant = LightOutlineSoft,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorLight.copy(alpha = 0.12f),
    onErrorContainer = ErrorLight,
    inverseSurface = AccentBrownDark,
    inverseOnSurface = LightSurface,
    inversePrimary = AccentPinkDark,
    surfaceTint = AccentPink,
    scrim = Color(0x88000000)
)

val DarkGlassColorScheme = darkColorScheme(
    primary = DarkThemeColors.darkPrimary,
    onPrimary = DarkThemeColors.darkOnPrimary,
    primaryContainer = DarkThemeColors.darkPrimaryContainer,
    onPrimaryContainer = DarkThemeColors.darkOnPrimaryContainer,
    secondary = DarkThemeColors.darkSecondary,
    onSecondary = DarkThemeColors.darkOnSecondary,
    secondaryContainer = DarkThemeColors.darkSecondaryContainer,
    onSecondaryContainer = DarkThemeColors.darkOnSecondaryContainer,
    tertiary = DarkThemeColors.darkTertiary,
    onTertiary = DarkThemeColors.darkOnTertiary,
    tertiaryContainer = DarkThemeColors.darkTertiaryContainer,
    onTertiaryContainer = DarkThemeColors.darkOnTertiaryContainer,
    background = DarkThemeColors.darkBackground,
    onBackground = DarkThemeColors.darkOnBackground,
    surface = DarkThemeColors.darkSurface,
    onSurface = DarkThemeColors.darkOnSurface,
    surfaceVariant = DarkThemeColors.darkSurfaceVariant,
    onSurfaceVariant = DarkThemeColors.darkOnSurfaceVariant,
    outline = DarkThemeColors.darkOutline,
    outlineVariant = DarkThemeColors.darkOutlineVariant,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorLight.copy(alpha = 0.2f),
    onErrorContainer = ErrorLight,
    inverseSurface = DarkThemeColors.darkSurfaceVariant,
    inverseOnSurface = DarkThemeColors.darkOnSurface,
    inversePrimary = DarkThemeColors.darkPrimary,
    surfaceTint = DarkThemeColors.darkPrimary,
    scrim = Color(0x88000000)
)

private val LightGlassTokens = GlassColors(
    containerTop = GlassTintTop,
    containerBottom = GlassTintBottom,
    highlight = GlassHighlight,
    borderBright = AccentPink.copy(alpha = 0.35f),
    borderShadow = LightOutlineSoft.copy(alpha = 0.5f),
    shadowColor = GlassShadow
)

private val DarkGlassTokens = GlassColors(
    containerTop = DarkGlassColors.darkGlassTintTop,
    containerBottom = DarkGlassColors.darkGlassTintBottom,
    highlight = DarkGlassColors.darkGlassHighlight,
    borderBright = DarkGlassColors.darkGlassBorderBright,
    borderShadow = DarkGlassColors.darkGlassBorderShadow,
    shadowColor = DarkGlassColors.darkGlassShadow
)

@Composable
fun DocTheme(content: @Composable () -> Unit) {
    val isDarkTheme = ThemeManager.isDarkTheme
    val colorScheme = if (isDarkTheme) DarkGlassColorScheme else LightGlassColorScheme
    val surfaceStyle = ThemeConfig.surfaceStyle
    val surfaceTokens = SurfaceTokens.current(surfaceStyle)
    val glassTokens = when (surfaceStyle) {
        SurfaceStyle.Glass -> if (isDarkTheme) DarkGlassTokens else LightGlassTokens
        SurfaceStyle.Matte -> matteTokensFor(colorScheme)
    }
    
    CompositionLocalProvider(LocalGlassColors provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = surfaceTokens.materialShapes
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                content()
            }
        }
    }
}

private fun matteTokensFor(colorScheme: ColorScheme): GlassColors = GlassColors(
    containerTop = colorScheme.surface,
    containerBottom = colorScheme.surface,
    highlight = Color.Transparent,
    borderBright = Color.Transparent,
    borderShadow = Color.Transparent,
    shadowColor = Color.Transparent
)
