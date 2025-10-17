package com.example.docapp.core

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileWriter
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
        
        // Показываем Toast только для критических ошибок с упрощенным форматом
        if (message.contains("ERROR") || message.contains("FAILED") || message.contains("CRITICAL")) {
            context?.let { ctx ->
                val toastMessage = message.replace("ERROR: ", "").replace("FAILED: ", "").replace("CRITICAL: ", "")
                Toast.makeText(ctx, toastMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    fun getLogFile(): File? = logFile
}
