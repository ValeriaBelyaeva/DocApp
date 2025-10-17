package com.example.docapp.domain.repo

import android.content.Context
import android.net.Uri
import com.example.docapp.data.db.entities.AttachmentEntity
import com.example.docapp.data.storage.AttachStorage
import com.example.docapp.data.storage.FileGc
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    suspend fun importAttachment(context: Context, uri: Uri): AttachmentEntity
    suspend fun importAttachments(context: Context, uris: List<Uri>): List<AttachmentEntity>
    suspend fun getAttachmentsByDoc(docId: String): List<AttachmentEntity>
    fun observeAttachmentsByDoc(docId: String): Flow<List<AttachmentEntity>>
    suspend fun deleteAttachment(id: String): Boolean
    suspend fun deleteAttachmentsByDoc(docId: String): Boolean
    suspend fun bindAttachmentsToDoc(attachmentIds: List<String>, docId: String)
    suspend fun getAttachment(id: String): AttachmentEntity?
    suspend fun findDuplicates(sha256: String): List<AttachmentEntity>
    suspend fun cleanupOrphans(): FileGc.CleanupResult
    suspend fun getAttachmentInputStream(context: Context, attachment: AttachmentEntity): java.io.InputStream?
    suspend fun getAttachmentFile(attachment: AttachmentEntity): java.io.File?
    suspend fun validateAttachmentIntegrity(attachment: AttachmentEntity): Boolean
}
