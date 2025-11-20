package com.example.docapp.core
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
val LocalThemeManager = staticCompositionLocalOf { ThemeManager }
