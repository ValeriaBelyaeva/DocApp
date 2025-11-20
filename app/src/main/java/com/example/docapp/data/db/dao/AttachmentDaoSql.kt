package com.example.docapp.data.db.dao
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.data.db.entities.AttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
class AttachmentDaoSql(private val db: SQLiteDatabase) : AttachmentDao {
    private val docAttachments = mutableMapOf<String, MutableStateFlow<List<AttachmentEntity>>>()
    override suspend fun insert(attachment: AttachmentEntity): Unit = withContext(Dispatchers.IO) {
        try {
            val cv = ContentValues().apply {
                put("id", attachment.id)
                put("docId", attachment.docId)
                put("name", attachment.name)
                put("mime", attachment.mime)
                put("size", attachment.size)
                put("sha256", attachment.sha256)
                put("path", attachment.path)
                put("uri", attachment.uri)
                put("createdAt", attachment.createdAt)
            }
            val result = db.insert("attachments_new", null, cv)
            if (result == -1L) {
                throw RuntimeException("Failed to insert attachment")
            }
            AppLogger.log("AttachmentDaoSql", "Attachment inserted: ${attachment.name}")
            attachment.docId?.let { emitDocAttachments(it) }
        } catch (e: Exception) {
            AppLogger.log("AttachmentDaoSql", "ERROR: Failed to insert attachment: ${e.message}")
            ErrorHandler.showError("Failed to save attachment: ${e.message}")
            throw e
        }
    }
    override suspend fun insertAll(attachments: List<AttachmentEntity>): Unit = withContext(Dispatchers.IO) {
        db.beginTransaction()
        try {
            attachments.forEach { attachment ->
                val cv = ContentValues().apply {
                    put("id", attachment.id)
                    put("docId", attachment.docId)
                    put("name", attachment.name)
                    put("mime", attachment.mime)
                    put("size", attachment.size)
                    put("sha256", attachment.sha256)
                    put("path", attachment.path)
                    put("uri", attachment.uri)
                    put("createdAt", attachment.createdAt)
                }
                db.insert("attachments_new", null, cv)
            }
            db.setTransactionSuccessful()
            AppLogger.log("AttachmentDaoSql", "Inserted ${attachments.size} attachments")
            attachments.mapNotNull { it.docId }.distinct().forEach { docId ->
                docAttachments[docId]?.value = emptyList()
            }
        } catch (e: Exception) {
            AppLogger.log("AttachmentDaoSql", "ERROR: Failed to insert attachments: ${e.message}")
            ErrorHandler.showError("Failed to save attachments: ${e.message}")
            throw e
        } finally {
            db.endTransaction()
        }
    }
    override suspend fun listByDoc(docId: String): List<AttachmentEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AttachmentEntity>()
        db.rawQuery(
            "SELECT * FROM attachments_new WHERE docId = ? ORDER BY createdAt DESC",
            arrayOf(docId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.toAttachmentEntity())
            }
        }
        result
    }
    override fun observeByDoc(docId: String): Flow<List<AttachmentEntity>> {
        return docAttachments.getOrPut(docId) {
            MutableStateFlow<List<AttachmentEntity>>(emptyList())
        }.asStateFlow()
    }
    override suspend fun deleteById(id: String): Unit = withContext(Dispatchers.IO) {
        try {
            val docId = db.rawQuery("SELECT docId FROM attachments_new WHERE id = ?", arrayOf(id)).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            val deleted = db.delete("attachments_new", "id = ?", arrayOf(id))
            if (deleted > 0) {
                AppLogger.log("AttachmentDaoSql", "Attachment deleted: $id")
                docId?.let { emitDocAttachments(it) }
            } else {
                AppLogger.log("AttachmentDaoSql", "No attachment found to delete: $id")
            }
        } catch (e: Exception) {
            AppLogger.log("AttachmentDaoSql", "ERROR: Failed to delete attachment: ${e.message}")
            ErrorHandler.showError("Failed to delete attachment: ${e.message}")
            throw e
        }
    }
    override suspend fun bindToDoc(ids: List<String>, docId: String): Unit = withContext(Dispatchers.IO) {
        db.beginTransaction()
        try {
            ids.forEach { id ->
                val cv = ContentValues().apply { put("docId", docId) }
                db.update("attachments_new", cv, "id = ?", arrayOf(id))
            }
            db.setTransactionSuccessful()
            AppLogger.log("AttachmentDaoSql", "Bound ${ids.size} attachments to document: $docId")
            emitDocAttachments(docId)
        } catch (e: Exception) {
            AppLogger.log("AttachmentDaoSql", "ERROR: Failed to bind attachments: ${e.message}")
            ErrorHandler.showError("Failed to bind attachments: ${e.message}")
            throw e
        } finally {
            db.endTransaction()
        }
    }
    override suspend fun listOrphans(): List<AttachmentEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AttachmentEntity>()
        try {
            db.rawQuery(
                """
                    SELECT a.* FROM attachments_new a
                    LEFT JOIN documents d ON a.docId = d.id
                    WHERE a.docId IS NULL OR d.id IS NULL
                """,
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(cursor.toAttachmentEntity())
                }
            }
            AppLogger.log("AttachmentDaoSql", "Found ${result.size} orphan attachments")
        } catch (e: Exception) {
            AppLogger.log("AttachmentDaoSql", "ERROR: Failed to list orphans: ${e.message}")
            ErrorHandler.showError("Failed to locate unused attachments: ${e.message}")
        }
        result
    }
    override suspend fun deleteByDocId(docId: String): Unit = withContext(Dispatchers.IO) {
        try {
            val deleted = db.delete("attachments_new", "docId = ?", arrayOf(docId))
            AppLogger.log("AttachmentDaoSql", "Deleted $deleted attachments for document: $docId")
            emitDocAttachments(docId)
        } catch (e: Exception) {
            AppLogger.log("AttachmentDaoSql", "ERROR: Failed to delete attachments by docId: ${e.message}")
            ErrorHandler.showError("Failed to delete document attachments: ${e.message}")
            throw e
        }
    }
    override suspend fun getById(id: String): AttachmentEntity? = withContext(Dispatchers.IO) {
        db.rawQuery("SELECT * FROM attachments_new WHERE id = ?", arrayOf(id)).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.toAttachmentEntity()
            } else null
        }
    }
    override suspend fun findBySha256(sha256: String): List<AttachmentEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AttachmentEntity>()
        db.rawQuery("SELECT * FROM attachments_new WHERE sha256 = ?", arrayOf(sha256)).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.toAttachmentEntity())
            }
        }
        result
    }
    override suspend fun countByDoc(docId: String): Int = withContext(Dispatchers.IO) {
        db.rawQuery("SELECT COUNT(*) FROM attachments_new WHERE docId = ?", arrayOf(docId)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }
    private suspend fun emitDocAttachments(docId: String) {
        docAttachments[docId]?.value = runCatching { listByDoc(docId) }.getOrDefault(emptyList())
    }
    private fun Cursor.toAttachmentEntity(): AttachmentEntity {
        return AttachmentEntity(
            id = getString(getColumnIndexOrThrow("id")),
            docId = getStringOrNull("docId"),
            name = getString(getColumnIndexOrThrow("name")),
            mime = getString(getColumnIndexOrThrow("mime")),
            size = getLong(getColumnIndexOrThrow("size")),
            sha256 = getString(getColumnIndexOrThrow("sha256")),
            path = getString(getColumnIndexOrThrow("path")),
            uri = getString(getColumnIndexOrThrow("uri")),
            createdAt = getLong(getColumnIndexOrThrow("createdAt"))
        )
    }
    private fun Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }
}
