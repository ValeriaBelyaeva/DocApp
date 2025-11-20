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
        
        Log.i(tag, message)
        
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

object ErrorHandler {
    
    @Suppress("UNUSED_PARAMETER")
    fun init(ctx: Context) {
        setupUncaughtExceptionHandler()
    }
    
    fun showError(message: String, throwable: Throwable? = null) {
        val errorMessage = formatErrorMessage(message, throwable)
        
        AppLogger.log("ErrorHandler", "ERROR: $errorMessage")
        if (throwable != null) {
            AppLogger.log("ErrorHandler", "Stack trace: ${getStackTrace(throwable)}")
        }
        
    }
    
    fun showCriticalError(message: String, throwable: Throwable? = null) {
        val errorMessage = formatErrorMessage(message, throwable)
        
        AppLogger.log("ErrorHandler", "CRITICAL ERROR: $errorMessage")
        if (throwable != null) {
            AppLogger.log("ErrorHandler", "Stack trace: ${getStackTrace(throwable)}")
        }
        
    }
    
    fun showWarning(message: String) {
        AppLogger.log("ErrorHandler", "WARNING: $message")
    }
    
    fun showSuccess(message: String) {
        AppLogger.log("ErrorHandler", "SUCCESS: $message")
    }
    
    fun showInfo(message: String) {
        AppLogger.log("ErrorHandler", "INFO: $message")
    }
    
    private fun formatErrorMessage(message: String, throwable: Throwable?): String {
        return if (throwable != null) {
            "$message: ${throwable.message ?: "Unknown error"}"
        } else {
            message
        }
    }
    
    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    private fun setupUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                AppLogger.log("ErrorHandler", "UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}")
                
                AppLogger.log("ErrorHandler", "Stack trace: ${getStackTrace(throwable)}")
                
            } catch (e: Exception) {
                android.util.Log.e("ErrorHandler", "Failed to handle uncaught exception", e)
            }
            
            System.exit(1)
        }
        AppLogger.log("ErrorHandler", "Global uncaught exception handler initialized")
    }
    
    fun <T> safeExecute(action: () -> T?): T? {
        return try {
            action()
        } catch (e: Exception) {
            showError("", e)
            null
        }
    }
    
    suspend fun <T> safeExecuteSuspend(action: suspend () -> T?): T? {
        return try {
            action()
        } catch (e: Exception) {
            showError("", e)
            null
        }
    }
}

object UriDebugger {
    
    private var isDebugEnabled = false
    
    fun init(ctx: Context) {
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

fun newId(): String = UUID.randomUUID().toString()
fun now(): Long = System.currentTimeMillis()
