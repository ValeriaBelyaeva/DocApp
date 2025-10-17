package com.example.docapp.domain.usecases

import android.net.Uri
import com.example.docapp.domain.Attachment
import com.example.docapp.domain.DocumentRepository
import com.example.docapp.domain.Repositories
import com.example.docapp.core.AttachmentStore
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.UriDebugger
import com.example.docapp.domain.usecase.ImportAttachmentsUseCase
import com.example.docapp.domain.usecase.DeleteAttachmentUseCase
import com.example.docapp.domain.usecase.CleanupOrphansUseCase
import com.example.docapp.domain.usecase.MigrateExternalUrisUseCase

class UseCases(
    private val repos: Repositories,
    private val attachmentStore: AttachmentStore,
    private val documentDao: com.example.docapp.data.DocumentDao
) {
    
    // Attachment UseCases
    val importAttachments = ImportAttachmentsUseCase(repos.attachments)
    val deleteAttachment = DeleteAttachmentUseCase(repos.attachments)
    val cleanupOrphans = CleanupOrphansUseCase(repos.attachments)
    val migrateExternalUris = MigrateExternalUrisUseCase(repos.attachments, documentDao)
    
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

    // Documents
    suspend fun createDoc(
        tplId: String?,
        folderId: String?,
        name: String,
        fields: List<Pair<String, String>>,
        photos: List<String>,
        pdfUris: List<String>
    ) = repos.documents.createDocument(tplId, folderId, name, fields, photos, pdfUris)

    suspend fun createDocWithNames(
        tplId: String?,
        folderId: String?,
        name: String,
        fields: List<Pair<String, String>>,
        photoFiles: List<Pair<Uri, String>>, // URI, displayName
        pdfFiles: List<Pair<Uri, String>> // URI, displayName
    ) = repos.documents.createDocumentWithNames(tplId, folderId, name, fields, photoFiles, pdfFiles)

    suspend fun getDoc(id: String) = repos.documents.getDocument(id)

    suspend fun updateDoc(fd: DocumentRepository.FullDocument) {
        ErrorHandler.showInfo("UseCases: Обновляем документ с ${fd.photos.size} фото и ${fd.pdfs.size} PDF")
        
        val preparedAttachments = buildList {
            addAll(fd.photos)
            addAll(fd.pdfs)
        }.map { attachment ->
            UriDebugger.showUriDebug("ВЛОЖЕНИЕ: ${attachment.displayName}", attachment.uri)
            
            if (attachment.requiresPersist) {
                UriDebugger.showUriDebug("СОХРАНИТЬ: ${attachment.displayName}", attachment.uri)
                val persistedUri = attachmentStore.persist(attachment.uri)
                UriDebugger.showUriSuccess("СОХРАНЕНО: ${attachment.displayName}", persistedUri)
                attachment.copy(
                    uri = persistedUri,
                    createdAt = if (attachment.createdAt <= 0L) System.currentTimeMillis() else attachment.createdAt,
                    requiresPersist = false
                )
            } else {
                UriDebugger.showUriSuccess("УЖЕ СОХРАНЕНО: ${attachment.displayName}", attachment.uri)
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

    // Pinned reorder
    suspend fun swapPinned(aId: String, bId: String) = repos.documents.swapPinned(aId, bId)
}