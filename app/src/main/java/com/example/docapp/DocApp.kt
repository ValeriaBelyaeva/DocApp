package com.example.docapp

import android.app.Application
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ThemeManager
import com.example.docapp.core.FolderStateStore

class DocApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logger first
        AppLogger.init(this)
        AppLogger.log("DocApp", "DocApp onCreate started")
        
        // Initialize error handler
        ErrorHandler.init(this)
        ErrorHandler.showInfo("Application starting...")
        
        // Initialize theme manager
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
