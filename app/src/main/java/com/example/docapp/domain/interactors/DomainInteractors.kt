package com.example.docapp.domain.interactors

import com.example.docapp.core.AttachmentStore
import com.example.docapp.data.DocumentDao
import com.example.docapp.domain.Repositories

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

