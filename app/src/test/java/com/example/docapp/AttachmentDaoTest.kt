package com.example.docapp
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.docapp.data.db.dao.AttachmentDaoSql
import com.example.docapp.data.db.entities.AttachmentEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
class AttachmentDaoTest {
    private lateinit var database: SQLiteDatabase
    private lateinit var attachmentDao: AttachmentDaoSql
    @Before
    fun setUp() {
        database = SQLiteDatabase.create(null)
        database.execSQL("""
            CREATE TABLE attachments_new(
                id TEXT PRIMARY KEY,
                docId TEXT,
                name TEXT NOT NULL,
                mime TEXT NOT NULL,
                size INTEGER NOT NULL,
                sha256 TEXT NOT NULL,
                path TEXT NOT NULL,
                uri TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)
        attachmentDao = AttachmentDaoSql(database)
    }
    @After
    fun tearDown() {
        database.close()
    }
    @Test
    fun `test insert attachment`() = runTest {
        val attachment = AttachmentEntity(
            id = "test_id_1",
            docId = "doc_1",
            name = "test_file.txt",
            mime = "text/plain",
            size = 100L,
            sha256 = "test_hash_1",
            path = "/test/path/file.txt",
            uri = "content:
            createdAt = System.currentTimeMillis()
        )
        attachmentDao.insert(attachment)
        val retrieved = attachmentDao.getById("test_id_1")
        assertNotNull("Attachment should be found", retrieved)
        assertEquals("Name should match", "test_file.txt", retrieved?.name)
        assertEquals("DocId should match", "doc_1", retrieved?.docId)
    }
    @Test
    fun `test insert multiple attachments`() = runTest {
        val attachments = listOf(
            AttachmentEntity(
                id = "test_id_1",
                docId = "doc_1",
                name = "file1.txt",
                mime = "text/plain",
                size = 100L,
                sha256 = "hash_1",
                path = "/test/path/file1.txt",
                uri = "content:
                createdAt = System.currentTimeMillis()
            ),
            AttachmentEntity(
                id = "test_id_2",
                docId = "doc_1",
                name = "file2.txt",
                mime = "text/plain",
                size = 200L,
                sha256 = "hash_2",
                path = "/test/path/file2.txt",
                uri = "content:
                createdAt = System.currentTimeMillis()
            )
        )
        attachmentDao.insertAll(attachments)
        val retrieved = attachmentDao.listByDoc("doc_1")
        assertEquals("Should have 2 attachments", 2, retrieved.size)
        assertTrue("Should contain file1.txt", retrieved.any { it.name == "file1.txt" })
        assertTrue("Should contain file2.txt", retrieved.any { it.name == "file2.txt" })
    }
    @Test
    fun `test delete attachment`() = runTest {
        val attachment = AttachmentEntity(
            id = "test_id_1",
            docId = "doc_1",
            name = "test_file.txt",
            mime = "text/plain",
            size = 100L,
            sha256 = "test_hash_1",
            path = "/test/path/file.txt",
            uri = "content:
            createdAt = System.currentTimeMillis()
        )
        attachmentDao.insert(attachment)
        val retrieved = attachmentDao.getById("test_id_1")
        assertNotNull("Attachment should exist before deletion", retrieved)
        attachmentDao.deleteById("test_id_1")
        val deleted = attachmentDao.getById("test_id_1")
        assertNull("Attachment should not exist after deletion", deleted)
    }
    @Test
    fun `test bind attachments to document`() = runTest {
        val attachments = listOf(
            AttachmentEntity(
                id = "test_id_1",
                docId = null,
                name = "file1.txt",
                mime = "text/plain",
                size = 100L,
                sha256 = "hash_1",
                path = "/test/path/file1.txt",
                uri = "content:
                createdAt = System.currentTimeMillis()
            ),
            AttachmentEntity(
                id = "test_id_2",
                docId = null,
                name = "file2.txt",
                mime = "text/plain",
                size = 200L,
                sha256 = "hash_2",
                path = "/test/path/file2.txt",
                uri = "content:
                createdAt = System.currentTimeMillis()
            )
        )
        attachmentDao.insertAll(attachments)
        attachmentDao.bindToDoc(listOf("test_id_1", "test_id_2"), "doc_1")
        val retrieved = attachmentDao.listByDoc("doc_1")
        assertEquals("Should have 2 attachments bound to doc_1", 2, retrieved.size)
        retrieved.forEach { attachment ->
            assertEquals("All attachments should be bound to doc_1", "doc_1", attachment.docId)
        }
    }
    @Test
    fun `test find by SHA256`() = runTest {
        val attachment1 = AttachmentEntity(
            id = "test_id_1",
            docId = "doc_1",
            name = "file1.txt",
            mime = "text/plain",
            size = 100L,
            sha256 = "duplicate_hash",
            path = "/test/path/file1.txt",
            uri = "content:
            createdAt = System.currentTimeMillis()
        )
        val attachment2 = AttachmentEntity(
            id = "test_id_2",
            docId = "doc_2",
            name = "file2.txt",
            mime = "text/plain",
            size = 200L,
            sha256 = "duplicate_hash",
            path = "/test/path/file2.txt",
            uri = "content:
            createdAt = System.currentTimeMillis()
        )
        attachmentDao.insertAll(listOf(attachment1, attachment2))
        val duplicates = attachmentDao.findBySha256("duplicate_hash")
        assertEquals("Should find 2 duplicates", 2, duplicates.size)
        assertTrue("Should contain file1.txt", duplicates.any { it.name == "file1.txt" })
        assertTrue("Should contain file2.txt", duplicates.any { it.name == "file2.txt" })
    }
    @Test
    fun `test count by document`() = runTest {
        val attachments = listOf(
            AttachmentEntity(
                id = "test_id_1",
                docId = "doc_1",
                name = "file1.txt",
                mime = "text/plain",
                size = 100L,
                sha256 = "hash_1",
                path = "/test/path/file1.txt",
                uri = "content:
                createdAt = System.currentTimeMillis()
            ),
            AttachmentEntity(
                id = "test_id_2",
                docId = "doc_1",
                name = "file2.txt",
                mime = "text/plain",
                size = 200L,
                sha256 = "hash_2",
                path = "/test/path/file2.txt",
                uri = "content:
                createdAt = System.currentTimeMillis()
            ),
            AttachmentEntity(
                id = "test_id_3",
                docId = "doc_2",
                name = "file3.txt",
                mime = "text/plain",
                size = 300L,
                sha256 = "hash_3",
                path = "/test/path/file3.txt",
                uri = "content:
                createdAt = System.currentTimeMillis()
            )
        )
        attachmentDao.insertAll(attachments)
        val countDoc1 = attachmentDao.countByDoc("doc_1")
        val countDoc2 = attachmentDao.countByDoc("doc_2")
        val countDoc3 = attachmentDao.countByDoc("doc_3")
        assertEquals("doc_1 should have 2 attachments", 2, countDoc1)
        assertEquals("doc_2 should have 1 attachment", 1, countDoc2)
        assertEquals("doc_3 should have 0 attachments", 0, countDoc3)
    }
}
