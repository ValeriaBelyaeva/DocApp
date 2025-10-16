package com.example.docapp.core

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream

/**
 * Простое хранилище вложений (временно без шифрования)
 */
class EncryptedAttachmentStore(private val context: Context) : AttachmentStore {
    
    companion object {
        private const val ATTACHMENTS_DIR = "attachments"
    }
    
    private val attachmentsDir: File by lazy {
        val dir = File(context.filesDir, ATTACHMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    override fun persist(uri: Uri): Uri {
        return try {
            val fileName = generateFileName(uri)
            val targetFile = File(attachmentsDir, fileName)
            
            // Копируем файл
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Не удалось открыть поток для файла")
            
            AppLogger.log("EncryptedAttachmentStore", "File persisted: $fileName")
            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            AppLogger.log("EncryptedAttachmentStore", "ERROR: Failed to persist file: ${e.message}")
            ErrorHandler.showError("Не удалось сохранить файл", e)
            throw e
        }
    }
    
    override fun release(uri: Uri) {
        // Простая реализация - ничего не делаем
        // В будущем здесь можно добавить шифрование
    }
    
    fun retrieve(uri: Uri): InputStream? {
        return try {
            // Проверяем, является ли URI файлом в нашей папке
            val file = File(uri.path ?: return null)
            if (file.exists() && file.isFile) {
                file.inputStream()
            } else {
                // Если файл не найден, пытаемся открыть через ContentResolver
                // Это для совместимости со старыми URI
                context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            AppLogger.log("EncryptedAttachmentStore", "ERROR: Failed to retrieve file: ${e.message}")
            ErrorHandler.showError("Не удалось открыть файл", e)
            null
        }
    }
    
    fun delete(uri: Uri) {
        try {
            val file = File(uri.path ?: return)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            AppLogger.log("EncryptedAttachmentStore", "ERROR: Failed to delete file: ${e.message}")
            ErrorHandler.showWarning("Не удалось удалить файл: ${e.message}")
        }
    }
    
    private fun generateFileName(uri: Uri): String {
        val originalFileName = uri.lastPathSegment ?: "attachment"
        val timestamp = System.currentTimeMillis()
        val extension = originalFileName.substringAfterLast('.', "")
        return "${timestamp}_${originalFileName.hashCode()}.${extension}"
    }
}
