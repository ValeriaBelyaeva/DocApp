package com.example.docapp.domain.interactors
import android.content.Context
import android.net.Uri
import com.example.docapp.core.AppLogger
import com.example.docapp.core.AttachmentStore
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.UriDebugger
import com.example.docapp.data.db.entities.AttachmentEntity
import com.example.docapp.data.storage.FileGc
import com.example.docapp.domain.repo.AttachmentRepository

/**
 * Interactor class for attachment operations, providing a clean interface to attachment repository.
 * Handles file import, deletion, cleanup, and migration operations.
 * 
 * Works by delegating attachment operations to the repository and attachment store,
 * providing business logic for file management and persistence.
 * 
 * arguments:
 *     repository - AttachmentRepository: The attachment repository to delegate operations to
 *     attachmentStore - AttachmentStore: Service for managing attachment file storage
 */
class AttachmentInteractors(
    private val repository: AttachmentRepository,
    private val attachmentStore: AttachmentStore
) {
    /**
     * Data class representing the result of an attachment import operation.
     * 
     * arguments:
     *     successful - Int: Number of successfully imported attachments
     *     failed - Int: Number of failed import attempts
     *     attachments - List<AttachmentEntity>: List of successfully imported attachment entities
     */
    data class ImportResult(
        val successful: Int,
        val failed: Int,
        val attachments: List<AttachmentEntity>
    )
    /**
     * Imports attachments from URIs and optionally binds them to a document.
     * 
     * Works by importing files from URIs through the repository, then binding them to a document
     * if documentId is provided. Returns a result with success/failure counts.
     * 
     * arguments:
     *     context - Context: Android context for accessing content providers
     *     documentId - String?: Optional document ID to bind attachments to, null to import without binding
     *     uris - List<Uri>: List of file URIs to import
     * 
     * return:
     *     result - ImportResult: Result containing success/failure counts and imported attachments
     * 
     * throws:
     *     Exception: If import operation fails
     */
    suspend fun import(
        context: Context,
        documentId: String?,
        uris: List<Uri>
    ): ImportResult {
        return try {
            AppLogger.log("AttachmentInteractors", "Importing ${uris.size} attachments for doc: $documentId")
            ErrorHandler.showInfo("Importing ${uris.size} files...")
            val imported = repository.importAttachments(context, uris)
            if (documentId != null && imported.isNotEmpty()) {
                val attachmentIds = imported.map { it.id }
                repository.bindAttachmentsToDoc(attachmentIds, documentId)
                AppLogger.log(
                    "AttachmentInteractors",
                    "Bound ${attachmentIds.size} attachments to document: $documentId"
                )
            }
            val result = ImportResult(
                successful = imported.size,
                failed = uris.size - imported.size,
                attachments = imported
            )
            AppLogger.log("AttachmentInteractors", "Import completed: $result")
            if (result.failed == 0) {
                ErrorHandler.showSuccess("All files imported successfully")
            } else {
                ErrorHandler.showWarning("Import finished: ${result.successful} succeeded, ${result.failed} failed")
            }
            result
        } catch (e: Exception) {
            AppLogger.log("AttachmentInteractors", "ERROR: Import failed: ${e.message}")
            ErrorHandler.showError("File import failed: ${e.message}")
            throw e
        }
    }
    /**
     * Deletes an attachment by its ID.
     * 
     * Works by calling the repository to delete the attachment, which removes both the database
     * record and the physical file.
     * 
     * arguments:
     *     attachmentId - String: The ID of the attachment to delete
     * 
     * return:
     *     success - Boolean: True if deletion was successful, false otherwise
     */
    suspend fun delete(attachmentId: String): Boolean {
        return try {
            AppLogger.log("AttachmentInteractors", "Deleting attachment: $attachmentId")
            val result = repository.deleteAttachment(attachmentId)
            if (result) {
                AppLogger.log("AttachmentInteractors", "Attachment deleted successfully: $attachmentId")
                ErrorHandler.showSuccess("File deleted")
            } else {
                AppLogger.log("AttachmentInteractors", "Failed to delete attachment: $attachmentId")
                ErrorHandler.showWarning("Unable to delete file")
            }
            result
        } catch (e: Exception) {
            AppLogger.log("AttachmentInteractors", "ERROR: Failed to delete attachment: ${e.message}")
            ErrorHandler.showError("Failed to delete file: ${e.message}")
            false
        }
    }
    /**
     * Cleans up orphaned attachment files that are no longer referenced in the database.
     * 
     * Works by finding files in storage that don't have corresponding database records
     * and deleting them to free up space.
     * 
     * return:
     *     result - FileGc.CleanupResult: Result containing number of deleted files and any errors
     * 
     * throws:
     *     Exception: If cleanup operation fails
     */
    suspend fun cleanupOrphans(): FileGc.CleanupResult {
        return try {
            AppLogger.log("AttachmentInteractors", "Starting orphan cleanup...")
            ErrorHandler.showInfo("Cleaning up unused files...")
            val result = repository.cleanupOrphans()
            AppLogger.log("AttachmentInteractors", "Cleanup completed: $result")
            when {
                result.errors == 0 && result.deletedFiles > 0 -> {
                    ErrorHandler.showSuccess("Cleanup complete: ${result.deletedFiles} files removed")
                }
                result.errors == 0 && result.deletedFiles == 0 -> {
                    ErrorHandler.showInfo("No unused files found")
                }
                else -> {
                    ErrorHandler.showWarning(
                        "Cleanup finished with issues: ${result.deletedFiles} files, ${result.errors} errors"
                    )
                }
            }
            result
        } catch (e: Exception) {
            AppLogger.log("AttachmentInteractors", "ERROR: Cleanup failed: ${e.message}")
            ErrorHandler.showError("File cleanup failed: ${e.message}")
            throw e
        }
    }
    /**
     * Migrates external URIs to persisted attachments and binds them to a document.
     * Used for migrating old document formats that used external URIs.
     * 
     * Works by importing each URI as an attachment and binding it to the specified document.
     * Continues processing even if individual migrations fail.
     * 
     * arguments:
     *     context - Context: Android context for accessing content providers
     *     uris - List<Uri>: List of external URIs to migrate
     *     documentId - String: The document ID to bind migrated attachments to
     *     type - String: Type identifier for logging purposes (e.g., "photo", "pdf")
     * 
     * return:
     *     migratedCount - Int: Number of successfully migrated attachments
     */
    suspend fun migrate(
        context: Context,
        uris: List<Uri>,
        documentId: String,
        type: String
    ): Int {
        var migratedCount = 0
        uris.forEach { uri ->
            try {
                AppLogger.log("AttachmentInteractors", "Migrating $type: $uri")
                val attachment = repository.importAttachment(context, uri)
                repository.bindAttachmentsToDoc(listOf(attachment.id), documentId)
                migratedCount++
            } catch (e: Exception) {
                AppLogger.log("AttachmentInteractors", "ERROR: Failed to migrate $type $uri: ${e.message}")
                ErrorHandler.showWarning("Failed to migrate file $uri: ${e.message}")
            }
        }
        return migratedCount
    }
    /**
     * Lists all attachments associated with a document.
     * 
     * arguments:
     *     documentId - String: The document ID
     * 
     * return:
     *     attachments - List<AttachmentEntity>: List of attachments for the document
     */
    suspend fun listByDocument(documentId: String): List<AttachmentEntity> =
        repository.getAttachmentsByDoc(documentId)
    
    /**
     * Binds a list of attachments to a document.
     * 
     * arguments:
     *     attachmentIds - List<String>: List of attachment IDs to bind
     *     documentId - String: The document ID to bind attachments to
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun bindToDocument(attachmentIds: List<String>, documentId: String) =
        repository.bindAttachmentsToDoc(attachmentIds, documentId)
    
    /**
     * Prepares attachments for document update by persisting temporary URIs.
     * Ensures all attachments are stored in persistent storage before saving to database.
     * 
     * Works by checking each attachment's requiresPersist flag. If true, persists the URI
     * to permanent storage and updates the attachment with the new URI. Sets createdAt
     * timestamp if missing.
     * 
     * arguments:
     *     attachments - List<Attachment>: List of attachments to prepare
     * 
     * return:
     *     preparedAttachments - List<Attachment>: List of attachments with persisted URIs
     */
    suspend fun prepareForUpdate(attachments: List<com.example.docapp.domain.Attachment>): List<com.example.docapp.domain.Attachment> {
        return buildList {
            addAll(attachments.map { attachment ->
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
            })
        }
    }
}
