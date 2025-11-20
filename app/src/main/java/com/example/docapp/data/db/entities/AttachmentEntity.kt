package com.example.docapp.data.db.entities
import java.util.UUID
data class AttachmentEntity(
    val id: String = UUID.randomUUID().toString(),
    val docId: String?,
    val name: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val path: String,
    val uri: String,
    val createdAt: Long = System.currentTimeMillis()
)
