package com.example.docapp

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.docapp.data.storage.AttachStorageImpl
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.File

class AttachmentStorageTest {
    
    private lateinit var context: Context
    private lateinit var attachStorage: AttachStorageImpl
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        attachStorage = AttachStorageImpl(context)
    }
    
    @Test
    fun `test uniqueFileName generation`() = runTest {
        // Создаем тестовые файлы для проверки уникальности имен
        val testDir = File(context.filesDir, "test_attachments")
        testDir.mkdirs()
        
        try {
            // Создаем файл с именем "test.txt"
            val existingFile = File(testDir, "test.txt")
            existingFile.createNewFile()
            
            // Проверяем, что следующее имя будет уникальным
            val uniqueName = attachStorage.generateUniqueFileName("test.txt")
            assertEquals("test(1).txt", uniqueName)
            
            // Создаем файл с именем "test(1).txt"
            val secondFile = File(testDir, "test(1).txt")
            secondFile.createNewFile()
            
            // Проверяем, что следующее имя будет "test(2).txt"
            // Но сначала нужно создать временную копию AttachStorage с testDir
            val testAttachStorage = AttachStorageImpl(context)
            // Меняем директорию для тестирования
            val originalDir = testAttachStorage.attachmentsDir
            // Это не работает напрямую, поэтому создадим файлы в правильной директории
            
            val thirdFile = File(testDir, "test(2).txt")
            thirdFile.createNewFile()
            
            val thirdUniqueName = attachStorage.generateUniqueFileName("test.txt")
            assertEquals("test(3).txt", thirdUniqueName)
            
        } finally {
            // Очищаем тестовые файлы
            testDir.deleteRecursively()
        }
    }
    
    @Test
    fun `test file exists check`() {
        // Создаем тестовый файл
        val testFile = File(context.filesDir, "test_file.txt")
        testFile.writeText("test content")
        
        try {
            val attachment = com.example.docapp.data.db.entities.AttachmentEntity(
                id = "test_id",
                docId = null,
                name = "test_file.txt",
                mime = "text/plain",
                size = testFile.length(),
                sha256 = "test_hash",
                path = testFile.absolutePath,
                uri = "content://test/test_file.txt",
                createdAt = System.currentTimeMillis()
            )
            
            assertTrue("File should exist", attachStorage.exists(attachment))
            
            // Удаляем файл
            testFile.delete()
            
            assertFalse("File should not exist after deletion", attachStorage.exists(attachment))
            
        } finally {
            // Убеждаемся, что файл удален
            if (testFile.exists()) {
                testFile.delete()
            }
        }
    }
    
    @Test
    fun `test file deletion`() {
        // Создаем тестовый файл
        val testFile = File(context.filesDir, "test_delete.txt")
        testFile.writeText("test content")
        
        val attachment = com.example.docapp.data.db.entities.AttachmentEntity(
            id = "test_id",
            docId = null,
            name = "test_delete.txt",
            mime = "text/plain",
            size = testFile.length(),
            sha256 = "test_hash",
            path = testFile.absolutePath,
            uri = "content://test/test_delete.txt",
            createdAt = System.currentTimeMillis()
        )
        
        assertTrue("File should exist before deletion", testFile.exists())
        
        val deleted = attachStorage.deletePhysical(attachment)
        
        assertTrue("Deletion should succeed", deleted)
        assertFalse("File should not exist after deletion", testFile.exists())
    }
    
    @Test
    fun `test fileFor returns correct file`() {
        val testPath = "/test/path/file.txt"
        val attachment = com.example.docapp.data.db.entities.AttachmentEntity(
            id = "test_id",
            docId = null,
            name = "file.txt",
            mime = "text/plain",
            size = 100L,
            sha256 = "test_hash",
            path = testPath,
            uri = "content://test/file.txt",
            createdAt = System.currentTimeMillis()
        )
        
        val file = attachStorage.fileFor(attachment)
        assertEquals("File path should match", testPath, file.absolutePath)
    }
}
