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
    val darkGlassTintTop = Color(0x3327292F)
    val darkGlassTintBottom = Color(0x33181B22)
    val darkGlassHighlight = Color(0x268B8C8E)
    val darkGlassShadow = Color(0x40000000)
    val darkGlassBorderBright = Color(0x33BCED57)
    val darkGlassBorderShadow = Color(0x33202127)
}

/**
 * Dark theme color scheme
 */
object DarkThemeColors {
    val darkPrimary = Color(0xFFBCED57)
    val darkOnPrimary = Color(0xFF10140A)
    val darkPrimaryContainer = Color(0xFF1A2A11)
    val darkOnPrimaryContainer = Color(0xFFBCED57)

    val darkSecondary = Color(0xFFA4CF49)
    val darkOnSecondary = Color(0xFF0E1406)
    val darkSecondaryContainer = Color(0xFF233016)
    val darkOnSecondaryContainer = Color(0xFFA4CF49)

    val darkTertiary = Color(0xFFBCCBDE)
    val darkOnTertiary = Color(0xFF0E1117)
    val darkTertiaryContainer = Color(0xFF1C2430)
    val darkOnTertiaryContainer = Color(0xFFBCCBDE)

    val darkBackground = Color(0xFF0F121A)
    val darkOnBackground = Color(0xFFFBFBFB)
    val darkSurface = Color(0xFF27292F)
    val darkOnSurface = Color(0xFFFBFBFB)
    val darkSurfaceVariant = Color(0xFF1C1F26)
    val darkOnSurfaceVariant = Color(0xFF8B8C8E)
    val darkOutline = Color(0xFF3A3D44)
    val darkOutlineVariant = Color(0xFF2B2E34)
}
