package com.example.docapp

import android.app.Application
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ThemeManager

class DocApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем логгер первым
        AppLogger.init(this)
        AppLogger.log("DocApp", "DocApp onCreate started")
        
        // Инициализируем обработчик ошибок
        ErrorHandler.init(this)
        ErrorHandler.showInfo("Приложение запускается...")
        
        // Инициализируем менеджер темы
        ThemeManager.initialize(this)
        
        try {
            ServiceLocator.init(this)
            AppLogger.log("DocApp", "ServiceLocator initialized successfully")
            ErrorHandler.showSuccess("Приложение успешно инициализировано")
        } catch (e: Exception) {
            AppLogger.log("DocApp", "ERROR: Failed to initialize ServiceLocator: ${e.message}")
            AppLogger.log("DocApp", "ERROR: Exception type: ${e.javaClass.simpleName}")
            AppLogger.log("DocApp", "ERROR: Stack trace: ${e.stackTraceToString()}")
            
            val errorMessage = when (e) {
                is RuntimeException -> "Критическая ошибка инициализации приложения: ${e.message}"
                is SecurityException -> "Ошибка безопасности при инициализации: ${e.message}"
                is IllegalStateException -> "Ошибка состояния системы: ${e.message}"
                else -> "Не удалось инициализировать приложение: ${e.message}"
            }
            
            ErrorHandler.showCriticalError(errorMessage, e)
            throw e
        }
    }
}
