package com.example.docapp.data.storage

import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.data.db.dao.AttachmentDao
import com.example.docapp.data.db.entities.AttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileGc(
    private val attachmentDao: AttachmentDao,
    private val attachStorage: AttachStorage
) {
    
    suspend fun cleanupOrphans(): CleanupResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("FileGc", "Starting orphan cleanup...")
            ErrorHandler.showInfo("Очистка неиспользуемых файлов...")
            
            val orphans = attachmentDao.listOrphans()
            AppLogger.log("FileGc", "Found ${orphans.size} orphan attachments")
            
            if (orphans.isEmpty()) {
                AppLogger.log("FileGc", "No orphans found")
                ErrorHandler.showInfo("Неиспользуемые файлы не найдены")
                return@withContext CleanupResult(0, 0, 0)
            }
            
            var deletedFiles = 0
            var deletedRecords = 0
            var errors = 0
            
            orphans.forEach { orphan ->
                try {
                    // Удаляем физический файл
                    val fileDeleted = attachStorage.deletePhysical(orphan)
                    
                    if (fileDeleted) {
                        deletedFiles++
                        
                        // Удаляем запись из БД только если физический файл удален успешно
                        attachmentDao.deleteById(orphan.id)
                        deletedRecords++
                        
                        AppLogger.log("FileGc", "Cleaned up orphan: ${orphan.name}")
                    } else {
                        errors++
                        AppLogger.log("FileGc", "Failed to delete physical file: ${orphan.path}")
                        // Не удаляем запись из БД, если файл не удалился
                    }
                    
                } catch (e: Exception) {
                    errors++
                    AppLogger.log("FileGc", "ERROR: Failed to cleanup orphan ${orphan.id}: ${e.message}")
                    ErrorHandler.showWarning("Ошибка при удалении файла ${orphan.name}: ${e.message}")
                    // Не удаляем запись из БД при исключении
                }
            }
            
            val result = CleanupResult(deletedFiles, deletedRecords, errors)
            AppLogger.log("FileGc", "Cleanup completed: $result")
            
            if (errors == 0) {
                ErrorHandler.showSuccess("Очистка завершена: удалено $deletedFiles файлов")
            } else {
                ErrorHandler.showWarning("Очистка завершена с ошибками: $deletedFiles файлов, $errors ошибок")
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("FileGc", "ERROR: Cleanup failed: ${e.message}")
            ErrorHandler.showError("Ошибка при очистке файлов: ${e.message}")
            throw e
        }
    }
    
    suspend fun cleanupDocumentAttachments(docId: String): CleanupResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("FileGc", "Cleaning up attachments for document: $docId")
            
            val attachments = attachmentDao.listByDoc(docId)
            var deletedFiles = 0
            var deletedRecords = 0
            var errors = 0
            
            attachments.forEach { attachment ->
                try {
                    // Удаляем физический файл
                    val fileDeleted = attachStorage.deletePhysical(attachment)
                    
                    if (fileDeleted) {
                        deletedFiles++
                        // Удаляем запись из БД только если физический файл удален успешно
                        attachmentDao.deleteById(attachment.id)
                        deletedRecords++
                        AppLogger.log("FileGc", "Cleaned up attachment: ${attachment.name}")
                    } else {
                        errors++
                        AppLogger.log("FileGc", "Failed to delete physical file: ${attachment.path}")
                        // Не удаляем запись из БД, если файл не удалился
                    }
                    
                } catch (e: Exception) {
                    errors++
                    AppLogger.log("FileGc", "ERROR: Failed to cleanup attachment ${attachment.id}: ${e.message}")
                    ErrorHandler.showWarning("Ошибка при удалении вложения ${attachment.name}: ${e.message}")
                    // Не удаляем запись из БД при исключении
                }
            }
            
            AppLogger.log("FileGc", "Document cleanup completed: $deletedFiles files, $errors errors")
            CleanupResult(deletedFiles, deletedRecords, errors)
            
        } catch (e: Exception) {
            AppLogger.log("FileGc", "ERROR: Document cleanup failed: ${e.message}")
            throw e
        }
    }
    
    suspend fun validateIntegrity(): IntegrityReport = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("FileGc", "Starting integrity validation...")
            
            // Получаем все вложения
            val allAttachments = mutableListOf<AttachmentEntity>()
            // Здесь нужно получить все вложения, но у нас нет метода getAll()
            // Пока что проверяем только по документам
            
            val orphanAttachments = attachmentDao.listOrphans()
            val missingFiles = mutableListOf<AttachmentEntity>()
            val corruptedFiles = mutableListOf<AttachmentEntity>()
            
            // Проверяем сироты
            orphanAttachments.forEach { orphan ->
                if (!attachStorage.exists(orphan)) {
                    missingFiles.add(orphan)
                }
            }
            
            val report = IntegrityReport(
                totalAttachments = orphanAttachments.size,
                missingFiles = missingFiles.size,
                corruptedFiles = corruptedFiles.size,
                orphanAttachments = orphanAttachments.size
            )
            
            AppLogger.log("FileGc", "Integrity validation completed: $report")
            report
            
        } catch (e: Exception) {
            AppLogger.log("FileGc", "ERROR: Integrity validation failed: ${e.message}")
            throw e
        }
    }
    
    data class CleanupResult(
        val deletedFiles: Int,
        val deletedRecords: Int,
        val errors: Int
    )
    
    data class IntegrityReport(
        val totalAttachments: Int,
        val missingFiles: Int,
        val corruptedFiles: Int,
        val orphanAttachments: Int
    )
}
