package com.example.docapp.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme management system for switching between light and dark themes
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_THEME = "dark_theme"
    
    private var _isDarkTheme by mutableStateOf(false)
    var isDarkTheme: Boolean
        get() = _isDarkTheme
        private set(value) {
            _isDarkTheme = value
        }

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkTheme = prefs.getBoolean(KEY_DARK_THEME, false)
    }

    fun toggleTheme(context: Context) {
        isDarkTheme = !isDarkTheme
        saveTheme(context)
    }

    fun setTheme(context: Context, dark: Boolean) {
        isDarkTheme = dark
        saveTheme(context)
    }
    
    private fun saveTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_THEME, isDarkTheme).apply()
    }
}

/**
 * CompositionLocal for theme state
 */
val LocalThemeManager = staticCompositionLocalOf { ThemeManager }

/**
 * Dark theme colors for glassmorphism
 */
object DarkGlassColors {
    val darkGlassTintTop = Color(0x33212C3A)
    val darkGlassTintBottom = Color(0x33151F2C)
    val darkGlassHighlight = Color(0x26C6FF00)
    val darkGlassShadow = Color(0x4010151E)
    val darkGlassBorderBright = Color(0x4DC6FF00)
    val darkGlassBorderShadow = Color(0x1A0C141D)
}

/**
 * Dark theme color scheme
 */
object DarkThemeColors {
    val darkPrimary = Color(0xFFC6FF00)
    val darkOnPrimary = Color(0xFF050A02)
    val darkPrimaryContainer = Color(0xFF121C27)
    val darkOnPrimaryContainer = Color(0xFFC6FF00)

    val darkSecondary = Color(0xFFC6FF00)
    val darkOnSecondary = Color(0xFF050A02)
    val darkSecondaryContainer = Color(0xFF202C3A)
    val darkOnSecondaryContainer = Color(0xFFE8EEF6)

    val darkTertiary = Color(0xFFC6FF00)
    val darkOnTertiary = Color(0xFF050A02)
    val darkTertiaryContainer = Color(0xFF152130)
    val darkOnTertiaryContainer = Color(0xFFE8EEF6)

    val darkBackground = Color(0xFF060B12)
    val darkOnBackground = Color(0xFFE8EEF6)
    val darkSurface = Color(0xFF202C3A)
    val darkOnSurface = Color(0xFFE8EEF6)
    val darkSurfaceVariant = Color(0xFF151F2C)
    val darkOnSurfaceVariant = Color(0xFF93A4B8)
    val darkOutline = Color(0xFF152130)
    val darkOutlineVariant = Color(0xFF1E2A38)
}
