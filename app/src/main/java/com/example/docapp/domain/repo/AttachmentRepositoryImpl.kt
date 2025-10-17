package com.example.docapp.domain.repo

import android.content.Context
import android.net.Uri
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.newId
import com.example.docapp.data.db.dao.AttachmentDao
import com.example.docapp.data.db.entities.AttachmentEntity
import com.example.docapp.data.storage.AttachStorage
import com.example.docapp.data.storage.FileGc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

class AttachmentRepositoryImpl(
    private val attachmentDao: AttachmentDao,
    private val attachStorage: AttachStorage,
    private val fileGc: FileGc
) : AttachmentRepository {
    
    override suspend fun importAttachment(context: Context, uri: Uri): AttachmentEntity = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("AttachmentRepository", "Importing attachment from URI: $uri")
            
            // Импортируем файл в хранилище
            val imported = attachStorage.importFromUri(context, uri)
            
            // Создаем сущность для БД
            val attachment = AttachmentEntity(
                id = newId(),
                docId = null, // Пока не привязан к документу
                name = imported.name,
                mime = imported.mime,
                size = imported.size,
                sha256 = imported.sha256,
                path = imported.absPath,
                uri = imported.contentUri.toString(),
                createdAt = System.currentTimeMillis()
            )
            
            // Сохраняем в БД
            attachmentDao.insert(attachment)
            
            AppLogger.log("AttachmentRepository", "Attachment imported successfully: ${attachment.name}")
            ErrorHandler.showSuccess("Файл импортирован: ${attachment.name}")
            
            attachment
            
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to import attachment: ${e.message}")
            ErrorHandler.showError("Не удалось импортировать файл: ${e.message}")
            throw e
        }
    }
    
    override suspend fun importAttachments(context: Context, uris: List<Uri>): List<AttachmentEntity> = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("AttachmentRepository", "Importing ${uris.size} attachments")
            ErrorHandler.showInfo("Импорт ${uris.size} файлов...")
            
            val imported = mutableListOf<AttachmentEntity>()
            var successCount = 0
            var errorCount = 0
            
            uris.forEachIndexed { index, uri ->
                // Проверяем отмену операции
                if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                    AppLogger.log("AttachmentRepository", "Import cancelled by user")
                    ErrorHandler.showInfo("Импорт отменен")
                    return@withContext imported
                }
                
                try {
                    // Импортируем файл напрямую, без вызова importAttachment (чтобы избежать двойного логирования)
                    val importedFile = attachStorage.importFromUri(context, uri)
                    
                    val attachment = AttachmentEntity(
                        id = com.example.docapp.core.newId(),
                        docId = null,
                        name = importedFile.name,
                        mime = importedFile.mime,
                        size = importedFile.size,
                        sha256 = importedFile.sha256,
                        path = importedFile.absPath,
                        uri = importedFile.contentUri.toString(),
                        createdAt = System.currentTimeMillis()
                    )
                    
                    attachmentDao.insert(attachment)
                    imported.add(attachment)
                    successCount++
                    
                    AppLogger.log("AttachmentRepository", "Imported $index/${uris.size}: ${attachment.name}")
                    
                } catch (e: Exception) {
                    errorCount++
                    AppLogger.log("AttachmentRepository", "Failed to import attachment $index: ${e.message}")
                    ErrorHandler.showWarning("Ошибка импорта файла $index: ${e.message}")
                }
            }
            
            AppLogger.log("AttachmentRepository", "Import completed: $successCount success, $errorCount errors")
            
            if (errorCount == 0) {
                ErrorHandler.showSuccess("Все файлы импортированы успешно")
            } else {
                ErrorHandler.showWarning("Импорт завершен: $successCount успешно, $errorCount ошибок")
            }
            
            imported
            
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Batch import failed: ${e.message}")
            ErrorHandler.showError("Ошибка массового импорта: ${e.message}")
            throw e
        }
    }
    
    override suspend fun getAttachmentsByDoc(docId: String): List<AttachmentEntity> = withContext(Dispatchers.IO) {
        try {
            attachmentDao.listByDoc(docId)
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to get attachments for doc $docId: ${e.message}")
            emptyList()
        }
    }
    
    override fun observeAttachmentsByDoc(docId: String): Flow<List<AttachmentEntity>> {
        return attachmentDao.observeByDoc(docId)
    }
    
    override suspend fun deleteAttachment(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("AttachmentRepository", "Deleting attachment: $id")
            
            // Получаем информацию о вложении
            val attachment = attachmentDao.getById(id)
            if (attachment == null) {
                AppLogger.log("AttachmentRepository", "Attachment not found: $id")
                return@withContext false
            }
            
            // Удаляем физический файл
            val fileDeleted = attachStorage.deletePhysical(attachment)
            
            // Удаляем запись из БД
            attachmentDao.deleteById(id)
            
            AppLogger.log("AttachmentRepository", "Attachment deleted: ${attachment.name}")
            ErrorHandler.showSuccess("Файл удален: ${attachment.name}")
            
            fileDeleted
            
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to delete attachment $id: ${e.message}")
            ErrorHandler.showError("Не удалось удалить файл: ${e.message}")
            false
        }
    }
    
    override suspend fun deleteAttachmentsByDoc(docId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("AttachmentRepository", "Deleting attachments for document: $docId")
            
            val result = fileGc.cleanupDocumentAttachments(docId)
            
            AppLogger.log("AttachmentRepository", "Document attachments cleanup: $result")
            result.errors == 0
            
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to delete attachments for doc $docId: ${e.message}")
            ErrorHandler.showError("Не удалось удалить вложения документа: ${e.message}")
            false
        }
    }
    
    override suspend fun bindAttachmentsToDoc(attachmentIds: List<String>, docId: String) = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("AttachmentRepository", "Binding ${attachmentIds.size} attachments to document: $docId")
            
            attachmentDao.bindToDoc(attachmentIds, docId)
            
            AppLogger.log("AttachmentRepository", "Attachments bound successfully")
            ErrorHandler.showSuccess("Вложения привязаны к документу")
            
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to bind attachments: ${e.message}")
            ErrorHandler.showError("Не удалось привязать вложения: ${e.message}")
            throw e
        }
    }
    
    override suspend fun getAttachment(id: String): AttachmentEntity? = withContext(Dispatchers.IO) {
        try {
            attachmentDao.getById(id)
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to get attachment $id: ${e.message}")
            null
        }
    }
    
    override suspend fun findDuplicates(sha256: String): List<AttachmentEntity> = withContext(Dispatchers.IO) {
        try {
            attachmentDao.findBySha256(sha256)
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to find duplicates for $sha256: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun cleanupOrphans(): FileGc.CleanupResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("AttachmentRepository", "Starting orphan cleanup...")
            
            val result = fileGc.cleanupOrphans()
            
            AppLogger.log("AttachmentRepository", "Orphan cleanup completed: $result")
            result
            
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Orphan cleanup failed: ${e.message}")
            ErrorHandler.showError("Ошибка очистки неиспользуемых файлов: ${e.message}")
            throw e
        }
    }
    
    override suspend fun getAttachmentInputStream(context: Context, attachment: AttachmentEntity): java.io.InputStream? = withContext(Dispatchers.IO) {
        try {
            attachStorage.openForRead(context, attachment)
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to open attachment stream: ${e.message}")
            null
        }
    }
    
    override suspend fun getAttachmentFile(attachment: AttachmentEntity): java.io.File? = withContext(Dispatchers.IO) {
        try {
            val file = attachStorage.fileFor(attachment)
            if (file.exists()) file else null
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to get attachment file: ${e.message}")
            null
        }
    }
    
    override suspend fun validateAttachmentIntegrity(attachment: AttachmentEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // Проверяем существование файла
            if (!attachStorage.exists(attachment)) {
                AppLogger.log("AttachmentRepository", "Attachment file not found: ${attachment.path}")
                false
            } else {
                // Здесь можно добавить проверку SHA256, но пока просто проверяем существование
                true
            }
            
        } catch (e: Exception) {
            AppLogger.log("AttachmentRepository", "ERROR: Failed to validate attachment: ${e.message}")
            false
        }
    }
}
