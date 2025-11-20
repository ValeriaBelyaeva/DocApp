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
 * Application-wide logging utility that writes logs to both Android Log and a file.
 * Provides centralized logging with timestamps and file persistence.
 * 
 * Works by writing log messages to both Android Log system and a log file in external storage,
 * allowing logs to persist across app restarts for debugging purposes.
 */
object AppLogger {
    private var context: Context? = null
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Initializes the logger with application context and creates log file.
     * 
     * arguments:
     *     ctx - Context: Android context for accessing external storage
     * 
     * return:
     *     Unit - No return value
     */
    fun init(ctx: Context) {
        context = ctx
        try {
            logFile = File(ctx.getExternalFilesDir(null), "app_logs.txt")
            log("AppLogger", "Logger initialized")
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to initialize logger: ${e.message}")
        }
    }
    
    /**
     * Logs a message with a tag to both Android Log and log file.
     * 
     * arguments:
     *     tag - String: Tag to identify the source of the log message
     *     message - String: The log message to write
     * 
     * return:
     *     Unit - No return value
     */
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
    
    /**
     * Gets the log file where logs are written.
     * 
     * return:
     *     logFile - File?: The log file if initialized, null otherwise
     */
    fun getLogFile(): File? = logFile
}

/**
 * Error handling utility that provides centralized error logging and user feedback.
 * Handles errors, warnings, info messages, and uncaught exceptions.
 * 
 * Works by logging errors to AppLogger and providing different severity levels
 * for error messages. Sets up global uncaught exception handler.
 */
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

/**
 * Debug utility for logging URI-related operations during development.
 * Only logs when debug mode is enabled (in DEBUG builds).
 * 
 * Works by checking if debug mode is enabled before logging URI operations,
 * helping developers track file URI handling during development.
 */
object UriDebugger {
    
    private var isDebugEnabled = false
    
    /**
     * Initializes the URI debugger, enabling debug mode in DEBUG builds.
     * 
     * arguments:
     *     ctx - Context: Android context (currently unused, kept for consistency)
     * 
     * return:
     *     Unit - No return value
     */
    fun init(ctx: Context) {
        isDebugEnabled = BuildConfig.DEBUG
    }
    
    /**
     * Logs a debug message with optional URI information.
     * 
     * arguments:
     *     message - String: Debug message to log
     *     uri - Uri?: Optional URI to include in the log message
     * 
     * return:
     *     Unit - No return value
     */
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

/**
 * Generates a new unique identifier using UUID.
 * 
 * return:
 *     id - String: A new UUID string
 */
fun newId(): String = UUID.randomUUID().toString()

/**
 * Gets the current system time in milliseconds since epoch.
 * 
 * return:
 *     timestamp - Long: Current time in milliseconds
 */
fun now(): Long = System.currentTimeMillis()
