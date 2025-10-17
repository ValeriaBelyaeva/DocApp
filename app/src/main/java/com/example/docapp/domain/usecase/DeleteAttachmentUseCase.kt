package com.example.docapp.domain.usecase

import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.domain.repo.AttachmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteAttachmentUseCase(
    private val attachmentRepository: AttachmentRepository
) {
    
    suspend operator fun invoke(attachmentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("DeleteAttachmentUseCase", "Deleting attachment: $attachmentId")
            
            val result = attachmentRepository.deleteAttachment(attachmentId)
            
            if (result) {
                AppLogger.log("DeleteAttachmentUseCase", "Attachment deleted successfully: $attachmentId")
                ErrorHandler.showSuccess("Файл удален")
            } else {
                AppLogger.log("DeleteAttachmentUseCase", "Failed to delete attachment: $attachmentId")
                ErrorHandler.showWarning("Не удалось удалить файл")
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("DeleteAttachmentUseCase", "ERROR: Failed to delete attachment: ${e.message}")
            ErrorHandler.showError("Ошибка при удалении файла: ${e.message}")
            false
        }
    }
}
