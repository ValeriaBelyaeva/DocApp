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

// Light theme with warm brown accent colors
private val PrimaryGreen = Color(0xFFD4A574)      // Warm brown for primary actions
private val PrimaryContainer = Color(0xFFF5E6D3)  // Light warm brown container
private val OnPrimaryContainer = Color(0xFF2D1B00) // Dark text on light warm brown
private val SecondaryGreen = Color(0xFFE6A85C)     // Warm orange for secondary
private val SecondaryContainer = Color(0xFFFFF8E1) // Very light warm container
private val SecondaryOnContainer = Color(0xFF2D1B00) // Dark text on warm container
private val TertiaryTeal = Color(0xFFB8860B)      // Golden for tertiary
private val TertiaryContainer = Color(0xFFFFF8DC) // Light golden container
private val TertiaryOnContainer = Color(0xFF00201B) // Dark text on light teal

// Background colors for readability
private val SoftBackground = Color(0xFFFFFBF0)    // Very soft yellow-orange background
private val SurfaceBase = Color(0xFFFFFEF8)       // Almost white with warm tint
private val SurfaceVariant = Color(0xFFFFF8E1)   // Light warm variant
private val OnSurfaceVariant = Color(0xFF2D1B00) // Dark warm text on light surfaces
private val Outline = Color(0xFFD4A574)          // Warm outline
private val OutlineVariant = Color(0xFFE6D4B8)   // Light warm outline

// Glassmorphism colors - enhanced for better visibility
private val GlassTintTop = Color(0xF2FFFFFF)      // 95% white for glass top (more opaque)
private val GlassTintBottom = Color(0xE6FFFFFF)   // 90% white for glass bottom (more opaque)
private val GlassHighlight = Color(0x99FFFFFF)    // 60% white highlight (more visible)
private val GlassShadow = Color(0x60000000)        // 37% black shadow (more prominent)

val LightGlassColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryOnContainer,
    tertiary = TertiaryTeal,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TertiaryOnContainer,
    background = SoftBackground,
    onBackground = Color(0xFF121613), // Almost black for high contrast
    surface = SurfaceBase,
    onSurface = Color(0xFF121613), // Almost black for high contrast
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF243027),
    inverseOnSurface = Color(0xFFEFF4EE),
    inversePrimary = Color(0xFF83C789),
    surfaceTint = PrimaryGreen,
    scrim = Color(0x88000000)
)

private val LightGlassTokens = GlassColors(
    containerTop = GlassTintTop,
    containerBottom = GlassTintBottom,
    highlight = GlassHighlight,
    borderBright = Color.White.copy(alpha = 0.7f),
    borderShadow = Color(0x33D4A574),
    shadowColor = GlassShadow
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
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE1E1E1),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFFD7FC5A),
    surfaceTint = DarkThemeColors.darkPrimary,
    scrim = Color(0x88000000)
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
    val glassTokens = when (surfaceStyle) {
        SurfaceStyle.Glass -> if (isDarkTheme) DarkGlassTokens else LightGlassTokens
        SurfaceStyle.Matte -> matteTokensFor(colorScheme)
    }
    
    CompositionLocalProvider(LocalGlassColors provides glassTokens) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography) {
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
