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
        override suspend fun isPinSet(): Boolean = crypto.isPinSet()
        override suspend fun verifyPin(pin: String): Boolean {
            // Используем CryptoManager для проверки PIN
            return crypto.verifyPin(pin)
        }
        override suspend fun setNewPin(pin: String) {
            // Используем CryptoManager для установки первого PIN
            crypto.setInitialPin(pin)
        }
        override suspend fun disablePin() {
            crypto.clearSecurityData()
            dao.settings.clearPin()
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
        override suspend fun updateTemplate(template: Template, fields: List<TemplateField>) {
            // TODO: Implement template update functionality
            // This would require updating the template record and its fields
            throw NotImplementedError("Template update not yet implemented")
        }
        override suspend fun deleteTemplate(id: String) {
            // TODO: Implement template deletion functionality
            // This would require deleting the template and all its fields
            throw NotImplementedError("Template deletion not yet implemented")
        }
        override suspend fun pinTemplate(id: String, pinned: Boolean) {
            // TODO: Implement template pinning functionality
            // This would require updating the is_pinned and pinned_order fields
            throw NotImplementedError("Template pinning not yet implemented")
        }
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
            pdfUris: List<String>
        ): String {
            val photos = photoUris.map { files.persist(Uri.parse(it)).toString() }
            val pdfs = pdfUris.map { files.persist(Uri.parse(it)).toString() }
            val id = dao.documents.create(templateId, folderId, name, fields, photos, pdfs)
            dao.documents.touch(id)
            return id
        }

        override suspend fun getDocument(id: String) = dao.documents.getFull(id)

        override suspend fun updateDocument(
            doc: Document,
            fields: List<DocumentField>,
            attachments: List<Attachment>
        ) {
            dao.documents.update(doc, fields, attachments)
        }

        override suspend fun deleteDocument(id: String) = dao.documents.delete(id)
        override suspend fun pinDocument(id: String, pinned: Boolean) = dao.documents.pin(id, pinned)
        override suspend fun touchOpened(id: String) = dao.documents.touch(id)

        override suspend fun moveToFolder(id: String, folderId: String?) = dao.documents.move(id, folderId)
        override suspend fun swapPinned(aId: String, bId: String) = dao.documents.swapPinned(aId, bId)
    }
}
