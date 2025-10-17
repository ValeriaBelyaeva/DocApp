package com.example.docapp

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.docapp.core.EncryptedAttachmentStore
import com.example.docapp.core.newId
import com.example.docapp.core.now
import com.example.docapp.domain.Attachment
import com.example.docapp.domain.AttachmentKind
import com.example.docapp.domain.Document
import com.example.docapp.domain.DocumentField
import com.example.docapp.domain.DocumentRepository
import com.example.docapp.domain.Folder
import com.example.docapp.domain.Repositories
import com.example.docapp.domain.SettingsRepository
import com.example.docapp.domain.Template
import com.example.docapp.domain.TemplateField
import com.example.docapp.domain.TemplateRepository
import com.example.docapp.domain.usecases.UseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DocumentUpdateUseCaseTest {

    private lateinit var context: Context
    private lateinit var attachmentStore: EncryptedAttachmentStore
    private lateinit var fakeRepos: FakeRepositories
    private lateinit var useCases: UseCases
    private lateinit var document: Document

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        attachmentStore = EncryptedAttachmentStore(context)
        val fakeDocumentRepository = FakeDocumentRepository()
        fakeRepos = FakeRepositories(fakeDocumentRepository)
        useCases = UseCases(fakeRepos, attachmentStore)
        val timestamp = now()
        document = Document(
            id = newId(),
            templateId = null,
            folderId = null,
            name = "Test",
            isPinned = false,
            pinnedOrder = null,
            createdAt = timestamp,
            updatedAt = timestamp,
            lastOpenedAt = timestamp
        )
    }

    @Test
    fun updateDoc_persistsNewAttachments_andAllowsRetrievalAfterRestart() = runBlocking {
        val existingSource = File(context.cacheDir, "existing-photo.jpg").apply {
            writeText("existing")
        }
        val existingStoredUri = attachmentStore.persist(Uri.fromFile(existingSource))
        val existingAttachment = Attachment(
            id = newId(),
            documentId = document.id,
            kind = AttachmentKind.photo,
            fileName = "existing-photo.jpg",
            uri = existingStoredUri,
            createdAt = System.currentTimeMillis(),
            requiresPersist = false
        )

        val newSource = File(context.cacheDir, "new-attachment.pdf").apply {
            writeText("new content")
        }
        val newAttachment = Attachment(
            id = newId(),
            documentId = document.id,
            kind = AttachmentKind.pdfs,
            fileName = "new-attachment.pdf",
            uri = Uri.fromFile(newSource),
            createdAt = System.currentTimeMillis(),
            requiresPersist = true
        )

        val fullDocument = DocumentRepository.FullDocument(
            doc = document,
            fields = emptyList(),
            photos = listOf(existingAttachment),
            pdfs = listOf(newAttachment)
        )

        useCases.updateDoc(fullDocument)

        val persistedAttachments = fakeRepos.documentRepository.lastUpdatedAttachments
        assertEquals(2, persistedAttachments.size)
        assertFalse(persistedAttachments.any { it.requiresPersist })

        val persistedNew = persistedAttachments.first { it.id == newAttachment.id }
        assertTrue(persistedNew.uri.path?.contains("attachments") == true)
        val retrievedStream = attachmentStore.retrieve(persistedNew.uri)
        assertNotNull("Persisted attachment should be retrievable", retrievedStream)
        retrievedStream?.bufferedReader()?.use { reader ->
            assertEquals("new content", reader.readText())
        }

        val newStoreInstance = EncryptedAttachmentStore(context)
        val retrievedAfterRestart = newStoreInstance.retrieve(persistedNew.uri)
        assertNotNull("Attachment should be retrievable after restarting store", retrievedAfterRestart)
    }

    private class FakeRepositories(
        val documentRepository: FakeDocumentRepository
    ) : Repositories {
        override val settings: SettingsRepository = object : SettingsRepository {
            override suspend fun isPinSet(): Boolean = throw NotImplementedError()
            override suspend fun verifyPin(pin: String): Boolean = throw NotImplementedError()
            override suspend fun setNewPin(pin: String) = throw NotImplementedError()
            override suspend fun disablePin() = throw NotImplementedError()
        }

        override val folders = object : com.example.docapp.domain.FolderRepository {
            override fun observeTree(): Flow<List<Folder>> = throw NotImplementedError()
            override suspend fun addFolder(name: String, parentId: String?): String = throw NotImplementedError()
            override suspend fun deleteFolder(id: String) = throw NotImplementedError()
            override suspend fun listAll(): List<Folder> = throw NotImplementedError()
        }

        override val templates: TemplateRepository = object : TemplateRepository {
            override suspend fun listTemplates(): List<Template> = throw NotImplementedError()
            override suspend fun getTemplate(id: String): Template? = throw NotImplementedError()
            override suspend fun listFields(templateId: String): List<TemplateField> = throw NotImplementedError()
            override suspend fun addTemplate(name: String, fields: List<String>): String = throw NotImplementedError()
            override suspend fun updateTemplate(template: Template, fields: List<TemplateField>) = throw NotImplementedError()
            override suspend fun deleteTemplate(id: String) = throw NotImplementedError()
            override suspend fun pinTemplate(id: String, pinned: Boolean) = throw NotImplementedError()
        }

        override val documents: DocumentRepository
            get() = documentRepository
    }

    private class FakeDocumentRepository : DocumentRepository {
        var lastUpdatedAttachments: List<Attachment> = emptyList()

        override fun observeHome(): Flow<DocumentRepository.HomeList> = throw NotImplementedError()

        override suspend fun createDocument(
            templateId: String?,
            folderId: String?,
            name: String,
            fields: List<Pair<String, String>>,
            photoUris: List<String>,
            pdfUris: List<String>
        ): String = throw NotImplementedError()

        override suspend fun getDocument(id: String): DocumentRepository.FullDocument? = throw NotImplementedError()

        override suspend fun updateDocument(
            doc: Document,
            fields: List<DocumentField>,
            attachments: List<Attachment>
        ) {
            lastUpdatedAttachments = attachments
        }

        override suspend fun deleteDocument(id: String) = throw NotImplementedError()
        override suspend fun pinDocument(id: String, pinned: Boolean) = throw NotImplementedError()
        override suspend fun touchOpened(id: String) = throw NotImplementedError()
        override suspend fun moveToFolder(id: String, folderId: String?) = throw NotImplementedError()
        override suspend fun swapPinned(aId: String, bId: String) = throw NotImplementedError()
    }
}
