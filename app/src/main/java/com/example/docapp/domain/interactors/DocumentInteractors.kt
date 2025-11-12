package com.example.docapp.domain.interactors

import com.example.docapp.domain.DocumentRepository

class DocumentInteractors(
    private val repository: DocumentRepository
) {
    suspend fun create(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoUris: List<String>,
        pdfUris: List<String>
    ) = repository.createDocument(templateId, folderId, name, description, fields, photoUris, pdfUris)

    suspend fun createWithNames(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoFiles: List<Pair<android.net.Uri, String>>,
        pdfFiles: List<Pair<android.net.Uri, String>>
    ) = repository.createDocumentWithNames(templateId, folderId, name, description, fields, photoFiles, pdfFiles)

    suspend fun get(id: String) = repository.getDocument(id)

    suspend fun update(fullDocument: DocumentRepository.FullDocument, preparedAttachments: List<com.example.docapp.domain.Attachment>) {
        repository.updateDocument(fullDocument.doc, fullDocument.fields, preparedAttachments)
    }

    suspend fun delete(id: String) = repository.deleteDocument(id)

    suspend fun pin(id: String, pinned: Boolean) = repository.pinDocument(id, pinned)

    suspend fun touchOpened(id: String) = repository.touchOpened(id)

    suspend fun moveToFolder(id: String, folderId: String?) = repository.moveToFolder(id, folderId)

    suspend fun swapPinned(aId: String, bId: String) = repository.swapPinned(aId, bId)

    suspend fun getDocumentsInFolder(folderId: String) = repository.getDocumentsInFolder(folderId)

    fun observeHome() = repository.observeHome()
}

