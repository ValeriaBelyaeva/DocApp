package com.example.docapp.ui.document

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ServiceLocator
import kotlinx.coroutines.launch

@Composable
fun ImportAttachmentsButton(
    docId: String?,
    onImportComplete: (List<String>) -> Unit = { }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useCases = ServiceLocator.useCases
    
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0f) }
    var showProgressDialog by remember { mutableStateOf(false) }
    
    // Пикер для множественных изображений
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                try {
                    isImporting = true
                    showProgressDialog = true
                    importProgress = 0f
                    
                    AppLogger.log("ImportAttachments", "Importing ${uris.size} photos")
                    ErrorHandler.showInfo("Импорт ${uris.size} фотографий...")
                    
                    val result = useCases.importAttachments(context, docId, uris)
                    
                    importProgress = 1f
                    
                    if (result.failed == 0) {
                        ErrorHandler.showSuccess("Все фотографии импортированы успешно")
                    } else {
                        ErrorHandler.showWarning("Импорт завершен: ${result.successful} успешно, ${result.failed} ошибок")
                    }
                    
                    onImportComplete(result.attachments.map { it.id })
                    
                } catch (e: Exception) {
                    AppLogger.log("ImportAttachments", "ERROR: Photo import failed: ${e.message}")
                    ErrorHandler.showError("Ошибка импорта фотографий: ${e.message}")
                } finally {
                    isImporting = false
                    showProgressDialog = false
                }
            }
        }
    }
    
    // Пикер для множественных документов (PDF)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                try {
                    isImporting = true
                    showProgressDialog = true
                    importProgress = 0f
                    
                    AppLogger.log("ImportAttachments", "Importing ${uris.size} documents")
                    ErrorHandler.showInfo("Импорт ${uris.size} документов...")
                    
                    val result = useCases.importAttachments(context, docId, uris)
                    
                    importProgress = 1f
                    
                    if (result.failed == 0) {
                        ErrorHandler.showSuccess("Все документы импортированы успешно")
                    } else {
                        ErrorHandler.showWarning("Импорт завершен: ${result.successful} успешно, ${result.failed} ошибок")
                    }
                    
                    onImportComplete(result.attachments.map { it.id })
                    
                } catch (e: Exception) {
                    AppLogger.log("ImportAttachments", "ERROR: Document import failed: ${e.message}")
                    ErrorHandler.showError("Ошибка импорта документов: ${e.message}")
                } finally {
                    isImporting = false
                    showProgressDialog = false
                }
            }
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка импорта фотографий
        OutlinedButton(
            onClick = {
                if (!isImporting) {
                    photoPickerLauncher.launch("image/*")
                }
            },
            enabled = !isImporting,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Photo, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить фото")
        }
        
        // Кнопка импорта PDF
        OutlinedButton(
            onClick = {
                if (!isImporting) {
                    documentPickerLauncher.launch(arrayOf("application/pdf"))
                }
            },
            enabled = !isImporting,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить PDF")
        }
    }
    
    // Диалог прогресса импорта
    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = { /* Нельзя отменить */ },
            title = { Text("Импорт файлов") },
            text = {
                Column {
                    Text("Импорт файлов в процессе...")
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = importProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { /* Нельзя отменить */ },
                    enabled = false
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ImportAttachmentsForNewDocument(
    onImportComplete: (photos: List<Uri>, pdfs: List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useCases = ServiceLocator.useCases
    
    var importedPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var importedPdfs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isImporting by remember { mutableStateOf(false) }
    
    // Пикер для множественных изображений
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                try {
                    isImporting = true
                    AppLogger.log("ImportAttachments", "Importing ${uris.size} photos for new document")
                    
                    // Импортируем фото без привязки к документу
                    val result = useCases.importAttachments(context, null, uris)
                    importedPhotos = uris
                    
                    ErrorHandler.showSuccess("Импортировано ${result.successful} фотографий")
                    
                } catch (e: Exception) {
                    AppLogger.log("ImportAttachments", "ERROR: Photo import failed: ${e.message}")
                    ErrorHandler.showError("Ошибка импорта фотографий: ${e.message}")
                } finally {
                    isImporting = false
                }
            }
        }
    }
    
    // Пикер для множественных документов (PDF)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                try {
                    isImporting = true
                    AppLogger.log("ImportAttachments", "Importing ${uris.size} documents for new document")
                    
                    // Импортируем PDF без привязки к документу
                    val result = useCases.importAttachments(context, null, uris)
                    importedPdfs = uris
                    
                    ErrorHandler.showSuccess("Импортировано ${result.successful} документов")
                    
                } catch (e: Exception) {
                    AppLogger.log("ImportAttachments", "ERROR: Document import failed: ${e.message}")
                    ErrorHandler.showError("Ошибка импорта документов: ${e.message}")
                } finally {
                    isImporting = false
                }
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Импорт файлов для нового документа",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Кнопка импорта фотографий
            OutlinedButton(
                onClick = {
                    if (!isImporting) {
                        photoPickerLauncher.launch("image/*")
                    }
                },
                enabled = !isImporting,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Photo, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Фото")
            }
            
            // Кнопка импорта PDF
            OutlinedButton(
                onClick = {
                    if (!isImporting) {
                        documentPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                },
                enabled = !isImporting,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PDF")
            }
        }
        
        // Показать импортированные файлы
        if (importedPhotos.isNotEmpty() || importedPdfs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Импортированные файлы:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    if (importedPhotos.isNotEmpty()) {
                        Text("Фото: ${importedPhotos.size}")
                    }
                    
                    if (importedPdfs.isNotEmpty()) {
                        Text("PDF: ${importedPdfs.size}")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onImportComplete(importedPhotos, importedPdfs)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Использовать")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                importedPhotos = emptyList()
                                importedPdfs = emptyList()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Очистить")
                        }
                    }
                }
            }
        }
    }
}
