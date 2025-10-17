package com.example.docapp.ui.document

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.AppLogger
import com.example.docapp.core.PdfPreviewExtractor
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
    val clipboardManager = LocalClipboardManager.current
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

    // Функция для открытия фотографий
    val openPhoto = { photoUri: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.parse(photoUri), "image/*")
            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(android.content.Intent.createChooser(intent, "Открыть фото"))
        } catch (e: Exception) {
            ErrorHandler.showError("Не удалось открыть фото: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = doc.doc.name,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Поля документа (только если есть поля)
        if (doc.fields.isNotEmpty()) {
            Text(
                text = "Поля документа:",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Список полей - единые поля с названием и содержимым
            doc.fields.forEachIndexed { index, field ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Единое поле: название + содержимое (такая же высота как у названия документа)
                OutlinedTextField(
                    value = field.valueCipher?.decodeToString() ?: "",
                    onValueChange = { /* Не редактируем в режиме просмотра */ },
                    label = { Text(field.name) },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                // Кнопка копирования поля
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(field.valueCipher?.decodeToString() ?: ""))
                        ErrorHandler.showSuccess("Скопировано: ${field.name}")
                    }
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Копировать",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Отображение прикрепленных файлов
        if (doc.photos.isNotEmpty() || doc.pdfs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Фотографии - каждая в отдельной строке с превью
            doc.photos.forEach { photo ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Превью фотографии (кликабельное)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Фото: ${photo.displayName ?: "Фото"}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { openPhoto(photo.uri.toString()) },
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // PDF файлы - превью с кнопками
            doc.pdfs.forEach { pdf ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Превью PDF с текстом (кликабельное)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.setDataAndType(android.net.Uri.parse(pdf.uri.toString()), "application/pdf")
                                    intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    context.startActivity(android.content.Intent.createChooser(intent, "Открыть PDF"))
                                } catch (e: Exception) {
                                    ErrorHandler.showError("Не удалось открыть PDF: ${e.message}")
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Заголовок PDF
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pdf.displayName ?: "PDF",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Превью текста
                            Text(
                                text = "PDF документ\n(содержимое недоступно для предварительного просмотра)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Кнопки "Редактировать" и "Удалить" в самом низу
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Редактировать")
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
    var pdfPreviews by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
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
    
    val openPhoto = { photoUri: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.parse(photoUri), "image/*")
            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(android.content.Intent.createChooser(intent, "Открыть фото"))
        } catch (e: Exception) {
            ErrorHandler.showError("Не удалось открыть фото: ${e.message}")
        }
    }
    
    // Функция для загрузки превью PDF
    val loadPdfPreview = { pdf: Attachment ->
        scope.launch {
            try {
                val preview = PdfPreviewExtractor.extractPreview(context, pdf.uri, 3)
                pdfPreviews = pdfPreviews + (pdf.id to preview)
            } catch (e: Exception) {
                AppLogger.log("DocumentEditScreen", "ERROR: Failed to load PDF preview: ${e.message}")
            }
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
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
                            
                            // Для новых документов файлы могут быть не привязаны к документу
                            // Попробуем получить их напрямую по ID
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
                                        
                                        val attachment = com.example.docapp.domain.Attachment(
                                            id = entity.id,
                                            documentId = entity.docId ?: "", // Временно пустой для новых документов
                                            kind = kind,
                                            fileName = entity.name,
                                            displayName = entity.name, // Используем name как displayName
                                            uri = android.net.Uri.parse(entity.uri),
                                            createdAt = entity.createdAt,
                                            requiresPersist = false // Файлы уже сохранены
                                        )
                                        attachment
                                    } else {
                                        null
                                    }
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
        
        // Отображение прикрепленных файлов
        
        if (currentPhotos.isNotEmpty() || currentPdfs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Фотографии - каждая в отдельной строке с превью
            currentPhotos.forEach { photo ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Превью фотографии (кликабельное)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Фото: ${photo.displayName ?: "Фото"}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { openPhoto(photo.uri.toString()) },
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Кнопка удалить под фотографией
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
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
            
            // PDF файлы - превью с кнопками
            currentPdfs.forEach { pdf ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Превью PDF с текстом (кликабельное)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openPdf(pdf.uri.toString()) },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Заголовок PDF
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pdf.displayName ?: "PDF",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Превью текста
                            val preview = pdfPreviews[pdf.id]
                            if (preview != null) {
                                Text(
                                    text = preview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                    } else {
                                // Загружаем превью
                                LaunchedEffect(pdf.id) {
                                    loadPdfPreview(pdf)
                                }
                                Text(
                                    text = "Загрузка превью...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Кнопка "Удалить" (клик по карточке открывает PDF)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = { deletePdf(pdf.id) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Удалить")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Кнопки "Отмена" и "Сохранить" в самом низу
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
            onClick = {
                    // Отменяем создание/редактирование документа
                    if (existingDocId == null) {
                        // При создании - отменяем создание и закрываем экран
                        onSaved("") // Пустая строка означает отмену
                    } else {
                        // При редактировании - возвращаем исходные данные
                        scope.launch {
                            try {
                                val originalDoc = useCases.getDoc(existingDocId)
                                if (originalDoc != null) {
                                    name = originalDoc.doc.name
                                    fields.clear()
                                    originalDoc.fields.forEach { field ->
                                        fields.add(field.name to (field.valueCipher?.decodeToString() ?: ""))
                                    }
                                    // Восстанавливаем исходные прикрепленные файлы
                                    currentPhotos = originalDoc.photos
                                    currentPdfs = originalDoc.pdfs
                                }
                            } catch (e: Exception) {
                                ErrorHandler.showError("Не удалось отменить изменения: ${e.message}")
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
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
                                                fields = fields.map { (name, value) ->
                                                    com.example.docapp.domain.DocumentField(
                                                        id = "", // ID будет сгенерирован в репозитории
                                                        documentId = existingDocId,
                                                        name = name,
                                                        valueCipher = value.encodeToByteArray(),
                                                        preview = if (value.length > 20) "${value.take(20)}..." else value,
                                                        isSecret = false,
                                                        ord = 0
                                                    )
                                                }
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
                enabled = !isSaving && name.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isSaving) "Сохранение..." else "Сохранить")
            }
        }
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
