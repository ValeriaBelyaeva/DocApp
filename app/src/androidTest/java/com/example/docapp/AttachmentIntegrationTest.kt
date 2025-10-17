package com.example.docapp

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.docapp.core.ServiceLocator
import com.example.docapp.data.db.entities.AttachmentEntity
import com.example.docapp.domain.repo.AttachmentRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class AttachmentIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var attachmentRepository: AttachmentRepository
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Инициализируем ServiceLocator для тестов
        ServiceLocator.init(context)
        // Устанавливаем тестовый PIN для создания БД
        ServiceLocator.initializeWithPin("1234", true)
        attachmentRepository = ServiceLocator.repos.attachments
    }
    
    @After
    fun tearDown() {
        // Очищаем тестовые файлы
        val attachmentsDir = File(context.filesDir, "attachments")
        if (attachmentsDir.exists()) {
            attachmentsDir.deleteRecursively()
        }
    }
    
    @Test
    fun `test complete attachment lifecycle`() = runTest {
        // 1. Создаем тестовый файл
        val testFile = createTestFile("test_document.pdf", "Test PDF content")
        val testUri = Uri.fromFile(testFile)
        
        // 2. Импортируем файл
        val importedAttachment = attachmentRepository.importAttachment(context, testUri)
        
        assertNotNull("Imported attachment should not be null", importedAttachment)
        assertEquals("Name should match", "test_document.pdf", importedAttachment.name)
        assertEquals("MIME type should be PDF", "application/pdf", importedAttachment.mime)
        assertTrue("Size should be greater than 0", importedAttachment.size > 0)
        assertNotNull("SHA256 should not be null", importedAttachment.sha256)
        
        // 3. Проверяем, что файл существует
        assertTrue("Attachment file should exist", attachmentRepository.validateAttachmentIntegrity(importedAttachment))
        
        // 4. Получаем InputStream
        val inputStream = attachmentRepository.getAttachmentInputStream(context, importedAttachment)
        assertNotNull("InputStream should not be null", inputStream)
        inputStream?.close()
        
        // 5. Получаем File объект
        val file = attachmentRepository.getAttachmentFile(importedAttachment)
        assertNotNull("File should not be null", file)
        assertTrue("File should exist", file?.exists() == true)
        
        // 6. Привязываем к документу
        attachmentRepository.bindAttachmentsToDoc(listOf(importedAttachment.id), "test_doc_1")
        
        // 7. Проверяем, что вложение привязано к документу
        val attachmentsByDoc = attachmentRepository.getAttachmentsByDoc("test_doc_1")
        assertEquals("Should have 1 attachment", 1, attachmentsByDoc.size)
        assertEquals("Attachment should be bound to document", "test_doc_1", attachmentsByDoc[0].docId)
        
        // 8. Удаляем вложение
        val deleted = attachmentRepository.deleteAttachment(importedAttachment.id)
        assertTrue("Attachment should be deleted successfully", deleted)
        
        // 9. Проверяем, что вложение больше не существует
        val deletedAttachment = attachmentRepository.getAttachment(importedAttachment.id)
        assertNull("Deleted attachment should be null", deletedAttachment)
    }
    
    @Test
    fun `test batch import and cleanup`() = runTest {
        // 1. Создаем несколько тестовых файлов
        val testFiles = listOf(
            createTestFile("test1.txt", "Content 1"),
            createTestFile("test2.txt", "Content 2"),
            createTestFile("test3.txt", "Content 3")
        )
        val testUris = testFiles.map { Uri.fromFile(it) }
        
        // 2. Импортируем все файлы
        val importedAttachments = attachmentRepository.importAttachments(context, testUris)
        
        assertEquals("Should import all 3 files", 3, importedAttachments.size)
        
        // 3. Проверяем дубликаты (файлы с одинаковым содержимым)
        val duplicates = attachmentRepository.findDuplicates(importedAttachments[0].sha256)
        assertTrue("Should find duplicates", duplicates.isNotEmpty())
        
        // 4. Проверяем очистку сирот (файлы не привязанные к документам)
        val cleanupResult = attachmentRepository.cleanupOrphans()
        
        // Поскольку файлы не привязаны к документам, они должны быть удалены как сироты
        assertEquals("Should have 3 deleted files", 3, cleanupResult.deletedFiles)
        assertEquals("Should have 3 deleted records", 3, cleanupResult.deletedRecords)
        assertEquals("Should have 0 errors", 0, cleanupResult.errors)
        
        // 5. Проверяем, что файлы действительно удалены
        val remainingAttachments = attachmentRepository.getAttachmentsByDoc("nonexistent_doc")
        assertEquals("Should have no remaining attachments", 0, remainingAttachments.size)
    }
    
    @Test
    fun `test document attachment management`() = runTest {
        val docId = "test_document_1"
        
        // 1. Создаем и импортируем файлы для документа
        val testFiles = listOf(
            createTestFile("photo1.jpg", "Photo 1 content"),
            createTestFile("photo2.jpg", "Photo 2 content"),
            createTestFile("document.pdf", "PDF content")
        )
        val testUris = testFiles.map { Uri.fromFile(it) }
        
        val importedAttachments = attachmentRepository.importAttachments(context, testUris)
        
        // 2. Привязываем все файлы к документу
        val attachmentIds = importedAttachments.map { it.id }
        attachmentRepository.bindAttachmentsToDoc(attachmentIds, docId)
        
        // 3. Проверяем, что все файлы привязаны
        val documentAttachments = attachmentRepository.getAttachmentsByDoc(docId)
        assertEquals("Should have 3 attachments", 3, documentAttachments.size)
        
        // 4. Удаляем один файл
        val attachmentToDelete = documentAttachments[0]
        val deleted = attachmentRepository.deleteAttachment(attachmentToDelete.id)
        assertTrue("Attachment should be deleted", deleted)
        
        // 5. Проверяем, что осталось 2 файла
        val remainingAttachments = attachmentRepository.getAttachmentsByDoc(docId)
        assertEquals("Should have 2 remaining attachments", 2, remainingAttachments.size)
        
        // 6. Удаляем все вложения документа
        val allDeleted = attachmentRepository.deleteAttachmentsByDoc(docId)
        assertTrue("All attachments should be deleted", allDeleted)
        
        // 7. Проверяем, что документ не имеет вложений
        val finalAttachments = attachmentRepository.getAttachmentsByDoc(docId)
        assertEquals("Should have no attachments", 0, finalAttachments.size)
    }
    
    @Test
    fun `test file integrity validation`() = runTest {
        // 1. Создаем и импортируем файл
        val testFile = createTestFile("integrity_test.txt", "Test content for integrity")
        val testUri = Uri.fromFile(testFile)
        
        val importedAttachment = attachmentRepository.importAttachment(context, testUri)
        
        // 2. Проверяем целостность существующего файла
        val isValid = attachmentRepository.validateAttachmentIntegrity(importedAttachment)
        assertTrue("Valid attachment should pass integrity check", isValid)
        
        // 3. Удаляем физический файл
        val file = attachmentRepository.getAttachmentFile(importedAttachment)
        assertTrue("File should exist before deletion", file?.delete() == true)
        
        // 4. Проверяем целостность после удаления файла
        val isInvalid = attachmentRepository.validateAttachmentIntegrity(importedAttachment)
        assertFalse("Deleted file should fail integrity check", isInvalid)
    }
    
    private fun createTestFile(fileName: String, content: String): File {
        val testDir = File(context.filesDir, "test_files")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        
        val file = File(testDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(content.toByteArray())
        }
        
        return file
    }
}
