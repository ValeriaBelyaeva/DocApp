package com.example.docapp.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.domain.repo.AttachmentRepository
import com.example.docapp.data.DocumentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MigrateExternalUrisUseCase(
    private val attachmentRepository: AttachmentRepository,
    private val documentDao: DocumentDao
) {
    
    suspend operator fun invoke(context: Context): MigrationResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("MigrateExternalUrisUseCase", "Starting migration of external URIs...")
            ErrorHandler.showInfo("Миграция внешних URI...")
            
            var migratedDocuments = 0
            var migratedAttachments = 0
            var errors = 0
            
            // Получаем все документы (нужно добавить метод в DocumentDao)
            // Пока что делаем заглушку
            val allDocuments = getAllDocuments()
            
            allDocuments.forEach { docId ->
                try {
                    AppLogger.log("MigrateExternalUrisUseCase", "Migrating document: $docId")
                    
                    // Получаем полный документ
                    val fullDoc = documentDao.getFull(docId)
                    if (fullDoc == null) {
                        AppLogger.log("MigrateExternalUrisUseCase", "Document not found: $docId")
                        return@forEach
                    }
                    
                    // Мигрируем фото
                    val photoAttachments = migrateAttachments(
                        context,
                        fullDoc.photos.map { it.uri },
                        docId,
                        "photo"
                    )
                    
                    // Мигрируем PDF
                    val pdfAttachments = migrateAttachments(
                        context,
                        fullDoc.pdfs.map { it.uri },
                        docId,
                        "pdf"
                    )
                    
                    migratedAttachments += photoAttachments + pdfAttachments
                    migratedDocuments++
                    
                    AppLogger.log("MigrateExternalUrisUseCase", "Migrated document $docId: $photoAttachments photos, $pdfAttachments PDFs")
                    
                } catch (e: Exception) {
                    errors++
                    AppLogger.log("MigrateExternalUrisUseCase", "ERROR: Failed to migrate document $docId: ${e.message}")
                    ErrorHandler.showWarning("Ошибка миграции документа $docId: ${e.message}")
                }
            }
            
            val result = MigrationResult(
                migratedDocuments = migratedDocuments,
                migratedAttachments = migratedAttachments,
                errors = errors
            )
            
            AppLogger.log("MigrateExternalUrisUseCase", "Migration completed: $result")
            
            if (errors == 0) {
                ErrorHandler.showSuccess("Миграция завершена: $migratedDocuments документов, $migratedAttachments вложений")
            } else {
                ErrorHandler.showWarning("Миграция завершена с ошибками: $migratedDocuments документов, $errors ошибок")
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("MigrateExternalUrisUseCase", "ERROR: Migration failed: ${e.message}")
            ErrorHandler.showError("Ошибка миграции: ${e.message}")
            throw e
        }
    }
    
    private suspend fun migrateAttachments(
        context: Context,
        uris: List<Uri>,
        docId: String,
        type: String
    ): Int {
        var migratedCount = 0
        
        uris.forEach { uri ->
            try {
                AppLogger.log("MigrateExternalUrisUseCase", "Migrating $type: $uri")
                
                // Импортируем вложение
                val attachment = attachmentRepository.importAttachment(context, uri)
                
                // Привязываем к документу
                attachmentRepository.bindAttachmentsToDoc(listOf(attachment.id), docId)
                
                migratedCount++
                
            } catch (e: Exception) {
                AppLogger.log("MigrateExternalUrisUseCase", "ERROR: Failed to migrate $type $uri: ${e.message}")
                ErrorHandler.showWarning("Ошибка миграции файла $uri: ${e.message}")
            }
        }
        
        return migratedCount
    }
    
    private suspend fun getAllDocuments(): List<String> {
        return documentDao.getAllDocumentIds()
    }
    
    data class MigrationResult(
        val migratedDocuments: Int,
        val migratedAttachments: Int,
        val errors: Int
    )
}
