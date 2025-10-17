package com.example.docapp.data.storage

import android.content.Context
import android.net.Uri
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.data.db.entities.AttachmentEntity
import java.io.*
import java.security.MessageDigest
import java.util.*

interface AttachStorage {
    data class Imported(
        val name: String, 
        val mime: String, 
        val size: Long,
        val sha256: String, 
        val contentUri: Uri, 
        val absPath: String
    )
    
    suspend fun importFromUri(ctx: Context, inUri: Uri): Imported
    fun openForRead(ctx: Context, attachment: AttachmentEntity): InputStream
    fun deletePhysical(attachment: AttachmentEntity): Boolean
    fun fileFor(attachment: AttachmentEntity): File
    fun exists(attachment: AttachmentEntity): Boolean
}

class AttachStorageImpl(private val context: Context) : AttachStorage {
    
    private val attachmentsDir = File(context.filesDir, "attachments")
    private val bufferSize = 8192 // 8 KiB buffer
    
    init {
        // Создаем директорию для вложений
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
            AppLogger.log("AttachStorage", "Created attachments directory: ${attachmentsDir.absolutePath}")
        }
    }
    
    override suspend fun importFromUri(ctx: Context, inUri: Uri): AttachStorage.Imported {
        try {
            AppLogger.log("AttachStorage", "Importing file from URI: $inUri")
            
            // Получаем информацию о файле
            val displayName = getDisplayName(ctx, inUri)
            val mimeType = getMimeType(ctx, inUri)
            val size = getFileSize(ctx, inUri)
            
            AppLogger.log("AttachStorage", "File info - name: $displayName, mime: $mimeType, size: $size")
            
            // Создаем уникальное имя файла с синхронизацией
            val fileName = synchronized(attachmentsDir) {
                generateUniqueFileName(displayName)
            }
            val file = File(attachmentsDir, fileName)
            
            // Копируем файл
            val sha256 = copyFileWithHash(ctx, inUri, file)
            
            // Создаем content URI через FileProvider
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                ctx, 
                "${ctx.packageName}.fileprovider", 
                file
            )
            
            AppLogger.log("AttachStorage", "File imported successfully: $fileName")
            ErrorHandler.showSuccess("Файл импортирован: $displayName")
            
            return AttachStorage.Imported(
                name = displayName,
                mime = mimeType,
                size = size,
                sha256 = sha256,
                contentUri = contentUri,
                absPath = file.absolutePath
            )
            
        } catch (e: Exception) {
            AppLogger.log("AttachStorage", "ERROR: Failed to import file: ${e.message}")
            ErrorHandler.showError("Не удалось импортировать файл: ${e.message}")
            throw e
        }
    }
    
    override fun openForRead(ctx: Context, attachment: AttachmentEntity): InputStream {
        val file = File(attachment.path)
        if (!file.exists()) {
            throw FileNotFoundException("Attachment file not found: ${attachment.path}")
        }
        // Проверяем, включено ли шифрование
        return if (AttachmentCrypto.encryptionEnabled) {
            val crypto = AttachmentCrypto(ctx)
            val encryptedFile = crypto.createEncryptedFile(file)
            if (encryptedFile != null) {
                crypto.readFromEncryptedFile(encryptedFile) ?: FileInputStream(file)
            } else {
                FileInputStream(file)
            }
        } else {
            FileInputStream(file)
        }
    }
    
    override fun deletePhysical(attachment: AttachmentEntity): Boolean {
        return try {
            val file = File(attachment.path)
            val deleted = file.delete()
            if (deleted) {
                AppLogger.log("AttachStorage", "Physical file deleted: ${attachment.path}")
            } else {
                AppLogger.log("AttachStorage", "Failed to delete physical file: ${attachment.path}")
            }
            deleted
        } catch (e: Exception) {
            AppLogger.log("AttachStorage", "ERROR: Exception deleting file: ${e.message}")
            false
        }
    }
    
    override fun fileFor(attachment: AttachmentEntity): File {
        return File(attachment.path)
    }
    
    override fun exists(attachment: AttachmentEntity): Boolean {
        return File(attachment.path).exists()
    }
    
    private fun getDisplayName(ctx: Context, uri: Uri): String {
        return try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment ?: "unknown_file"
        } catch (e: Exception) {
            AppLogger.log("AttachStorage", "Failed to get display name: ${e.message}")
            uri.lastPathSegment ?: "unknown_file"
        }
    }
    
    private fun getMimeType(ctx: Context, uri: Uri): String {
        return try {
            ctx.contentResolver.getType(uri) ?: "application/octet-stream"
        } catch (e: Exception) {
            AppLogger.log("AttachStorage", "Failed to get MIME type: ${e.message}")
            "application/octet-stream"
        }
    }
    
    private fun getFileSize(ctx: Context, uri: Uri): Long {
        return try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else -1L
            } ?: -1L
        } catch (e: Exception) {
            AppLogger.log("AttachStorage", "Failed to get file size: ${e.message}")
            -1L
        }
    }
    
    fun generateUniqueFileName(originalName: String): String {
        val baseName = originalName.substringBeforeLast(".")
        val extension = originalName.substringAfterLast(".", "")
        val ext = if (extension.isNotEmpty()) ".$extension" else ""
        
        var fileName = "$baseName$ext"
        var counter = 1
        val maxAttempts = 1000 // Защита от бесконечного цикла
        
        while (File(attachmentsDir, fileName).exists() && counter <= maxAttempts) {
            fileName = "$baseName($counter)$ext"
            counter++
        }
        
        if (counter > maxAttempts) {
            // Если не удалось найти уникальное имя, добавляем timestamp
            fileName = "${baseName}_${System.currentTimeMillis()}$ext"
        }
        
        return fileName
    }
    
    private suspend fun copyFileWithHash(ctx: Context, sourceUri: Uri, targetFile: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(bufferSize)
        
        ctx.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            // Проверяем, включено ли шифрование
            if (AttachmentCrypto.encryptionEnabled) {
                val crypto = AttachmentCrypto(ctx)
                val encryptedFile = crypto.createEncryptedFile(targetFile)
                if (encryptedFile != null) {
                    crypto.writeToEncryptedFile(encryptedFile, inputStream)
                    // Для зашифрованных файлов нужно пересчитать хеш
                    return FileInputStream(targetFile).use { fis ->
                        val digest2 = MessageDigest.getInstance("SHA-256")
                        val buffer2 = ByteArray(bufferSize)
                        var bytesRead: Int
                        while (fis.read(buffer2).also { bytesRead = it } != -1) {
                            digest2.update(buffer2, 0, bytesRead)
                        }
                        digest2.digest().joinToString("") { "%02x".format(it) }
                    }
                }
            }
            
            // Обычное копирование без шифрования
            FileOutputStream(targetFile).use { outputStream ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                }
            }
        } ?: throw IOException("Cannot open input stream for URI: $sourceUri")
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
