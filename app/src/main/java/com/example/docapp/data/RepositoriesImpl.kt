package com.example.docapp.data

import android.content.Context
import android.net.Uri
import com.example.docapp.core.AttachmentStore
import com.example.docapp.core.CryptoManager
import com.example.docapp.domain.*
import kotlinx.coroutines.flow.Flow

class RepositoriesImpl(
    private val dao: SqlDaoFactory,
    private val crypto: CryptoManager,
    private val files: AttachmentStore,
    private val ctx: Context
) : Repositories {

    override val settings: SettingsRepository = object : SettingsRepository {
        override suspend fun isPinSet(): Boolean = dao.settings.isPinSet()
        override suspend fun verifyPin(pin: String): Boolean {
            val s = dao.settings.get()
            val hash = crypto.sha256(pin.toByteArray())
            return s.pinHash.contentEquals(hash).also { if (it) crypto.deriveRuntimeKeysFromPin(pin) }
        }
        override suspend fun setNewPin(pin: String) {
            val hash = crypto.sha256(pin.toByteArray())
            dao.settings.updatePin(hash)
            crypto.deriveRuntimeKeysFromPin(pin)
        }
    }

    override val folders: FolderRepository = object : FolderRepository {
        override fun observeTree() = dao.folders.observeTree()
        override suspend fun addFolder(name: String, parentId: String?) = dao.folders.add(name, parentId)
        override suspend fun deleteFolder(id: String) = dao.folders.delete(id)
        override suspend fun listAll(): List<Folder> = dao.folders.list()
    }

    override val templates: TemplateRepository = object : TemplateRepository {
        override suspend fun listTemplates(): List<Template> = dao.templates.list()
        override suspend fun getTemplate(id: String): Template? = dao.templates.get(id)
        override suspend fun listFields(templateId: String) = dao.templates.listFields(templateId)
        override suspend fun addTemplate(name: String, fields: List<String>) = dao.templates.add(name, fields)
        override suspend fun updateTemplate(template: Template, fields: List<TemplateField>) { /* TODO */ }
        override suspend fun deleteTemplate(id: String) { /* TODO */ }
        override suspend fun pinTemplate(id: String, pinned: Boolean) { /* TODO */ }
    }

    override val documents: DocumentRepository = object : DocumentRepository {
        override fun observeHome(): Flow<DocumentRepository.HomeList> =
            dao.documents.observeHome().also { dao.documents.emitHome(); dao.folders.emitTree() }

        override suspend fun createDocument(
            templateId: String?,
            folderId: String?,
            name: String,
            fields: List<Pair<String, String>>,
            photoUris: List<String>,
            pdfUri: String?
        ): String {
            val photos = photoUris.map { files.persist(Uri.parse(it)).toString() }
            val pdf = pdfUri?.let { files.persist(Uri.parse(it)).toString() }
            val id = dao.documents.create(templateId, folderId, name, fields, photos, pdf)
            dao.documents.touch(id)
            return id
        }

        override suspend fun getDocument(id: String) = dao.documents.getFull(id)

        override suspend fun updateDocument(
            doc: Document,
            fields: List<DocumentField>,
            attachments: List<Attachment>
        ) = dao.documents.update(doc, fields, attachments)

        override suspend fun deleteDocument(id: String) = dao.documents.delete(id)
        override suspend fun pinDocument(id: String, pinned: Boolean) = dao.documents.pin(id, pinned)
        override suspend fun touchOpened(id: String) = dao.documents.touch(id)

        override suspend fun moveToFolder(id: String, folderId: String?) = dao.documents.move(id, folderId)
        override suspend fun swapPinned(aId: String, bId: String) = dao.documents.swapPinned(aId, bId)
    }
}
