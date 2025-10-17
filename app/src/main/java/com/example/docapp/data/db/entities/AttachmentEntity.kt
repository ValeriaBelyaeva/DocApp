package com.example.docapp.data.db.entities

import java.util.UUID

data class AttachmentEntity(
    val id: String = UUID.randomUUID().toString(),
    val docId: String?,                 // null — временно не привязан
    val name: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val path: String,                   // /data/data/<pkg>/files/attachments/...
    val uri: String,                    // content://<pkg>.fileprovider/attachments/...
    val createdAt: Long = System.currentTimeMillis()
)
