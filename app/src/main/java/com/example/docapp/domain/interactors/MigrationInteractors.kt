package com.example.docapp.domain.interactors

import android.content.Context
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.data.DocumentDao

class MigrationInteractors(
    private val documentDao: DocumentDao,
    private val attachmentInteractors: AttachmentInteractors
) {
    data class MigrationResult(
        val migratedDocuments: Int,
        val migratedAttachments: Int,
        val errors: Int
    )

    suspend fun migrateExternalUris(context: Context): MigrationResult {
        return try {
            AppLogger.log("MigrationInteractors", "Starting migration of external URIs...")
            ErrorHandler.showInfo("Migrating external URIs...")

            var migratedDocuments = 0
            var migratedAttachments = 0
            var errors = 0

            val allDocuments = documentDao.getAllDocumentIds()

            allDocuments.forEach { docId ->
                try {
                    AppLogger.log("MigrationInteractors", "Migrating document: $docId")

                    val fullDoc = documentDao.getFull(docId)
                    if (fullDoc == null) {
                        AppLogger.log("MigrationInteractors", "Document not found: $docId")
                        return@forEach
                    }

                    val photoAttachments = attachmentInteractors.migrate(
                        context,
                        fullDoc.photos.map { it.uri },
                        docId,
                        "photo"
                    )

                    val pdfAttachments = attachmentInteractors.migrate(
                        context,
                        fullDoc.pdfs.map { it.uri },
                        docId,
                        "pdf"
                    )

                    migratedAttachments += photoAttachments + pdfAttachments
                    migratedDocuments++

                    AppLogger.log(
                        "MigrationInteractors",
                        "Migrated document $docId: $photoAttachments photos, $pdfAttachments PDFs"
                    )
                } catch (e: Exception) {
                    errors++
                    AppLogger.log("MigrationInteractors", "ERROR: Failed to migrate document $docId: ${e.message}")
                    ErrorHandler.showWarning("Document migration error for $docId: ${e.message}")
                }
            }

            val result = MigrationResult(
                migratedDocuments = migratedDocuments,
                migratedAttachments = migratedAttachments,
                errors = errors
            )

            AppLogger.log("MigrationInteractors", "Migration completed: $result")

            if (errors == 0) {
                ErrorHandler.showSuccess("Migration complete: $migratedDocuments documents, $migratedAttachments attachments")
            } else {
                ErrorHandler.showWarning("Migration completed with errors: $migratedDocuments documents, $errors errors")
            }

            result
        } catch (e: Exception) {
            AppLogger.log("MigrationInteractors", "ERROR: Migration failed: ${e.message}")
            ErrorHandler.showError("Migration failed: ${e.message}")
            throw e
        }
    }
}

