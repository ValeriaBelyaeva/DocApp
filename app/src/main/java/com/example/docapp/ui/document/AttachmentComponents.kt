package com.example.docapp.ui.document
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ServiceLocator
import com.example.docapp.data.db.entities.AttachmentEntity
import com.example.docapp.ui.theme.GlassCard
import com.example.docapp.ui.theme.AppColors
import kotlinx.coroutines.launch
import com.example.docapp.ui.theme.AppDimens
@Composable
fun AttachmentManager(
    docId: String?,
    onAttachmentsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val domainInteractors = ServiceLocator.domain
    val attachmentInteractors = domainInteractors.attachments
    var attachments by remember { mutableStateOf<List<AttachmentEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<AttachmentEntity?>(null) }
    var showShareDialog by remember { mutableStateOf<List<AttachmentEntity>?>(null) }
        LaunchedEffect(docId) {
            docId?.let { id ->
                try {
                    attachments = attachmentInteractors.listByDocument(id)
                } catch (e: Exception) {
                    AppLogger.log("AttachmentComponents", "ERROR: Failed to load attachments: ${e.message}")
                    ErrorHandler.showError("Failed to load attachments: ${e.message}")
                    attachments = emptyList()
                }
            } ?: run {
                attachments = emptyList()
            }
        }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ATTACHMENTS (${attachments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "image/*"
                                val uris = attachments.map { Uri.parse(it.uri) }
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val packageManager = context.packageManager
                            val activities = packageManager.queryIntentActivities(intent, 0)
                            if (activities.isNotEmpty()) {
                                context.startActivity(Intent.createChooser(intent, "Share files"))
                                AppLogger.log("AttachmentComponents", "Share intent launched successfully")
                                ErrorHandler.showSuccess("Files ready to share")
                            } else {
                                ErrorHandler.showError("No apps available to share files")
                            }
                        } catch (e: Exception) {
                            AppLogger.log("AttachmentComponents", "ERROR: Failed to share attachments: ${e.message}")
                            ErrorHandler.showError("Failed to share files: ${e.message}")
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share attachments")
                }
            }
        }
    }
}
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
