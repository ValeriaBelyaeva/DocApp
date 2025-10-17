package com.example.docapp.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.domain.repo.AttachmentRepository
import com.example.docapp.data.db.entities.AttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportAttachmentsUseCase(
    private val attachmentRepository: AttachmentRepository
) {
    
    suspend operator fun invoke(
        context: Context,
        docId: String?,
        uris: List<Uri>
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("ImportAttachmentsUseCase", "Importing ${uris.size} attachments for doc: $docId")
            ErrorHandler.showInfo("Импорт ${uris.size} файлов...")
            
            val imported = attachmentRepository.importAttachments(context, uris)
            
            // Если указан docId, привязываем вложения к документу
            if (docId != null && imported.isNotEmpty()) {
                val attachmentIds = imported.map { it.id }
                attachmentRepository.bindAttachmentsToDoc(attachmentIds, docId)
                AppLogger.log("ImportAttachmentsUseCase", "Bound ${attachmentIds.size} attachments to document: $docId")
            }
            
            val result = ImportResult(
                successful = imported.size,
                failed = uris.size - imported.size,
                attachments = imported
            )
            
            AppLogger.log("ImportAttachmentsUseCase", "Import completed: $result")
            
            if (result.failed == 0) {
                ErrorHandler.showSuccess("Все файлы импортированы успешно")
            } else {
                ErrorHandler.showWarning("Импорт завершен: ${result.successful} успешно, ${result.failed} ошибок")
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("ImportAttachmentsUseCase", "ERROR: Import failed: ${e.message}")
            ErrorHandler.showError("Ошибка импорта файлов: ${e.message}")
            throw e
        }
    }
    
    data class ImportResult(
        val successful: Int,
        val failed: Int,
        val attachments: List<AttachmentEntity>
    )
}
