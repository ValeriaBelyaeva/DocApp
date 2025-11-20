package com.example.docapp.data.db.dao
import com.example.docapp.data.db.entities.AttachmentEntity
import kotlinx.coroutines.flow.Flow
interface AttachmentDao {
    suspend fun insert(attachment: AttachmentEntity): Unit
    suspend fun insertAll(attachments: List<AttachmentEntity>): Unit
    suspend fun listByDoc(docId: String): List<AttachmentEntity>
    fun observeByDoc(docId: String): Flow<List<AttachmentEntity>>
    suspend fun deleteById(id: String): Unit
    suspend fun bindToDoc(ids: List<String>, docId: String): Unit
    suspend fun listOrphans(): List<AttachmentEntity>
    suspend fun deleteByDocId(docId: String): Unit
    suspend fun getById(id: String): AttachmentEntity?
    suspend fun findBySha256(sha256: String): List<AttachmentEntity>
    suspend fun countByDoc(docId: String): Int
}
