package com.example.docapp.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun isPinSet(): Boolean
    suspend fun verifyPin(pin: String): Boolean
    suspend fun setNewPin(pin: String)
    suspend fun disablePin()
}

interface FolderRepository {
    fun observeTree(): Flow<List<Folder>>
    suspend fun addFolder(name: String, parentId: String?): String
    suspend fun deleteFolder(id: String)
    suspend fun listAll(): List<Folder>
}

interface TemplateRepository {
    suspend fun listTemplates(): List<Template>
    suspend fun getTemplate(id: String): Template?
    suspend fun listFields(templateId: String): List<TemplateField>
    suspend fun addTemplate(name: String, fields: List<String>): String
    suspend fun updateTemplate(template: Template, fields: List<TemplateField>)
    suspend fun deleteTemplate(id: String)
    suspend fun pinTemplate(id: String, pinned: Boolean)
}

interface DocumentRepository {
    data class HomeList(val pinned: List<Document>, val recent: List<Document>)
    data class FullDocument(
        val doc: Document,
        val fields: List<DocumentField>,
        val photos: List<Attachment>,
        val pdfs: List<Attachment>
    )

    fun observeHome(): Flow<HomeList>

    suspend fun createDocument(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoUris: List<String>,
        pdfUris: List<String>
    ): String

    suspend fun createDocumentWithNames(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoFiles: List<Pair<Uri, String>>, // URI, displayName
        pdfFiles: List<Pair<Uri, String>> // URI, displayName
    ): String

    suspend fun getDocument(id: String): FullDocument?
    suspend fun updateDocument(doc: Document, fields: List<DocumentField>, attachments: List<Attachment>)
    suspend fun deleteDocument(id: String)
    suspend fun pinDocument(id: String, pinned: Boolean)
    suspend fun touchOpened(id: String)

    suspend fun moveToFolder(id: String, folderId: String?)
    suspend fun swapPinned(aId: String, bId: String)
    suspend fun getDocumentsInFolder(folderId: String): List<Document>
}

/** Интерфейс-агрегатор репозиториев */
interface Repositories {
    val settings: SettingsRepository
    val folders: FolderRepository
    val templates: TemplateRepository
    val documents: DocumentRepository
    val attachments: com.example.docapp.domain.repo.AttachmentRepository
}
