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

class AttachmentInteractors(
    private val repository: AttachmentRepository,
    private val attachmentStore: AttachmentStore
) {
    data class ImportResult(
        val successful: Int,
        val failed: Int,
        val attachments: List<AttachmentEntity>
    )

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

    suspend fun listByDocument(documentId: String): List<AttachmentEntity> =
        repository.getAttachmentsByDoc(documentId)

    suspend fun bindToDocument(attachmentIds: List<String>, documentId: String) =
        repository.bindAttachmentsToDoc(attachmentIds, documentId)

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

