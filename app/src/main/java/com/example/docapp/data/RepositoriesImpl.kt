package com.example.docapp.data

import android.content.Context
import android.net.Uri
import com.example.docapp.core.AttachmentStore
import com.example.docapp.core.CryptoManager
import com.example.docapp.core.ErrorHandler
import com.example.docapp.domain.*
import com.example.docapp.domain.repo.AttachmentRepository
import com.example.docapp.domain.repo.AttachmentRepositoryImpl
import com.example.docapp.data.storage.AttachStorage
import com.example.docapp.data.storage.AttachStorageImpl
import com.example.docapp.data.storage.FileGc
import kotlinx.coroutines.flow.Flow

class RepositoriesImpl(
    private val dao: SqlDaoFactory,
    private val crypto: CryptoManager,
    private val files: AttachmentStore,
    private val ctx: Context
) : Repositories {
    
    private val attachStorage: AttachStorage = AttachStorageImpl(ctx)
    private val fileGc: FileGc = FileGc(dao.attachments, attachStorage)
    
    override val attachments: AttachmentRepository = AttachmentRepositoryImpl(
        dao.attachments,
        attachStorage,
        fileGc
    )

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
            // Обновление шаблона пока не реализовано
            throw NotImplementedError("Template update not yet implemented")
        }
        override suspend fun deleteTemplate(id: String) {
            dao.templates.delete(id)
        }
        override suspend fun pinTemplate(id: String, pinned: Boolean) {
            // Закрепление шаблона пока не реализовано
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
            description: String,
            fields: List<Pair<String, String>>,
            photoUris: List<String>,
            pdfUris: List<String>
        ): String {
            val photos = photoUris.map { files.persist(Uri.parse(it)).toString() }
            val pdfs = pdfUris.map { files.persist(Uri.parse(it)).toString() }
            val id = dao.documents.create(templateId, folderId, name, description, fields, photos, pdfs)
            dao.documents.touch(id)
            return id
        }

        override suspend fun createDocumentWithNames(
            templateId: String?,
            folderId: String?,
            name: String,
            description: String,
            fields: List<Pair<String, String>>,
            photoFiles: List<Pair<Uri, String>>, // URI, displayName
            pdfFiles: List<Pair<Uri, String>> // URI, displayName
        ): String {
            ErrorHandler.showInfo("RepositoriesImpl: Создаем документ: $name")
            ErrorHandler.showInfo("RepositoriesImpl: Фото: ${photoFiles.size}, PDF: ${pdfFiles.size}")
            
            val photos = photoFiles.mapIndexed { index, (uri, displayName) -> 
                ErrorHandler.showInfo("RepositoriesImpl: Обрабатываем фото $index: $displayName")
                ErrorHandler.showInfo("RepositoriesImpl: Исходный URI: $uri")
                val persistedUri = files.persist(uri)
                val persistedUriString = persistedUri.toString()
                ErrorHandler.showInfo("RepositoriesImpl: Фото $index сохранено: ${persistedUriString.take(50)}...")
                Pair(persistedUriString, displayName)
            }
            
            val pdfs = pdfFiles.mapIndexed { index, (uri, displayName) -> 
                ErrorHandler.showInfo("RepositoriesImpl: Обрабатываем PDF $index: $displayName")
                ErrorHandler.showInfo("RepositoriesImpl: Исходный URI: $uri")
                val persistedUri = files.persist(uri)
                val persistedUriString = persistedUri.toString()
                ErrorHandler.showInfo("RepositoriesImpl: PDF $index сохранен: ${persistedUriString.take(50)}...")
                Pair(persistedUriString, displayName)
            }
            
            ErrorHandler.showInfo("RepositoriesImpl: Сохраняем в БД: ${photos.size} фото, ${pdfs.size} PDF")
            val id = dao.documents.createWithNames(templateId, folderId, name, description, fields, photos, pdfs)
            ErrorHandler.showInfo("RepositoriesImpl: Документ создан с ID: $id")
            dao.documents.touch(id)
            return id
        }

        override suspend fun getDocument(id: String) = dao.documents.getFull(id).also { fullDoc ->
            ErrorHandler.showInfo("RepositoriesImpl: Загружаем документ: $id")
            fullDoc?.let { doc ->
                ErrorHandler.showInfo("RepositoriesImpl: Документ: ${doc.doc.name}")
                ErrorHandler.showInfo("RepositoriesImpl: Фото: ${doc.photos.size}, PDF: ${doc.pdfs.size}")
                
                doc.photos.forEachIndexed { index, photo ->
                    ErrorHandler.showInfo("RepositoriesImpl: Фото $index: ${photo.displayName ?: "Без имени"}")
                    // Проверяем существование файла через новую систему AttachStorage
                    try {
                        val file = java.io.File(photo.uri.path ?: "")
                        val exists = file.exists() && file.canRead()
                        ErrorHandler.showInfo("RepositoriesImpl: Фото $index существует: $exists")
                    } catch (e: Exception) {
                        ErrorHandler.showWarning("RepositoriesImpl: Ошибка проверки фото $index: ${e.message}")
                    }
                }
                
                doc.pdfs.forEachIndexed { index, pdf ->
                    ErrorHandler.showInfo("RepositoriesImpl: PDF $index: ${pdf.displayName ?: "Без имени"}")
                    // Проверяем существование файла через новую систему AttachStorage
                    try {
                        val file = java.io.File(pdf.uri.path ?: "")
                        val exists = file.exists() && file.canRead()
                        ErrorHandler.showInfo("RepositoriesImpl: PDF $index существует: $exists")
                    } catch (e: Exception) {
                        ErrorHandler.showWarning("RepositoriesImpl: Ошибка проверки PDF $index: ${e.message}")
                    }
                }
            }
        }

        override suspend fun updateDocument(
            doc: Document,
            fields: List<DocumentField>,
            attachments: List<Attachment>
        ) {
            ErrorHandler.showInfo("RepositoriesImpl: Обновляем документ: ${doc.name}")
            ErrorHandler.showInfo("RepositoriesImpl: Вложений: ${attachments.size}")
            
            // Обрабатываем все вложения через AttachmentStore.persist()
            val persistedAttachments = attachments.mapIndexed { index, attachment ->
                ErrorHandler.showInfo("RepositoriesImpl: Обрабатываем вложение $index: ${attachment.displayName}")
                val originalUri = attachment.uri
                val persistedUri = files.persist(originalUri)
                val persistedUriString = persistedUri.toString()
                ErrorHandler.showInfo("RepositoriesImpl: Вложение $index сохранено: ${persistedUriString.take(50)}...")
                
                // Создаем новый Attachment с сохраненным URI
                attachment.copy(uri = persistedUri)
            }
            
            ErrorHandler.showInfo("RepositoriesImpl: Сохраняем в БД: ${persistedAttachments.size} вложений")
            dao.documents.update(doc, fields, persistedAttachments)
        }

        override suspend fun deleteDocument(id: String) = dao.documents.delete(id)
        override suspend fun pinDocument(id: String, pinned: Boolean) = dao.documents.pin(id, pinned)
        override suspend fun touchOpened(id: String) = dao.documents.touch(id)

        override suspend fun moveToFolder(id: String, folderId: String?) = dao.documents.move(id, folderId)
        override suspend fun swapPinned(aId: String, bId: String) = dao.documents.swapPinned(aId, bId)
    }
}
