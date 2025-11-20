package com.example.docapp.domain.interactors
import com.example.docapp.core.AttachmentStore
import com.example.docapp.data.DocumentDao
import com.example.docapp.domain.Repositories

/**
 * Container class that provides access to all domain interactors.
 * Lazy-initializes interactors for different domain entities.
 * 
 * Works by creating and exposing interactor instances for attachments, documents, templates,
 * folders, settings, and migration operations. Interactors are created lazily on first access.
 * 
 * arguments:
 *     repositories - Repositories: Container providing access to all repository interfaces
 *     attachmentStore - AttachmentStore: Service for managing attachment file storage
 *     documentDao - DocumentDao: Data access object for direct document database operations
 */
class DomainInteractors(
    private val repositories: Repositories,
    private val attachmentStore: AttachmentStore,
    private val documentDao: DocumentDao
) {
    val attachments by lazy {
        AttachmentInteractors(
            repository = repositories.attachments,
            attachmentStore = attachmentStore
        )
    }
    val documents by lazy { DocumentInteractors(repositories.documents) }
    val templates by lazy { TemplateInteractors(repositories.templates) }
    val folders by lazy { FolderInteractors(repositories.folders) }
    val settings by lazy { SettingsInteractors(repositories.settings) }
    val migration by lazy {
        MigrationInteractors(
            documentDao = documentDao,
            attachmentInteractors = attachments
        )
    }
}
