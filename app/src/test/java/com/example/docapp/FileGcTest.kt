package com.example.docapp

import androidx.test.core.app.ApplicationProvider
import com.example.docapp.data.db.dao.AttachmentDao
import com.example.docapp.data.storage.AttachStorage
import com.example.docapp.data.storage.FileGc
import com.example.docapp.data.storage.FileGc.CleanupResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class FileGcTest {
    
    private lateinit var mockAttachmentDao: AttachmentDao
    private lateinit var mockAttachStorage: AttachStorage
    private lateinit var fileGc: FileGc
    
    @Before
    fun setUp() {
        mockAttachmentDao = mock(AttachmentDao::class.java)
        mockAttachStorage = mock(AttachStorage::class.java)
        fileGc = FileGc(mockAttachmentDao, mockAttachStorage)
    }
    
    @Test
    fun `test cleanup orphans with no orphans`() = runTest {
        // Настраиваем мок для возврата пустого списка сирот
        `when`(mockAttachmentDao.listOrphans()).thenReturn(emptyList())
        
        val result = fileGc.cleanupOrphans()
        
        assertEquals("Should have 0 deleted files", 0, result.deletedFiles)
        assertEquals("Should have 0 deleted records", 0, result.deletedRecords)
        assertEquals("Should have 0 errors", 0, result.errors)
        
        // Проверяем, что методы были вызваны
        verify(mockAttachmentDao).listOrphans()
        verify(mockAttachStorage, never()).deletePhysical(any())
        verify(mockAttachmentDao, never()).deleteById(any())
    }
    
    @Test
    fun `test cleanup orphans with successful deletion`() = runTest {
        val orphan1 = createTestAttachment("id1", "file1.txt", "/path/file1.txt")
        val orphan2 = createTestAttachment("id2", "file2.txt", "/path/file2.txt")
        
        // Настраиваем мок для возврата списка сирот
        `when`(mockAttachmentDao.listOrphans()).thenReturn(listOf(orphan1, orphan2))
        
        // Настраиваем мок для успешного удаления файлов
        `when`(mockAttachStorage.deletePhysical(orphan1)).thenReturn(true)
        `when`(mockAttachStorage.deletePhysical(orphan2)).thenReturn(true)
        
        val result = fileGc.cleanupOrphans()
        
        assertEquals("Should have 2 deleted files", 2, result.deletedFiles)
        assertEquals("Should have 2 deleted records", 2, result.deletedRecords)
        assertEquals("Should have 0 errors", 0, result.errors)
        
        // Проверяем, что все методы были вызваны
        verify(mockAttachmentDao).listOrphans()
        verify(mockAttachStorage).deletePhysical(orphan1)
        verify(mockAttachStorage).deletePhysical(orphan2)
        verify(mockAttachmentDao).deleteById("id1")
        verify(mockAttachmentDao).deleteById("id2")
    }
    
    @Test
    fun `test cleanup orphans with file deletion failure`() = runTest {
        val orphan1 = createTestAttachment("id1", "file1.txt", "/path/file1.txt")
        val orphan2 = createTestAttachment("id2", "file2.txt", "/path/file2.txt")
        
        // Настраиваем мок для возврата списка сирот
        `when`(mockAttachmentDao.listOrphans()).thenReturn(listOf(orphan1, orphan2))
        
        // Настраиваем мок для частично неудачного удаления файлов
        `when`(mockAttachStorage.deletePhysical(orphan1)).thenReturn(true)
        `when`(mockAttachStorage.deletePhysical(orphan2)).thenReturn(false) // Ошибка удаления
        
        val result = fileGc.cleanupOrphans()
        
        assertEquals("Should have 1 deleted file", 1, result.deletedFiles)
        assertEquals("Should have 2 deleted records", 2, result.deletedRecords)
        assertEquals("Should have 1 error", 1, result.errors)
        
        // Проверяем, что все методы были вызваны
        verify(mockAttachmentDao).listOrphans()
        verify(mockAttachStorage).deletePhysical(orphan1)
        verify(mockAttachStorage).deletePhysical(orphan2)
        verify(mockAttachmentDao).deleteById("id1")
        verify(mockAttachmentDao).deleteById("id2")
    }
    
    @Test
    fun `test cleanup document attachments`() = runTest {
        val attachment1 = createTestAttachment("id1", "file1.txt", "/path/file1.txt", "doc1")
        val attachment2 = createTestAttachment("id2", "file2.txt", "/path/file2.txt", "doc1")
        
        // Настраиваем мок для возврата вложений документа
        `when`(mockAttachmentDao.listByDoc("doc1")).thenReturn(listOf(attachment1, attachment2))
        
        // Настраиваем мок для успешного удаления файлов
        `when`(mockAttachStorage.deletePhysical(attachment1)).thenReturn(true)
        `when`(mockAttachStorage.deletePhysical(attachment2)).thenReturn(true)
        
        val result = fileGc.cleanupDocumentAttachments("doc1")
        
        assertEquals("Should have 2 deleted files", 2, result.deletedFiles)
        assertEquals("Should have 2 deleted records", 2, result.deletedRecords)
        assertEquals("Should have 0 errors", 0, result.errors)
        
        // Проверяем, что все методы были вызваны
        verify(mockAttachmentDao).listByDoc("doc1")
        verify(mockAttachStorage).deletePhysical(attachment1)
        verify(mockAttachStorage).deletePhysical(attachment2)
        verify(mockAttachmentDao).deleteById("id1")
        verify(mockAttachmentDao).deleteById("id2")
    }
    
    @Test
    fun `test integrity validation with no issues`() = runTest {
        // Настраиваем мок для возврата пустого списка сирот
        `when`(mockAttachmentDao.listOrphans()).thenReturn(emptyList())
        
        val report = fileGc.validateIntegrity()
        
        assertEquals("Should have 0 total attachments", 0, report.totalAttachments)
        assertEquals("Should have 0 missing files", 0, report.missingFiles)
        assertEquals("Should have 0 corrupted files", 0, report.corruptedFiles)
        assertEquals("Should have 0 orphan attachments", 0, report.orphanAttachments)
    }
    
    @Test
    fun `test integrity validation with missing files`() = runTest {
        val orphan1 = createTestAttachment("id1", "file1.txt", "/path/file1.txt")
        val orphan2 = createTestAttachment("id2", "file2.txt", "/path/file2.txt")
        
        // Настраиваем мок для возврата списка сирот
        `when`(mockAttachmentDao.listOrphans()).thenReturn(listOf(orphan1, orphan2))
        
        // Настраиваем мок для проверки существования файлов
        `when`(mockAttachStorage.exists(orphan1)).thenReturn(false) // Файл отсутствует
        `when`(mockAttachStorage.exists(orphan2)).thenReturn(true)  // Файл существует
        
        val report = fileGc.validateIntegrity()
        
        assertEquals("Should have 2 total attachments", 2, report.totalAttachments)
        assertEquals("Should have 1 missing file", 1, report.missingFiles)
        assertEquals("Should have 0 corrupted files", 0, report.corruptedFiles)
        assertEquals("Should have 2 orphan attachments", 2, report.orphanAttachments)
    }
    
    private fun createTestAttachment(
        id: String,
        name: String,
        path: String,
        docId: String? = null
    ): com.example.docapp.data.db.entities.AttachmentEntity {
        return com.example.docapp.data.db.entities.AttachmentEntity(
            id = id,
            docId = docId,
            name = name,
            mime = "text/plain",
            size = 100L,
            sha256 = "test_hash_$id",
            path = path,
            uri = "content://test/$name",
            createdAt = System.currentTimeMillis()
        )
    }
}
