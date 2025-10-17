package com.example.docapp.ui.document

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.ErrorHandler
import com.example.docapp.domain.Attachment
import kotlinx.coroutines.launch

/**
 * Экраны документов с интеграцией новой системы вложений
 */

@Composable
fun DocumentViewScreen(
    docId: String,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    val useCases = ServiceLocator.useCases
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var fullDoc by remember { mutableStateOf<com.example.docapp.domain.DocumentRepository.FullDocument?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(docId) {
        try {
            val doc = useCases.getDoc(docId)
            fullDoc = doc
            useCases.touchOpened(docId)
        } catch (e: Exception) {
            ErrorHandler.showError("Не удалось загрузить документ: ${e.message}")
        }
    }

    val doc = fullDoc
    if (doc == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Загрузка...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок с названием документа
        Text(
            text = doc.doc.name,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Поля документа
        if (doc.fields.isNotEmpty()) {
            Text(
                text = "Поля документа:",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            doc.fields.forEach { field ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = field.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = field.preview ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Отображение прикрепленных файлов (только просмотр, без возможности удаления)
        if (doc.photos.isNotEmpty() || doc.pdfs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Фотографии - каждая в отдельной строке
            doc.photos.forEach { photo ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = "Фото",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                            Text(
                        text = photo.displayName ?: "Фото",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // PDF файлы - только кнопка "Открыть"
            doc.pdfs.forEach { pdf ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pdf.displayName ?: "PDF",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Только кнопка "Открыть"
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                intent.setDataAndType(android.net.Uri.parse(pdf.uri.toString()), "application/pdf")
                                intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                context.startActivity(android.content.Intent.createChooser(intent, "Открыть PDF"))
                            } catch (e: Exception) {
                                ErrorHandler.showError("Не удалось открыть PDF: ${e.message}")
                            }
                        }
                    ) {
                        Text("Открыть")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // В режиме просмотра не показываем кнопку добавления файлов
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Кнопки управления
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Редактировать",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Удалить")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить документ") },
            text = { Text("Вы уверены, что хотите удалить этот документ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                    scope.launch {
                            try {
                                useCases.deleteDoc(docId)
                                ErrorHandler.showSuccess("Документ удален")
                        onDeleted()
                            } catch (e: Exception) {
                                ErrorHandler.showError("Не удалось удалить документ: ${e.message}")
                            }
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun DocumentEditScreen(
    existingDocId: String?,
    templateId: String?,
    folderId: String?,
    onSaved: (String) -> Unit
) {
    val useCases = ServiceLocator.useCases
    val repos = ServiceLocator.repos
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val fields = remember { mutableStateListOf<Pair<String, String>>() }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var newFieldName by remember { mutableStateOf("") }
    var importedAttachments by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentPhotos by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var currentPdfs by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    
    // Функции для работы с прикрепленными файлами
    val deletePhoto: (String) -> Unit = { photoId: String ->
        scope.launch {
            try {
                useCases.deleteAttachment(photoId)
                currentPhotos = currentPhotos.filter { it.id != photoId }
                // Также удаляем из списка импортированных файлов для новых документов
                if (existingDocId == null) {
                    importedAttachments = importedAttachments.filter { it != photoId }
                }
                ErrorHandler.showSuccess("Фотография удалена")
            } catch (e: Exception) {
                ErrorHandler.showError("Не удалось удалить фотографию: ${e.message}")
            }
        }
    }
    
    val deletePdf: (String) -> Unit = { pdfId: String ->
        scope.launch {
            try {
                useCases.deleteAttachment(pdfId)
                currentPdfs = currentPdfs.filter { it.id != pdfId }
                // Также удаляем из списка импортированных файлов для новых документов
                if (existingDocId == null) {
                    importedAttachments = importedAttachments.filter { it != pdfId }
                }
                ErrorHandler.showSuccess("PDF удален")
            } catch (e: Exception) {
                ErrorHandler.showError("Не удалось удалить PDF: ${e.message}")
            }
        }
    }
    
    val openPdf = { pdfUri: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.parse(pdfUri), "application/pdf")
            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(android.content.Intent.createChooser(intent, "Открыть PDF"))
        } catch (e: Exception) {
            ErrorHandler.showError("Не удалось открыть PDF: ${e.message}")
        }
    }

    LaunchedEffect(existingDocId, templateId) {
        if (existingDocId != null) {
            try {
                val doc = useCases.getDoc(existingDocId)
                name = doc?.doc?.name ?: ""
                fields.clear()
                doc?.fields?.forEach { field ->
                    fields.add(field.name to (field.valueCipher?.decodeToString() ?: ""))
                }
                // Загружаем прикрепленные файлы
                currentPhotos = doc?.photos ?: emptyList()
                currentPdfs = doc?.pdfs ?: emptyList()
            } catch (e: Exception) {
                ErrorHandler.showError("Не удалось загрузить документ: ${e.message}")
            }
        } else {
            // Для новых документов инициализируем пустые списки
            name = ""
            fields.clear()
            currentPhotos = emptyList()
            currentPdfs = emptyList()
            importedAttachments = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (existingDocId != null) "Редактирование документа" else "Создание документа",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название документа") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Управление полями документа
        Text(
            text = "Поля документа:",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Список полей - единые поля с названием и содержимым
        fields.forEachIndexed { index, (fieldName, fieldValue) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Единое поле: название + содержимое (такая же высота как у названия документа)
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        fields[index] = fieldName to newValue
                    },
                    label = { Text(fieldName) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                
                // Кнопка удаления поля
                IconButton(
                    onClick = { fields.removeAt(index) }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить поле",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Кнопка добавления поля
        OutlinedButton(
            onClick = { 
                newFieldName = ""
                showAddFieldDialog = true 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Добавить поле")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    // Отменяем создание/редактирование документа
                    name = ""
                    fields.clear()
                },
                enabled = !isSaving
            ) {
                Text("Отмена")
            }
            
            Button(
            onClick = {
                    if (name.isNotBlank()) {
                scope.launch {
                            isSaving = true
                            try {
                                val id = if (existingDocId != null) {
                                    // Обновляем существующий документ
                                    try {
                                        val existingDoc = useCases.getDoc(existingDocId)
                                        if (existingDoc != null) {
                                            useCases.updateDoc(existingDoc.copy(
                                                doc = existingDoc.doc.copy(name = name),
                                                fields = existingDoc.fields // Пока оставляем существующие поля
                                            ))
                                            existingDocId
                                        } else {
                                            throw Exception("Документ не найден")
                                        }
                                    } catch (e: Exception) {
                                        throw Exception("Не удалось обновить документ: ${e.message}")
                                    }
                                } else {
                                    // Создаем новый документ
                                    val newDocId = useCases.createDoc(
                                        tplId = templateId,
                                        folderId = folderId,
                                        name = name,
                                        fields = fields.toList(),
                                        photos = emptyList(), // Файлы добавляются через ImportAttachmentsButton
                                        pdfUris = emptyList()    // Файлы добавляются через ImportAttachmentsButton
                                    )
                                    
                                    // Привязываем импортированные файлы к новому документу
                                    if (importedAttachments.isNotEmpty()) {
                                        useCases.bindAttachmentsToDoc(importedAttachments, newDocId)
                                    }
                                    
                                    newDocId
                                }
                                ErrorHandler.showSuccess("Документ сохранен")
                                onSaved(id)
                            } catch (e: Exception) {
                                ErrorHandler.showError("Не удалось сохранить документ: ${e.message}")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = !isSaving && name.isNotBlank()
            ) {
                Text(if (isSaving) "Сохранение..." else "Сохранить")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Кнопки импорта файлов - работает и для создания, и для редактирования
        ImportAttachmentsButton(
            docId = existingDocId, // null для новых документов, но все равно можно прикреплять файлы
            onImportComplete = { attachmentIds ->
                if (existingDocId == null) {
                    // Для новых документов сохраняем ID файлов
                    importedAttachments = importedAttachments + attachmentIds
                    
                    // Немедленно показываем импортированные файлы
                    scope.launch {
                        try {
                            // Получаем информацию о прикрепленных файлах из репозитория
                            val attachments = attachmentIds.mapNotNull { id ->
                                try {
                                    // Преобразуем AttachmentEntity в Attachment
                                    val entity = repos.attachments.getAttachment(id)
                                    if (entity != null) {
                                        // Определяем тип файла по MIME типу
                                        val kind = when {
                                            entity.mime.startsWith("image/") -> com.example.docapp.domain.AttachmentKind.photo
                                            entity.mime == "application/pdf" -> com.example.docapp.domain.AttachmentKind.pdf
                                            else -> com.example.docapp.domain.AttachmentKind.photo // По умолчанию фото
                                        }
                                        
                                        com.example.docapp.domain.Attachment(
                                            id = entity.id,
                                            documentId = entity.docId ?: "", // Временно пустой для новых документов
                                            kind = kind,
                                            fileName = entity.name,
                                            displayName = entity.name, // Используем name как displayName
                                            uri = android.net.Uri.parse(entity.uri),
                                            createdAt = entity.createdAt,
                                            requiresPersist = false // Файлы уже сохранены
                                        )
                                    } else null
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            // Разделяем на фото и PDF
                            val photos = attachments.filter { it.kind == com.example.docapp.domain.AttachmentKind.photo }
                            val pdfs = attachments.filter { it.kind == com.example.docapp.domain.AttachmentKind.pdf }
                            
                            currentPhotos = currentPhotos + photos
                            currentPdfs = currentPdfs + pdfs
                        } catch (e: Exception) {
                            ErrorHandler.showError("Не удалось обновить список файлов: ${e.message}")
                        }
                    }
                } else {
                    // Для существующих документов перезагружаем файлы
                    scope.launch {
                        try {
                            val doc = useCases.getDoc(existingDocId)
                            currentPhotos = doc?.photos ?: emptyList()
                            currentPdfs = doc?.pdfs ?: emptyList()
                        } catch (e: Exception) {
                            ErrorHandler.showError("Не удалось обновить список файлов: ${e.message}")
                        }
                    }
                }
                ErrorHandler.showSuccess("Файлы импортированы успешно")
            }
        )
        
        // Отображение прикрепленных файлов - ПРОСТОЙ СПИСОК ПОСЛЕ КНОПОК
        if (currentPhotos.isNotEmpty() || currentPdfs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Фотографии - каждая в отдельной строке
            currentPhotos.forEach { photo ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = "Фото",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = photo.displayName ?: "Фото",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Кнопка удалить под фотографией
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(32.dp)) // Отступ под иконку
                    OutlinedButton(
                        onClick = { deletePhoto(photo.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("Удалить")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // PDF файлы - кнопки "Открыть" и "Удалить" в ряду
            currentPdfs.forEach { pdf ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pdf.displayName ?: "PDF",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Кнопки "Открыть" и "Удалить" в ряду
                    Button(
                        onClick = { openPdf(pdf.uri.toString()) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Открыть")
                    }
                    
                    OutlinedButton(
                        onClick = { deletePdf(pdf.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Удалить")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Используйте кнопку выше для добавления фото и PDF файлов",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Диалог добавления поля
    if (showAddFieldDialog) {
        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            title = { Text("Добавить поле") },
            text = {
                OutlinedTextField(
                    value = newFieldName,
                    onValueChange = { newFieldName = it },
                    label = { Text("Название поля") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFieldName.isNotBlank()) {
                            fields.add(newFieldName to "")
                            newFieldName = ""
                            showAddFieldDialog = false
                        }
                    },
                    enabled = newFieldName.isNotBlank()
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFieldDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}


// -------------------------------
// ПРОСТАЯ СИСТЕМА ОТОБРАЖЕНИЯ ФАЙЛОВ
// -------------------------------
// Все компоненты удалены - используется простой inline код в экранах
