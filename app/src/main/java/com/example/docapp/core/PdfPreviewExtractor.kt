package com.example.docapp.core
import android.content.Context
import android.net.Uri
object PdfPreviewExtractor {
    @Suppress("UNUSED_PARAMETER")
    suspend fun extractPreview(context: Context, uri: Uri, maxLines: Int = 5): String {
        return try {
            ""
        } catch (e: Exception) {
            AppLogger.log("PdfPreviewExtractor", "ERROR: Failed to extract PDF preview: ${e.message}")
            "Failed to read PDF"
        }
    }
}
