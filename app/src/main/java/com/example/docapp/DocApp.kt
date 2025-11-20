package com.example.docapp
import android.app.Application
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ThemeManager
import com.example.docapp.core.FolderStateStore
/**
 * Main application class that initializes core services and components on app startup.
 * Sets up logging, error handling, theme management, folder state, and service locator.
 * 
 * Works by initializing all core systems in onCreate() before any activities are created,
 * ensuring services are available when the app starts. Handles initialization errors gracefully.
 */
class DocApp : Application() {
    /**
     * Initializes the application by setting up core services and components.
     * Called by Android system when the application process is created.
     * 
     * Works by initializing AppLogger, ErrorHandler, ThemeManager, FolderStateStore, and ServiceLocator
     * in sequence. Catches and handles initialization errors, showing appropriate error messages.
     * 
     * return:
     *     Unit - No return value
     * 
     * throws:
     *     Exception: If ServiceLocator initialization fails, the exception is rethrown after logging
     */
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.log("DocApp", "DocApp onCreate started")
        ErrorHandler.init(this)
        ErrorHandler.showInfo("Application starting...")
        ThemeManager.initialize(this)
        FolderStateStore.init(this)
        try {
            ServiceLocator.init(this)
            AppLogger.log("DocApp", "ServiceLocator initialized successfully")
            ErrorHandler.showSuccess("Application initialized successfully")
        } catch (e: Exception) {
            AppLogger.log("DocApp", "ERROR: Failed to initialize ServiceLocator: ${e.message}")
            AppLogger.log("DocApp", "ERROR: Exception type: ${e.javaClass.simpleName}")
            AppLogger.log("DocApp", "ERROR: Stack trace: ${e.stackTraceToString()}")
            val errorMessage = when (e) {
                is RuntimeException -> "Critical application initialization error: ${e.message}"
                is SecurityException -> "Security error during initialization: ${e.message}"
                is IllegalStateException -> "System state error during initialization: ${e.message}"
                else -> "Failed to initialize application: ${e.message}"
            }
            ErrorHandler.showCriticalError(errorMessage, e)
            throw e
        }
    }
}
