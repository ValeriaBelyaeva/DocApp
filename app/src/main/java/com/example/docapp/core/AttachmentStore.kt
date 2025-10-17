package com.example.docapp.core

import android.net.Uri

/**
 * Интерфейс для совместимости со старым кодом
 * Используется для обработки URI в существующих документах
 */
interface AttachmentStore {
    fun persist(uri: Uri): Uri
    fun release(uri: Uri)
}

/**
 * Реализация для совместимости со старым кодом
 * Обрабатывает URI для существующих документов
 */
class AttachmentStoreImpl(private val ctx: android.content.Context) : AttachmentStore {
    override fun persist(uri: Uri): Uri {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ctx.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) { /* ignore */ }
        return uri
    }

    override fun release(uri: Uri) {
        val perms: List<android.content.UriPermission> = ctx.contentResolver.persistedUriPermissions
        perms.firstOrNull { it.uri == uri }?.let { p ->
            val flags = (if (p.isReadPermission) android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                    (if (p.isWritePermission) android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
            try { ctx.contentResolver.releasePersistableUriPermission(uri, flags) } catch (_: Exception) {}
        }
    }
}
