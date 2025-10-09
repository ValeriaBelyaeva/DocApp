package com.example.docapp.domain

import android.net.Uri

// Settings
data class Settings(
    val version: String,
    val pinHash: ByteArray,
    val pinSalt: ByteArray,
    val dbKeySalt: ByteArray
)

// Folders
data class Folder(val id: String, val parentId: String?, val name: String, val ord: Int)

// Templates
data class Template(
    val id: String, val name: String, val isPinned: Boolean, val pinnedOrder: Int?,
    val createdAt: Long, val updatedAt: Long
)
data class TemplateField(
    val id: String, val templateId: String, val name: String, val type: FieldType, val ord: Int
)

// Documents
data class Document(
    val id: String,
    val templateId: String?,
    val folderId: String?,
    val name: String,
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
    val valueCipher: ByteArray?, // пока незашифровано, но интерфейс готов
    val preview: String?,
    val isSecret: Boolean,
    val ord: Int
)

enum class AttachmentKind { photo, pdf }
data class Attachment(
    val id: String,
    val documentId: String,
    val kind: AttachmentKind,
    val fileName: String?,
    val uri: Uri,
    val createdAt: Long
)

enum class FieldType { text }
