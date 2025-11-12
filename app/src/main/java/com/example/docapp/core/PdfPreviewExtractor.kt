package com.example.docapp.core

import android.content.Context
import android.net.Uri

/**
 * Утилита для извлечения превью текста из PDF файлов
 * Пока используем простое отображение без сложной обработки PDF
 */
object PdfPreviewExtractor {
    
    /**
     * Извлекает превью для PDF файла
     * Пока возвращаем простое сообщение, так как PDF парсинг сложен
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun extractPreview(context: Context, uri: Uri, maxLines: Int = 5): String {
        return try {
            // Пока возвращаем простое превью без парсинга PDF
            // В будущем можно добавить реальный парсинг PDF
            ""
        } catch (e: Exception) {
            AppLogger.log("PdfPreviewExtractor", "ERROR: Failed to extract PDF preview: ${e.message}")
            "Failed to read PDF"
        }
    }
}
