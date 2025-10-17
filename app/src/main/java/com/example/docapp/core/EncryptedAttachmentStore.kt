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
        // Используем внешнее хранилище для более стабильного сохранения файлов
        val externalDir = context.getExternalFilesDir(null)
        val dir = if (externalDir != null) {
            File(externalDir, ATTACHMENTS_DIR)
        } else {
            // Fallback на внутреннее хранилище если внешнее недоступно
            File(context.filesDir, ATTACHMENTS_DIR)
        }
        
        if (!dir.exists()) {
            val created = dir.mkdirs()
            AppLogger.log("EncryptedAttachmentStore", "Created attachments directory: ${dir.absolutePath}, success: $created")
        }
        
        // Проверяем, есть ли файлы в старом месте (internal storage) и мигрируем их
        val oldDir = File(context.filesDir, ATTACHMENTS_DIR)
        if (oldDir.exists() && oldDir != dir) {
            AppLogger.log("EncryptedAttachmentStore", "Found old attachments directory, migrating files...")
            migrateFiles(oldDir, dir)
        }
        
        AppLogger.log("EncryptedAttachmentStore", "Attachments directory: ${dir.absolutePath}")
        dir
    }
    
    private fun migrateFiles(fromDir: File, toDir: File) {
        try {
            fromDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val targetFile = File(toDir, file.name)
                    if (!targetFile.exists()) {
                        val success = file.copyTo(targetFile, overwrite = false)
                        AppLogger.log("EncryptedAttachmentStore", "Migrated file: ${file.name} -> $success")
                    }
                }
            }
            AppLogger.log("EncryptedAttachmentStore", "File migration completed")
        } catch (e: Exception) {
            AppLogger.log("EncryptedAttachmentStore", "ERROR: Failed to migrate files: ${e.message}")
        }
    }
    
    fun initialize() {
        // Принудительно инициализируем директорию при старте
        val dir = attachmentsDir
        ErrorHandler.showInfo("EncryptedAttachmentStore: Директория: ${dir.absolutePath}")
        
        // Показываем список файлов в директории
        val files = dir.listFiles()
        ErrorHandler.showInfo("EncryptedAttachmentStore: Найдено файлов: ${files?.size ?: 0}")
        files?.forEachIndexed { index, file ->
            ErrorHandler.showInfo("EncryptedAttachmentStore: Файл $index: ${file.name}, размер: ${file.length()}")
        }
    }
    
    override fun persist(uri: Uri): Uri {
        return try {
            ErrorHandler.showInfo("EncryptedAttachmentStore: Сохраняем файл")
            
            // Проверяем, является ли URI уже файлом в нашем хранилище
            if (uri.scheme == "file" && uri.path?.startsWith(attachmentsDir.absolutePath) == true) {
                ErrorHandler.showInfo("EncryptedAttachmentStore: Файл уже в хранилище, возвращаем оригинальный URI")
                return uri
            }
            
            val fileName = generateFileName(uri)
            val targetFile = File(attachmentsDir, fileName)
            
            ErrorHandler.showInfo("EncryptedAttachmentStore: Целевой файл: $fileName")
            
            // Копируем файл только если целевой файл не существует
            if (!targetFile.exists()) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        val bytesCopied = inputStream.copyTo(outputStream)
                        ErrorHandler.showInfo("EncryptedAttachmentStore: Скопировано $bytesCopied байт в $fileName")
                    }
                } ?: throw IllegalStateException("Не удалось открыть поток для файла")
            } else {
                ErrorHandler.showInfo("EncryptedAttachmentStore: Файл уже существует: $fileName")
            }
            
            val newUri = Uri.fromFile(targetFile)
            ErrorHandler.showInfo("EncryptedAttachmentStore: Файл сохранен: $fileName")
            ErrorHandler.showInfo("EncryptedAttachmentStore: Размер файла: ${targetFile.length()} байт")
            ErrorHandler.showInfo("EncryptedAttachmentStore: URI схема: ${newUri.scheme}")
            
            // Проверяем, что URI корректный
            if (newUri.scheme != "file") {
                ErrorHandler.showWarning("EncryptedAttachmentStore: Некорректная схема URI: ${newUri.scheme}")
                // Попробуем создать URI вручную
                val manualUri = Uri.parse("file://${targetFile.absolutePath}")
                ErrorHandler.showInfo("EncryptedAttachmentStore: Создан ручной URI")
                return manualUri
            }
            
            newUri
        } catch (e: Exception) {
            AppLogger.log("EncryptedAttachmentStore", "ERROR: Failed to persist file: ${e.message}")
            AppLogger.log("EncryptedAttachmentStore", "ERROR: Original URI was: $uri")
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
            ErrorHandler.showInfo("EncryptedAttachmentStore: Получаем файл")
            ErrorHandler.showInfo("EncryptedAttachmentStore: URI схема: ${uri.scheme}")
            
            // Проверяем, является ли URI файлом в нашей папке
            val filePath = uri.path
            if (filePath != null) {
                val file = File(filePath)
                ErrorHandler.showInfo("EncryptedAttachmentStore: Путь к файлу: ${file.absolutePath}")
                ErrorHandler.showInfo("EncryptedAttachmentStore: Файл существует: ${file.exists()}")
                
                if (file.exists() && file.isFile) {
                    ErrorHandler.showInfo("EncryptedAttachmentStore: Файл найден локально")
                    file.inputStream()
                } else {
                    ErrorHandler.showInfo("EncryptedAttachmentStore: Файл не найден локально, пробуем ContentResolver")
                    // Если файл не найден, пытаемся открыть через ContentResolver
                    // Это для совместимости со старыми URI
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        ErrorHandler.showInfo("EncryptedAttachmentStore: Файл открыт через ContentResolver")
                    } else {
                        ErrorHandler.showWarning("EncryptedAttachmentStore: Файл не найден через ContentResolver")
                    }
                    inputStream
                }
            } else {
                ErrorHandler.showInfo("EncryptedAttachmentStore: URI path null, пробуем ContentResolver")
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    ErrorHandler.showInfo("EncryptedAttachmentStore: Файл открыт через ContentResolver")
                } else {
                    ErrorHandler.showWarning("EncryptedAttachmentStore: Файл не найден через ContentResolver")
                }
                inputStream
            }
        } catch (e: Exception) {
            ErrorHandler.showError("EncryptedAttachmentStore: Ошибка получения файла: ${e.message}", e)
            null
        }
    }

    fun exists(uri: Uri): Boolean {
        return try {
            ErrorHandler.showInfo("EncryptedAttachmentStore: Проверяем существование файла")
            val filePath = uri.path
            if (filePath != null) {
                val file = File(filePath)
                ErrorHandler.showInfo("EncryptedAttachmentStore: Путь к файлу: ${file.absolutePath}")
                if (file.exists() && file.isFile) {
                    ErrorHandler.showInfo("EncryptedAttachmentStore: Файл существует локально")
                    true
                } else {
                    ErrorHandler.showInfo("EncryptedAttachmentStore: Файл не найден локально")
                    // Проверяем через ContentResolver
                    val exists = try {
                        context.contentResolver.openInputStream(uri)?.use { true } ?: false
                    } catch (e: Exception) {
                        ErrorHandler.showWarning("EncryptedAttachmentStore: Файл не найден через ContentResolver")
                        false
                    }
                    ErrorHandler.showInfo("EncryptedAttachmentStore: Файл существует через ContentResolver: $exists")
                    exists
                }
            } else {
                ErrorHandler.showInfo("EncryptedAttachmentStore: URI path null, проверяем через ContentResolver")
                val exists = try {
                    context.contentResolver.openInputStream(uri)?.use { true } ?: false
                } catch (e: Exception) {
                    ErrorHandler.showWarning("EncryptedAttachmentStore: Файл не найден через ContentResolver")
                    false
                }
                ErrorHandler.showInfo("EncryptedAttachmentStore: Файл существует через ContentResolver: $exists")
                exists
            }
        } catch (e: Exception) {
            ErrorHandler.showError("EncryptedAttachmentStore: Ошибка проверки существования файла: ${e.message}", e)
            false
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
