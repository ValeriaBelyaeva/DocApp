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
import kotlinx.coroutines.launch

@Composable
fun AttachmentManager(
    docId: String?,
    onAttachmentsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useCases = ServiceLocator.useCases
    
    var attachments by remember { mutableStateOf<List<AttachmentEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<AttachmentEntity?>(null) }
    var showShareDialog by remember { mutableStateOf<List<AttachmentEntity>?>(null) }
    
        // Загружаем вложения при изменении docId
        LaunchedEffect(docId) {
            docId?.let { id ->
                try {
                    attachments = ServiceLocator.repos.attachments.getAttachmentsByDoc(id)
                } catch (e: Exception) {
                    AppLogger.log("AttachmentComponents", "ERROR: Failed to load attachments: ${e.message}")
                    ErrorHandler.showError("Не удалось загрузить вложения: ${e.message}")
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
                text = "ВЛОЖЕНИЯ (${attachments.size})",
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
                        context.startActivity(Intent.createChooser(intent, "Выберите фото"))
                    }
                ) {
                    Icon(Icons.Default.Photo, contentDescription = "Добавить фото")
                }
                
                // Кнопка добавления PDF
                IconButton(
                    onClick = {
                        // Открываем файловый менеджер для выбора PDF
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "application/pdf"
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        context.startActivity(Intent.createChooser(intent, "Выберите PDF"))
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Добавить PDF")
                }
                
                // Кнопка очистки сирот
                IconButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val result = useCases.cleanupOrphans()
                                if (result.deletedFiles > 0) {
                                    ErrorHandler.showSuccess("Удалено ${result.deletedFiles} неиспользуемых файлов")
                                    onAttachmentsChanged()
                                } else {
                                    ErrorHandler.showInfo("Неиспользуемые файлы не найдены")
                                }
                            } catch (e: Exception) {
                                ErrorHandler.showError("Ошибка очистки: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Очистить неиспользуемые")
                }
            }
        }
        
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Список вложений
        if (attachments.isEmpty()) {
            Text(
                text = "Нет вложений",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                Spacer(modifier = Modifier.width(8.dp))
                Text("Поделиться всеми (${attachments.size})")
            }
        }
    }
    
    // Диалог удаления
    showDeleteDialog?.let { attachment ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить вложение") },
            text = { Text("Вы уверены, что хотите удалить \"${attachment.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                useCases.deleteAttachment(attachment.id)
                                attachments = attachments.filter { it.id != attachment.id }
                                onAttachmentsChanged()
                                showDeleteDialog = null
                            } catch (e: Exception) {
                                ErrorHandler.showError("Ошибка удаления: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог шаринга
    showShareDialog?.let { attachmentsToShare ->
        AlertDialog(
            onDismissRequest = { showShareDialog = null },
            title = { Text("Поделиться вложениями") },
            text = { Text("Открыть вложения для шаринга?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        shareAttachments(context, attachmentsToShare)
                        showShareDialog = null
                    }
                ) {
                    Text("Поделиться")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = null }) {
                    Text("Отмена")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
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
                    Icon(Icons.Default.OpenInNew, contentDescription = "Открыть")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Поделиться")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
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
            ErrorHandler.showSuccess("Файл открыт: ${attachment.name}")
        } else {
            ErrorHandler.showError("Нет приложений для открытия файла типа ${attachment.mime}")
        }
        
    } catch (e: Exception) {
        AppLogger.log("AttachmentComponents", "ERROR: Failed to open attachment: ${e.message}")
        ErrorHandler.showError("Не удалось открыть файл: ${e.message}")
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
            context.startActivity(Intent.createChooser(intent, "Поделиться файлами"))
            AppLogger.log("AttachmentComponents", "Share intent launched successfully")
            ErrorHandler.showSuccess("Файлы готовы для шаринга")
        } else {
            ErrorHandler.showError("Нет приложений для шаринга файлов")
        }
        
    } catch (e: Exception) {
        AppLogger.log("AttachmentComponents", "ERROR: Failed to share attachments: ${e.message}")
        ErrorHandler.showError("Не удалось поделиться файлами: ${e.message}")
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
