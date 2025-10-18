package com.example.docapp.ui.template

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.ErrorHandler
import kotlinx.coroutines.launch
import android.net.Uri

@Composable
fun TemplateFillScreen(
    templateId: String,
    folderId: String?,
    onDocumentCreated: (String) -> Unit,
    onCancel: () -> Unit
) {
    val useCases = ServiceLocator.useCases
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Состояние экрана
    var template by remember { mutableStateOf<com.example.docapp.domain.Template?>(null) }
    var templateFields by remember { mutableStateOf<List<com.example.docapp.domain.TemplateField>>(emptyList()) }
    var fieldValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var documentName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    
    // Прикрепленные файлы
    var attachedPhotos by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // URI, displayName
    var attachedPdfs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // URI, displayName
    
    // Пикеры файлов
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newPhotos = uris.map { uri ->
                uri.toString() to (uri.lastPathSegment ?: "photo_${System.currentTimeMillis()}")
            }
            attachedPhotos = attachedPhotos + newPhotos
        }
    }
    
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newPdfs = uris.map { uri ->
                uri.toString() to (uri.lastPathSegment ?: "document_${System.currentTimeMillis()}")
            }
            attachedPdfs = attachedPdfs + newPdfs
        }
    }
    
    // Загрузка шаблона
    LaunchedEffect(templateId) {
        try {
            template = useCases.getTemplate(templateId)
            templateFields = useCases.listTemplateFields(templateId)
            
            // Инициализируем значения полей
            val initialValues = templateFields.associate { field ->
                field.name to ""
            }
            fieldValues = initialValues
            
            // Устанавливаем пустое название документа
            documentName = ""
        } catch (e: Exception) {
            ErrorHandler.showError("Не удалось загрузить шаблон: ${e.message}")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Заголовок
        Text(
            text = "Заполнение шаблона: ${template?.name ?: ""}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Название документа
        OutlinedTextField(
            value = documentName,
            onValueChange = { documentName = it },
            label = { Text("Название документа") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Поля шаблона
        Text(
            text = "Заполните поля:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        templateFields.forEach { field ->
            OutlinedTextField(
                value = fieldValues[field.name] ?: "",
                onValueChange = { newValue ->
                    fieldValues = fieldValues + (field.name to newValue)
                },
                label = { Text(field.name) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Секция прикрепленных файлов
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📎 Прикрепленные файлы",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Прикрепленные фото
                attachedPhotos.forEach { (uri, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = {
                                attachedPhotos = attachedPhotos.filter { it.first != uri }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Прикрепленные PDF
                attachedPdfs.forEach { (uri, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = {
                                attachedPdfs = attachedPdfs.filter { it.first != uri }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Кнопки добавления файлов
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить фото")
                    }
                    
                    OutlinedButton(
                        onClick = { documentPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить PDF")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Кнопки управления
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }
            
            Button(
                onClick = {
                    if (documentName.isNotBlank()) {
                        scope.launch {
                            isCreating = true
                            try {
                                // Преобразуем поля в нужный формат
                                val fields = templateFields.map { field ->
                                    field.name to (fieldValues[field.name] ?: "")
                                }
                                
                                // Создаем документ
                                val docId = useCases.createDocWithNames(
                                    tplId = templateId,
                                    folderId = folderId,
                                    name = documentName,
                                    fields = fields,
                                    photoFiles = attachedPhotos.map { (uriString, name) -> 
                                        Uri.parse(uriString) to name 
                                    },
                                    pdfFiles = attachedPdfs.map { (uriString, name) -> 
                                        Uri.parse(uriString) to name 
                                    }
                                )
                                
                                ErrorHandler.showSuccess("Документ создан успешно")
                                onDocumentCreated(docId)
                            } catch (e: Exception) {
                                ErrorHandler.showError("Не удалось создать документ: ${e.message}")
                            } finally {
                                isCreating = false
                            }
                        }
                    }
                },
                enabled = !isCreating && documentName.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isCreating) "Создание..." else "Создать документ")
            }
        }
    }
}
