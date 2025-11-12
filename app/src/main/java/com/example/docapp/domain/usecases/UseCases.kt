package com.example.docapp.domain.usecases

import android.content.Context
import android.net.Uri
import com.example.docapp.domain.Attachment
import com.example.docapp.domain.DocumentRepository
import com.example.docapp.domain.Repositories
import com.example.docapp.core.AttachmentStore
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.UriDebugger
import com.example.docapp.core.AppLogger
import com.example.docapp.data.storage.FileGc
import com.example.docapp.data.db.entities.AttachmentEntity

class UseCases(
    private val repos: Repositories,
    private val attachmentStore: AttachmentStore,
    private val documentDao: com.example.docapp.data.DocumentDao
) {
    
    // Attachment UseCases (inline implementation)
    suspend fun importAttachments(context: Context, docId: String?, uris: List<Uri>): ImportResult {
        return try {
            AppLogger.log("UseCases", "Importing ${uris.size} attachments for doc: $docId")
            ErrorHandler.showInfo("Importing ${uris.size} files...")
            
            val imported = repos.attachments.importAttachments(context, uris)
            
            // Если указан docId, привязываем вложения к документу
            if (docId != null && imported.isNotEmpty()) {
                val attachmentIds = imported.map { it.id }
                repos.attachments.bindAttachmentsToDoc(attachmentIds, docId)
                AppLogger.log("UseCases", "Bound ${attachmentIds.size} attachments to document: $docId")
            }
            
            val result = ImportResult(
                successful = imported.size,
                failed = uris.size - imported.size,
                attachments = imported
            )
            
            AppLogger.log("UseCases", "Import completed: $result")
            
            if (result.failed == 0) {
                ErrorHandler.showSuccess("All files imported successfully")
            } else {
                ErrorHandler.showWarning("Import finished: ${result.successful} succeeded, ${result.failed} failed")
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("UseCases", "ERROR: Import failed: ${e.message}")
            ErrorHandler.showError("File import failed: ${e.message}")
            throw e
        }
    }
    
    suspend fun deleteAttachment(attachmentId: String): Boolean {
        return try {
            AppLogger.log("UseCases", "Deleting attachment: $attachmentId")
            
            val result = repos.attachments.deleteAttachment(attachmentId)
            
            if (result) {
                AppLogger.log("UseCases", "Attachment deleted successfully: $attachmentId")
                ErrorHandler.showSuccess("File deleted")
            } else {
                AppLogger.log("UseCases", "Failed to delete attachment: $attachmentId")
                ErrorHandler.showWarning("Unable to delete file")
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("UseCases", "ERROR: Failed to delete attachment: ${e.message}")
            ErrorHandler.showError("Failed to delete file: ${e.message}")
            false
        }
    }
    
    suspend fun cleanupOrphans(): FileGc.CleanupResult {
        return try {
            AppLogger.log("UseCases", "Starting orphan cleanup...")
            ErrorHandler.showInfo("Cleaning up unused files...")
            
            val result = repos.attachments.cleanupOrphans()
            
            AppLogger.log("UseCases", "Cleanup completed: $result")
            
            when {
                result.errors == 0 && result.deletedFiles > 0 -> {
                    ErrorHandler.showSuccess("Cleanup complete: ${result.deletedFiles} files removed")
                }
                result.errors == 0 && result.deletedFiles == 0 -> {
                    ErrorHandler.showInfo("No unused files found")
                }
                else -> {
                    ErrorHandler.showWarning("Cleanup finished with issues: ${result.deletedFiles} files, ${result.errors} errors")
                }
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("UseCases", "ERROR: Cleanup failed: ${e.message}")
            ErrorHandler.showError("File cleanup failed: ${e.message}")
            throw e
        }
    }
    
    suspend fun migrateExternalUris(context: Context): MigrationResult {
        return try {
            AppLogger.log("UseCases", "Starting migration of external URIs...")
            ErrorHandler.showInfo("Migrating external URIs...")
            
            var migratedDocuments = 0
            var migratedAttachments = 0
            var errors = 0
            
            // Получаем все документы
            val allDocuments = documentDao.getAllDocumentIds()
            
            allDocuments.forEach { docId ->
                try {
                    AppLogger.log("UseCases", "Migrating document: $docId")
                    
                    // Получаем полный документ
                    val fullDoc = documentDao.getFull(docId)
                    if (fullDoc == null) {
                        AppLogger.log("UseCases", "Document not found: $docId")
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
                    
                    AppLogger.log("UseCases", "Migrated document $docId: $photoAttachments photos, $pdfAttachments PDFs")
                    
                } catch (e: Exception) {
                    errors++
                    AppLogger.log("UseCases", "ERROR: Failed to migrate document $docId: ${e.message}")
                    ErrorHandler.showWarning("Document migration error for $docId: ${e.message}")
                }
            }
            
            val result = MigrationResult(
                migratedDocuments = migratedDocuments,
                migratedAttachments = migratedAttachments,
                errors = errors
            )
            
            AppLogger.log("UseCases", "Migration completed: $result")
            
            if (errors == 0) {
                ErrorHandler.showSuccess("Migration complete: $migratedDocuments documents, $migratedAttachments attachments")
            } else {
                ErrorHandler.showWarning("Migration completed with errors: $migratedDocuments documents, $errors errors")
            }
            
            result
            
        } catch (e: Exception) {
            AppLogger.log("UseCases", "ERROR: Migration failed: ${e.message}")
            ErrorHandler.showError("Migration failed: ${e.message}")
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
                AppLogger.log("UseCases", "Migrating $type: $uri")
                
                // Импортируем вложение
                val attachment = repos.attachments.importAttachment(context, uri)
                
                // Привязываем к документу
                repos.attachments.bindAttachmentsToDoc(listOf(attachment.id), docId)
                
                migratedCount++
                
            } catch (e: Exception) {
                AppLogger.log("UseCases", "ERROR: Failed to migrate $type $uri: ${e.message}")
                ErrorHandler.showWarning("Failed to migrate file $uri: ${e.message}")
            }
        }
        
        return migratedCount
    }
    
    // Attachment binding
    suspend fun bindAttachmentsToDoc(attachmentIds: List<String>, docId: String) = 
        repos.attachments.bindAttachmentsToDoc(attachmentIds, docId)
    // PIN
    suspend fun verifyPin(pin: String) = repos.settings.verifyPin(pin)
    suspend fun isPinSet() = repos.settings.isPinSet()
    suspend fun setNewPin(pin: String) = repos.settings.setNewPin(pin)
    suspend fun disablePin() = repos.settings.disablePin()

    // Observers
    fun observeHome() = repos.documents.observeHome()
    fun observeTree() = repos.folders.observeTree()

    // Templates
    suspend fun listTemplates() = repos.templates.listTemplates()
    suspend fun getTemplate(id: String) = repos.templates.getTemplate(id)
    suspend fun listTemplateFields(id: String) = repos.templates.listFields(id)
    suspend fun addTemplate(name: String, fields: List<String>) =
        repos.templates.addTemplate(name, fields)
    suspend fun deleteTemplate(id: String) = repos.templates.deleteTemplate(id)

    // Documents
    suspend fun createDoc(
        tplId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photos: List<String>,
        pdfUris: List<String>
    ) = repos.documents.createDocument(tplId, folderId, name, description, fields, photos, pdfUris)

    suspend fun createDocWithNames(
        tplId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoFiles: List<Pair<Uri, String>>, // URI, displayName
        pdfFiles: List<Pair<Uri, String>> // URI, displayName
    ) = repos.documents.createDocumentWithNames(tplId, folderId, name, description, fields, photoFiles, pdfFiles)

    suspend fun getDoc(id: String) = repos.documents.getDocument(id)

    suspend fun updateDoc(fd: DocumentRepository.FullDocument) {
        ErrorHandler.showInfo("UseCases: Updating document with ${fd.photos.size} photos and ${fd.pdfs.size} PDFs")
        
        val preparedAttachments = buildList {
            addAll(fd.photos)
            addAll(fd.pdfs)
        }.map { attachment ->
            UriDebugger.showUriDebug("ATTACHMENT: ${attachment.displayName}", attachment.uri)
            
            if (attachment.requiresPersist) {
                UriDebugger.showUriDebug("SAVE: ${attachment.displayName}", attachment.uri)
                val persistedUri = attachmentStore.persist(attachment.uri)
                UriDebugger.showUriSuccess("SAVED: ${attachment.displayName}", persistedUri)
                attachment.copy(
                    uri = persistedUri,
                    createdAt = if (attachment.createdAt <= 0L) System.currentTimeMillis() else attachment.createdAt,
                    requiresPersist = false
                )
            } else {
                UriDebugger.showUriSuccess("ALREADY SAVED: ${attachment.displayName}", attachment.uri)
                attachment
            }
        }

        repos.documents.updateDocument(fd.doc, fd.fields, preparedAttachments)
    }

    suspend fun deleteDoc(id: String) = repos.documents.deleteDocument(id)
    suspend fun pinDoc(id: String, pinned: Boolean) = repos.documents.pinDocument(id, pinned)
    suspend fun touchOpened(id: String) = repos.documents.touchOpened(id)

    // Folders
    suspend fun listFolders() = repos.folders.listAll()
    suspend fun moveDocToFolder(docId: String, folderId: String?) = repos.documents.moveToFolder(docId, folderId)
    suspend fun getDocumentsInFolder(folderId: String) = repos.documents.getDocumentsInFolder(folderId)
    suspend fun deleteFolder(folderId: String, deleteDocuments: Boolean) {
        if (deleteDocuments) {
            val documentsInFolder = getDocumentsInFolder(folderId)
            documentsInFolder.forEach { doc ->
                deleteDoc(doc.id)
            }
        } else {
            // Move documents to "no folder" (null folderId)
            val documentsInFolder = getDocumentsInFolder(folderId)
            documentsInFolder.forEach { doc ->
                moveDocToFolder(doc.id, null)
            }
        }
        repos.folders.deleteFolder(folderId)
    }

    // Pinned reorder
    suspend fun swapPinned(aId: String, bId: String) = repos.documents.swapPinned(aId, bId)
}

// Data classes for attachment operations
data class ImportResult(
    val successful: Int,
    val failed: Int,
    val attachments: List<AttachmentEntity>
)

data class MigrationResult(
    val migratedDocuments: Int,
    val migratedAttachments: Int,
    val errors: Int
)