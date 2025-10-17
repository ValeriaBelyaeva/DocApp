package com.example.docapp.domain.usecase

import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.domain.repo.AttachmentRepository
import com.example.docapp.data.storage.FileGc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CleanupOrphansUseCase(
    private val attachmentRepository: AttachmentRepository
) {
    
    suspend operator fun invoke(): FileGc.CleanupResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("CleanupOrphansUseCase", "Starting orphan cleanup...")
            ErrorHandler.showInfo("Очистка неиспользуемых файлов...")
            
            val result = attachmentRepository.cleanupOrphans()
            
            AppLogger.log("CleanupOrphansUseCase", "Cleanup completed: $result")
            
            when {
                result.errors == 0 && result.deletedFiles > 0 -> {
                    ErrorHandler.showSuccess("Очистка завершена: удалено ${result.deletedFiles} файлов")
                }
                result.errors == 0 && result.deletedFiles == 0 -> {
                    ErrorHandler.showInfo("Неиспользуемые файлы не найдены")
                }
                else -> {
                    ErrorHandler.showWarning("Очистка завершена с ошибками: ${result.deletedFiles} файлов, ${result.errors} ошибок")
                }
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("CleanupOrphansUseCase", "ERROR: Cleanup failed: ${e.message}")
            ErrorHandler.showError("Ошибка при очистке файлов: ${e.message}")
            throw e
        }
    }
}
