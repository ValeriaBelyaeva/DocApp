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
    
        // Загружаем вложения при изменении docId
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
        // Заголовок и кнопки управления
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
                // Кнопка добавления фото
                IconButton(
                    onClick = {
                        // Открываем галерею для выбора фото
                        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        intent.type = "image/*"
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        context.startActivity(Intent.createChooser(intent, "Select photos"))
                    }
                ) {
                    Icon(Icons.Default.Photo, contentDescription = "Add photo")
                }
                
                // Кнопка добавления PDF
                IconButton(
                    onClick = {
                        // Открываем файловый менеджер для выбора PDF
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "application/pdf"
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        context.startActivity(Intent.createChooser(intent, "Select PDFs"))
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Add PDF")
                }
                
                // Кнопка очистки сирот
                IconButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val result = attachmentInteractors.cleanupOrphans()
                                if (result.deletedFiles > 0) {
                                    ErrorHandler.showSuccess("Removed ${result.deletedFiles} unused files")
                                    onAttachmentsChanged()
                                } else {
                                    ErrorHandler.showInfo("No orphan files found")
                                }
                            } catch (e: Exception) {
                                ErrorHandler.showError("Cleanup failed: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clean unused files")
                }
            }
        }
        
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        
        Spacer(modifier = Modifier.height(AppDimens.spaceSm))
        
        // Список вложений
        if (attachments.isEmpty()) {
            Text(
                text = "No attachments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                )
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(attachments) { attachment ->
                    AttachmentItem(
                        attachment = attachment,
                        onOpen = { openAttachment(context, attachment) },
                        onShare = { showShareDialog = listOf(attachment) },
                        onDelete = { showDeleteDialog = attachment }
                    )
                }
            }
        }
        
        // Кнопка массового шаринга
        if (attachments.size > 1) {
            OutlinedButton(
                onClick = { showShareDialog = attachments },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                Text("Share all (${attachments.size})")
            }
        }
    }
    
    // Диалог удаления
    showDeleteDialog?.let { attachment ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete attachment") },
            text = { Text("Delete \"${attachment.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                attachmentInteractors.delete(attachment.id)
                                attachments = attachments.filter { it.id != attachment.id }
                                onAttachmentsChanged()
                                showDeleteDialog = null
                            } catch (e: Exception) {
                                ErrorHandler.showError("Failed to delete attachment: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Диалог шаринга
    showShareDialog?.let { attachmentsToShare ->
        AlertDialog(
            onDismissRequest = { showShareDialog = null },
            title = { Text("Share attachments") },
            text = { Text("Open attachments for sharing?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        shareAttachments(context, attachmentsToShare)
                        showShareDialog = null
                    }
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AttachmentItem(
    attachment: AttachmentEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка типа файла
            Icon(
                imageVector = when {
                    attachment.mime.startsWith("image/") -> Icons.Default.Image
                    attachment.mime == "application/pdf" -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.AttachFile
                },
                contentDescription = null,
                tint = AppColors.iconAccent()
            )
            
            Spacer(modifier = Modifier.width(AppDimens.spaceMd))
            
            // Информация о файле
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${attachment.mime} • ${formatFileSize(attachment.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Кнопки действий
            Row {
                IconButton(onClick = onOpen) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = AppColors.iconAccent())
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = AppColors.iconAccent())
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.iconAccent())
                }
            }
        }
    }
}

private fun openAttachment(context: android.content.Context, attachment: AttachmentEntity) {
    try {
        AppLogger.log("AttachmentComponents", "Opening attachment: ${attachment.name}")
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(attachment.uri), attachment.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(intent, 0)
        
        if (activities.isNotEmpty()) {
            context.startActivity(intent)
            AppLogger.log("AttachmentComponents", "Attachment opened successfully")
            ErrorHandler.showSuccess("File opened: ${attachment.name}")
        } else {
            ErrorHandler.showError("No apps can open files of type ${attachment.mime}")
        }
        
    } catch (e: Exception) {
        AppLogger.log("AttachmentComponents", "ERROR: Failed to open attachment: ${e.message}")
        ErrorHandler.showError("Failed to open file: ${e.message}")
    }
}

private fun shareAttachments(context: android.content.Context, attachments: List<AttachmentEntity>) {
    try {
        AppLogger.log("AttachmentComponents", "Sharing ${attachments.size} attachments")
        
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
