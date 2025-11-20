package com.example.docapp.domain
import android.net.Uri
data class Settings(
    val version: String,
    val pinHash: ByteArray,
    val pinSalt: ByteArray,
    val dbKeySalt: ByteArray
)
data class Folder(val id: String, val parentId: String?, val name: String, val ord: Int)
data class Template(
    val id: String, val name: String, val isPinned: Boolean, val pinnedOrder: Int?,
    val createdAt: Long, val updatedAt: Long
)
data class TemplateField(
    val id: String, val templateId: String, val name: String, val type: FieldType, val ord: Int
)
data class Document(
    val id: String,
    val templateId: String?,
    val folderId: String?,
    val name: String,
    val description: String,
    val isPinned: Boolean,
    val pinnedOrder: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long
)
data class DocumentField(
    val id: String,
    val documentId: String,
    val name: String,
    val valueCipher: ByteArray?,
    val preview: String?,
    val isSecret: Boolean,
    val ord: Int
)
enum class AttachmentKind { photo, pdf, pdfs }
data class Attachment(
    val id: String,
    val documentId: String,
    val kind: AttachmentKind,
    val fileName: String?,
    val displayName: String?,
    val uri: Uri,
    val createdAt: Long,
    val requiresPersist: Boolean = false
)
enum class FieldType { text }
