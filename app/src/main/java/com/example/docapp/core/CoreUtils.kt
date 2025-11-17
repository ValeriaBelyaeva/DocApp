package com.example.docapp.core

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.docapp.BuildConfig
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Объединенные утилиты для логирования, обработки ошибок и отладки
 */

// ===== LOGGING =====

object AppLogger {
    private var context: Context? = null
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    fun init(ctx: Context) {
        context = ctx
        try {
            logFile = File(ctx.getExternalFilesDir(null), "app_logs.txt")
            log("AppLogger", "Logger initialized")
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to initialize logger: ${e.message}")
        }
    }
    
    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] $tag: $message"
        
        // Логируем в Android Log
        Log.i(tag, message)
        
        // Сохраняем в файл
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.appendLine(logMessage)
                }
            }
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to write to log file: ${e.message}")
        }
    }
    
    fun getLogFile(): File? = logFile
}

// ===== ERROR HANDLING =====

/**
 * Централизованный обработчик ошибок приложения
 * Показывает пользователю понятные сообщения об ошибках
 */
object ErrorHandler {
    
    @Suppress("UNUSED_PARAMETER")
    fun init(ctx: Context) {
        setupUncaughtExceptionHandler()
    }
    
    /**
     * Показывает ошибку пользователю в Toast
     */
    fun showError(message: String, throwable: Throwable? = null) {
        val errorMessage = formatErrorMessage(message, throwable)
        
        // Логируем ошибку
        AppLogger.log("ErrorHandler", "ERROR: $errorMessage")
        if (throwable != null) {
            AppLogger.log("ErrorHandler", "Stack trace: ${getStackTrace(throwable)}")
        }
        
    }
    
    /**
     * Показывает критическую ошибку
     */
    fun showCriticalError(message: String, throwable: Throwable? = null) {
        val errorMessage = formatErrorMessage(message, throwable)
        
        // Логируем критическую ошибку
        AppLogger.log("ErrorHandler", "CRITICAL ERROR: $errorMessage")
        if (throwable != null) {
            AppLogger.log("ErrorHandler", "Stack trace: ${getStackTrace(throwable)}")
        }
        
    }
    
    /**
     * Показывает предупреждение (только в логах)
     */
    fun showWarning(message: String) {
        AppLogger.log("ErrorHandler", "WARNING: $message")
    }
    
    /**
     * Показывает сообщение об успехе
     */
    fun showSuccess(message: String) {
        AppLogger.log("ErrorHandler", "SUCCESS: $message")
    }
    
    /**
     * Показывает информационное сообщение (только в логах)
     */
    fun showInfo(message: String) {
        AppLogger.log("ErrorHandler", "INFO: $message")
    }
    
    /**
     * Форматирует сообщение об ошибке
     */
    private fun formatErrorMessage(message: String, throwable: Throwable?): String {
        return if (throwable != null) {
            "$message: ${throwable.message ?: "Unknown error"}"
        } else {
            message
        }
    }
    
    /**
     * Получает стек-трейс исключения
     */
    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    /**
     * Устанавливает глобальный обработчик необработанных исключений
     */
    private fun setupUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                AppLogger.log("ErrorHandler", "UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}")
                
                // Логируем стек-трейс
                AppLogger.log("ErrorHandler", "Stack trace: ${getStackTrace(throwable)}")
                
                // Можно добавить отправку отчета об ошибке на сервер
                // Crashlytics.recordException(throwable)
            } catch (e: Exception) {
                // Если даже логирование упало, просто завершаем приложение
                android.util.Log.e("ErrorHandler", "Failed to handle uncaught exception", e)
            }
            
            // Завершаем приложение
            System.exit(1)
        }
        AppLogger.log("ErrorHandler", "Global uncaught exception handler initialized")
    }
    
    /**
     * Безопасное выполнение кода с обработкой ошибок
     */
    fun <T> safeExecute(action: () -> T?): T? {
        return try {
            action()
        } catch (e: Exception) {
            showError("", e)
            null
        }
    }
    
    /**
     * Безопасное выполнение suspend функции с обработкой ошибок
     */
    suspend fun <T> safeExecuteSuspend(action: suspend () -> T?): T? {
        return try {
            action()
        } catch (e: Exception) {
            showError("", e)
            null
        }
    }
}

// ===== URI DEBUGGING =====

/**
 * Дебаг система для отслеживания проблем с URI
 */
object UriDebugger {
    
    private var isDebugEnabled = false
    
    fun init(ctx: Context) {
        // Включаем дебаг только в debug сборке
        isDebugEnabled = BuildConfig.DEBUG
    }
    
    fun showUriDebug(message: String, uri: Uri? = null) {
        if (!isDebugEnabled) return
        
        val uriSuffix = uri?.let { " URI: $it" } ?: ""
        AppLogger.log("UriDebugger", "DEBUG: $message$uriSuffix")
    }
    
    fun showUriError(message: String, uri: Uri? = null, throwable: Throwable? = null) {
        if (!isDebugEnabled) return
        
        val uriSuffix = uri?.let { " URI: $it" } ?: ""
        val errorSuffix = throwable?.let { " Throwable: ${it.message}" } ?: ""
        AppLogger.log("UriDebugger", "ERROR: $message$uriSuffix$errorSuffix")
    }
    
    fun showUriSuccess(message: String, uri: Uri? = null) {
        if (!isDebugEnabled) return
        
        val uriSuffix = uri?.let { " URI: $it" } ?: ""
        AppLogger.log("UriDebugger", "SUCCESS: $message$uriSuffix")
    }
    
    fun enableDebug() {
        isDebugEnabled = true
    }
    
    fun disableDebug() {
        isDebugEnabled = false
    }
}

// ===== UTILITY FUNCTIONS =====

fun newId(): String = UUID.randomUUID().toString()
fun now(): Long = System.currentTimeMillis()
