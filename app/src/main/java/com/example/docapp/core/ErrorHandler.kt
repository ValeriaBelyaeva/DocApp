package com.example.docapp.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Централизованный обработчик ошибок приложения
 * Показывает пользователю понятные сообщения об ошибках
 */
object ErrorHandler {
    
    private var context: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    
    fun init(ctx: Context) {
        context = ctx
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
        
        // Показываем Toast
        showToast("❌ $errorMessage", Toast.LENGTH_LONG)
    }
    
    /**
     * Показывает критическую ошибку
     */
    fun showCriticalError(message: String, throwable: Throwable? = null) {
        val errorMessage = "КРИТИЧЕСКАЯ ОШИБКА: $message"
        
        // Логируем критическую ошибку
        AppLogger.log("ErrorHandler", "CRITICAL ERROR: $errorMessage")
        if (throwable != null) {
            AppLogger.log("ErrorHandler", "Stack trace: ${getStackTrace(throwable)}")
        }
        
        // Показываем Toast
        showToast("🚨 $errorMessage", Toast.LENGTH_LONG)
    }
    
    /**
     * Показывает предупреждение
     */
    fun showWarning(message: String) {
        AppLogger.log("ErrorHandler", "WARNING: $message")
        showToast("⚠️ $message", Toast.LENGTH_SHORT)
    }
    
    /**
     * Показывает сообщение об успехе
     */
    fun showSuccess(message: String) {
        AppLogger.log("ErrorHandler", "SUCCESS: $message")
        showToast("✅ $message", Toast.LENGTH_SHORT)
    }
    
    /**
     * Показывает информационное сообщение
     */
    fun showInfo(message: String) {
        AppLogger.log("ErrorHandler", "INFO: $message")
        showToast("ℹ️ $message", Toast.LENGTH_SHORT)
    }
    
    /**
     * Форматирует сообщение об ошибке
     */
    private fun formatErrorMessage(message: String, throwable: Throwable?): String {
        return if (throwable != null) {
            "$message: ${throwable.message ?: "Неизвестная ошибка"}"
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
     * Показывает Toast сообщение
     */
    private fun showToast(message: String, duration: Int) {
        context?.let { ctx ->
            handler.post {
                Toast.makeText(ctx, message, duration).show()
            }
        }
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
                
                // Показываем Toast напрямую, без вызова showCriticalError
                context?.let { ctx ->
                    handler.post {
                        Toast.makeText(ctx, "🚨 КРИТИЧЕСКАЯ ОШИБКА: Необработанное исключение", Toast.LENGTH_LONG).show()
                    }
                }
                
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
            showError("Произошла ошибка при выполнении операции", e)
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
            showError("Произошла ошибка при выполнении операции", e)
            null
        }
    }
}