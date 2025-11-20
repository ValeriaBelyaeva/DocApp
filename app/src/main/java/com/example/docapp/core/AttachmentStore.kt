package com.example.docapp.core
import android.net.Uri

/**
 * Interface for managing persistent URI permissions for attachments.
 * Provides methods to persist and release URI permissions for long-term file access.
 * 
 * Works by managing Android's persistent URI permissions, allowing the app to access
 * files even after the user grants permission, without requiring re-granting.
 */
interface AttachmentStore {
    /**
     * Makes a URI permission persistent, allowing long-term access to the file.
     * 
     * arguments:
     *     uri - Uri: The URI to make persistent
     * 
     * return:
     *     uri - Uri: The same URI that was made persistent
     */
    fun persist(uri: Uri): Uri
    
    /**
     * Releases a persistent URI permission, revoking long-term access to the file.
     * 
     * arguments:
     *     uri - Uri: The URI to release permissions for
     * 
     * return:
     *     Unit - No return value
     */
    fun release(uri: Uri)
}

/**
 * Implementation of AttachmentStore interface using Android ContentResolver.
 * Manages persistent URI permissions for file attachments.
 * 
 * Works by using ContentResolver to take and release persistent URI permissions,
 * allowing the app to access files across app restarts.
 * 
 * arguments:
 *     ctx - Context: Android context for accessing ContentResolver
 */
class AttachmentStoreImpl(private val ctx: android.content.Context) : AttachmentStore {
    override fun persist(uri: Uri): Uri {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ctx.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {  }
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
