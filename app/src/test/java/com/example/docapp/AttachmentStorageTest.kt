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
        val testDir = File(context.filesDir, "test_attachments")
        testDir.mkdirs()
        try {
            val existingFile = File(testDir, "test.txt")
            existingFile.createNewFile()
            val uniqueName = attachStorage.generateUniqueFileName("test.txt")
            assertEquals("test(1).txt", uniqueName)
            val secondFile = File(testDir, "test(1).txt")
            secondFile.createNewFile()
            val testAttachStorage = AttachStorageImpl(context)
            val originalDir = testAttachStorage.attachmentsDir
            val thirdFile = File(testDir, "test(2).txt")
            thirdFile.createNewFile()
            val thirdUniqueName = attachStorage.generateUniqueFileName("test.txt")
            assertEquals("test(3).txt", thirdUniqueName)
        } finally {
            testDir.deleteRecursively()
        }
    }
    @Test
    fun `test file exists check`() {
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
                uri = "content:
                createdAt = System.currentTimeMillis()
            )
            assertTrue("File should exist", attachStorage.exists(attachment))
            testFile.delete()
            assertFalse("File should not exist after deletion", attachStorage.exists(attachment))
        } finally {
            if (testFile.exists()) {
                testFile.delete()
            }
        }
    }
    @Test
    fun `test file deletion`() {
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
            uri = "content:
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
            uri = "content:
            createdAt = System.currentTimeMillis()
        )
        val file = attachStorage.fileFor(attachment)
        assertEquals("File path should match", testPath, file.absolutePath)
    }
}
