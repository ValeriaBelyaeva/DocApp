package com.example.docapp.core

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.example.docapp.BuildConfig
import java.io.File

/**
 * Дебаг система для отслеживания проблем с URI
 */
object UriDebugger {
    
    private var context: Context? = null
    private var isDebugEnabled = false
    
    fun init(ctx: Context) {
        context = ctx
        // Включаем дебаг только в debug сборке
        isDebugEnabled = BuildConfig.DEBUG
    }
    
    fun showUriDebug(message: String, uri: Uri? = null) {
        if (!isDebugEnabled) return
        
        context?.let { ctx ->
            Toast.makeText(ctx, "🔍 $message", Toast.LENGTH_SHORT).show()
            if (uri != null) {
                Toast.makeText(ctx, "📁 $uri", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun showUriError(message: String, uri: Uri? = null, throwable: Throwable? = null) {
        if (!isDebugEnabled) return
        
        context?.let { ctx ->
            Toast.makeText(ctx, "❌ $message", Toast.LENGTH_SHORT).show()
            if (uri != null) {
                Toast.makeText(ctx, "📁 $uri", Toast.LENGTH_SHORT).show()
            }
            if (throwable != null) {
                Toast.makeText(ctx, "💥 ${throwable.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun showUriSuccess(message: String, uri: Uri? = null) {
        if (!isDebugEnabled) return
        
        context?.let { ctx ->
            Toast.makeText(ctx, "✅ $message", Toast.LENGTH_SHORT).show()
            if (uri != null) {
                Toast.makeText(ctx, "📁 $uri", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun enableDebug() {
        isDebugEnabled = true
    }
    
    fun disableDebug() {
        isDebugEnabled = false
    }
}
