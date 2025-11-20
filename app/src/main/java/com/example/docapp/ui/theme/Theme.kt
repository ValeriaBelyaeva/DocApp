package com.example.docapp.ui.theme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import com.example.docapp.core.ThemeManager
import com.example.docapp.core.setSystemBarColors
@Composable
fun DocTheme(content: @Composable () -> Unit) {
    val isDarkTheme = ThemeManager.isDarkTheme
    val colorScheme = remember(isDarkTheme) {
        if (isDarkTheme) ThemePalette.darkColorScheme else ThemePalette.lightColorScheme
    }
    val surfaceStyle = ThemeConfig.surfaceStyle
    val surfaceTokens = remember(surfaceStyle) { SurfaceTokens.current(surfaceStyle) }
    val glassTokens = remember(surfaceStyle, colorScheme, isDarkTheme) {
        when (surfaceStyle) {
            SurfaceStyle.Glass -> if (isDarkTheme) ThemePalette.darkGlassTokens else ThemePalette.lightGlassTokens
            SurfaceStyle.Matte -> ThemePalette.matteGlassTokens(colorScheme)
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.setSystemBarColors(
                statusColor = colorScheme.background.toArgb(),
                navColor = colorScheme.surface.toArgb(),
                darkIcons = !isDarkTheme
            )
        }
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
