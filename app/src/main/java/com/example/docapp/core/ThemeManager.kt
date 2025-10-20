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
    // Dark theme glass colors
    val darkGlassTintTop = Color(0x1AFFFFFF)      // 10% white for glass top
    val darkGlassTintBottom = Color(0x0DFFFFFF)   // 5% white for glass bottom
    val darkGlassHighlight = Color(0x33FFFFFF)    // 20% white highlight
    val darkGlassShadow = Color(0x40000000)       // 25% black shadow
    val darkGlassBorderBright = Color(0x4DFFFFFF) // 30% white bright border
    val darkGlassBorderShadow = Color(0x1AD7FC5A) // 10% lime green shadow border
}

/**
 * Dark theme color scheme
 */
object DarkThemeColors {
    // Dark theme with lime green accent
    val darkPrimary = Color(0xFFD7FC5A)           // Bright lime green for dark theme
    val darkOnPrimary = Color(0xFF000000)         // Black text on lime green
    val darkPrimaryContainer = Color(0xFF2D3A00)  // Dark lime green container
    val darkOnPrimaryContainer = Color(0xFFE8FF8A) // Light lime green text
    
    val darkSecondary = Color(0xFF81C784)         // Light green for secondary
    val darkOnSecondary = Color(0xFF000000)      // Black text on light green
    val darkSecondaryContainer = Color(0xFF1B5E20) // Very dark green container
    val darkOnSecondaryContainer = Color(0xFFC8E6C9) // Light green text
    
    val darkTertiary = Color(0xFF80CBC4)         // Light teal for tertiary
    val darkOnTertiary = Color(0xFF000000)       // Black text on teal
    val darkTertiaryContainer = Color(0xFF004D40) // Dark teal container
    val darkOnTertiaryContainer = Color(0xFFA6F2E2) // Light teal text
    
    val darkBackground = Color(0xFF121212)        // Very dark background
    val darkOnBackground = Color(0xFFFFFFFF)     // White text on dark background
    val darkSurface = Color(0xFF1E1E1E)          // Dark surface
    val darkOnSurface = Color(0xFFFFFFFF)        // White text on dark surface
    val darkSurfaceVariant = Color(0xFF2C2C2C)   // Darker surface variant
    val darkOnSurfaceVariant = Color(0xFFB0B0B0)  // Light gray text
    val darkOutline = Color(0xFFFFFFFF)          // White outline for dark theme
    val darkOutlineVariant = Color(0xFFB0B0B0)   // Light gray outline variant
}
