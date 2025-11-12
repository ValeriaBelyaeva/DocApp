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
import com.example.docapp.ui.theme.GlassCard
import com.example.docapp.ui.theme.AppDimens
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
                    ErrorHandler.showInfo("Importing ${uris.size} photos...")
                    
                    val result = useCases.importAttachments(context, docId, uris)
                    
                    importProgress = 1f
                    
                    if (result.failed == 0) {
                        ErrorHandler.showSuccess("All photos imported successfully")
                    } else {
                        ErrorHandler.showWarning("Import finished: ${result.successful} succeeded, ${result.failed} failed")
                    }
                    
                    onImportComplete(result.attachments.map { it.id })
                    
                } catch (e: Exception) {
                    AppLogger.log("ImportAttachments", "ERROR: Photo import failed: ${e.message}")
                    ErrorHandler.showError("Photo import failed: ${e.message}")
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
                    ErrorHandler.showInfo("Importing ${uris.size} documents...")
                    
                    val result = useCases.importAttachments(context, docId, uris)
                    
                    importProgress = 1f
                    
                    if (result.failed == 0) {
                        ErrorHandler.showSuccess("All documents imported successfully")
                    } else {
                        ErrorHandler.showWarning("Import finished: ${result.successful} succeeded, ${result.failed} failed")
                    }
                    
                    onImportComplete(result.attachments.map { it.id })
                    
                } catch (e: Exception) {
                    AppLogger.log("ImportAttachments", "ERROR: Document import failed: ${e.message}")
                    ErrorHandler.showError("Document import failed: ${e.message}")
                } finally {
                    isImporting = false
                    showProgressDialog = false
                }
            }
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
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
            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
            Text("Add photo")
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
            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
            Text("Add PDF")
        }
    }
    
    // Диалог прогресса импорта
    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = { /* cannot cancel */ },
            title = { Text("Importing files") },
            text = {
                Column {
                    Text("File import in progress...")
                    Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                    LinearProgressIndicator(
                        progress = { importProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { /* cannot cancel */ },
                    enabled = false
                ) {
                    Text("Cancel")
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
                    
                    ErrorHandler.showSuccess("Imported ${result.successful} photos")
                    
                } catch (e: Exception) {
                    AppLogger.log("ImportAttachments", "ERROR: Photo import failed: ${e.message}")
                    ErrorHandler.showError("Photo import failed: ${e.message}")
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
                    
                    ErrorHandler.showSuccess("Imported ${result.successful} documents")
                    
                } catch (e: Exception) {
                    AppLogger.log("ImportAttachments", "ERROR: Document import failed: ${e.message}")
                    ErrorHandler.showError("Document import failed: ${e.message}")
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
            text = "Import files for a new document",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(AppDimens.spaceSm))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
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
                Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                Text("Photos")
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
                Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                Text("PDF")
            }
        }
        
        // Показать импортированные файлы
        if (importedPhotos.isNotEmpty() || importedPdfs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.cardPaddingLarge)
                ) {
                    Text(
                        text = "Imported files:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    if (importedPhotos.isNotEmpty()) {
                        Text("Photos: ${importedPhotos.size}")
                    }
                    
                    if (importedPdfs.isNotEmpty()) {
                        Text("PDFs: ${importedPdfs.size}")
                    }
                    
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onImportComplete(importedPhotos, importedPdfs)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Use")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                importedPhotos = emptyList()
                                importedPdfs = emptyList()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}
