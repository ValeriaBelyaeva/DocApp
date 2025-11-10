package com.example.docapp.ui.template

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docapp.core.DataValidator
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.NamingRules
import com.example.docapp.core.ServiceLocator
import com.example.docapp.ui.theme.GlassCard
import com.example.docapp.ui.theme.AppShapes
import com.example.docapp.ui.theme.AppRadii
import com.example.docapp.ui.theme.AppColors
import kotlinx.coroutines.launch

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
    val fieldValues = remember { mutableStateMapOf<String, String>() }
    var documentName by remember { mutableStateOf("") }
    var documentDescription by remember { mutableStateOf("") }
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
            fieldValues.clear()
            templateFields.forEach { field ->
                fieldValues[field.name] = ""
            }
            documentName = ""
        } catch (e: Exception) {
            ErrorHandler.showError("Не удалось загрузить шаблон: ${e.message}")
        }
    }
    
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
            Spacer(Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
                    text = "Заполнение шаблона",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = template?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TemplateSectionCard(title = "Основные данные") {
                TemplateTextField(
                    label = "Название документа",
                    value = documentName,
                    onValueChange = { documentName = it }
                )
                Spacer(Modifier.height(16.dp))
                TemplateTextField(
                    label = "Описание документа",
                    value = documentDescription,
                    onValueChange = { documentDescription = it },
                    singleLine = false,
                    maxLines = 3
                )
            }

            if (templateFields.isNotEmpty()) {
                TemplateSectionCard(title = "Заполните поля") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        templateFields.forEach { field ->
                            TemplateTextField(
                                label = field.name,
                                value = fieldValues[field.name] ?: "",
                                onValueChange = { newValue ->
                                    fieldValues[field.name] = newValue
                                }
                            )
                        }
                    }
                }
            }

            TemplateSectionCard(title = "Прикреплённые файлы") {
                AttachmentList(
                    photos = attachedPhotos,
                    pdfs = attachedPdfs,
                    onRemovePhoto = { uri ->
                        attachedPhotos = attachedPhotos.filterNot { it.first == uri }
                    },
                    onRemovePdf = { uri ->
                        attachedPdfs = attachedPdfs.filterNot { it.first == uri }
                    }
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "Добавить фото",
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "Добавить PDF",
                        onClick = { documentPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f)
                    )
            }
        }
        
            Spacer(Modifier.height(12.dp))
        
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                SecondaryButton(
                    text = "Отмена",
                onClick = onCancel,
                modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = if (isCreating) "Создание..." else "Создать",
                    enabled = !isCreating && documentName.isNotBlank(),
                    modifier = Modifier.weight(1f),
                onClick = {
                    val nameValidation = DataValidator.validateDocumentName(documentName)
                    val fieldValidations = templateFields.map { field ->
                        val value = fieldValues[field.name] ?: ""
                        DataValidator.validateFieldValue(value)
                    }
                    
                    if (nameValidation.isSuccess && fieldValidations.all { it.isSuccess }) {
                        scope.launch {
                            isCreating = true
                            try {
                                val fields = templateFields.mapIndexed { index, field ->
                                    field.name to fieldValidations[index].getValue()!!
                                }
                                val docId = useCases.createDocWithNames(
                                    tplId = templateId,
                                    folderId = folderId,
                                    name = NamingRules.formatName(nameValidation.getValue()!!, NamingRules.NameKind.Document),
                                    description = documentDescription,
                                    fields = fields,
                                        photoFiles = attachedPhotos.map { (uriString, name) -> Uri.parse(uriString) to name },
                                        pdfFiles = attachedPdfs.map { (uriString, name) -> Uri.parse(uriString) to name }
                                )
                                    ErrorHandler.showSuccess("Документ создан")
                                onDocumentCreated(docId)
                            } catch (e: Exception) {
                                ErrorHandler.showError("Не удалось создать документ: ${e.message}")
                            } finally {
                                isCreating = false
                            }
                        }
                    } else {
                            val errors = buildList {
                                if (!nameValidation.isSuccess) add(nameValidation.getError()!!)
                        fieldValidations.forEachIndexed { index, validation ->
                                    if (!validation.isSuccess) add("Поле ${templateFields[index].name}: ${validation.getError()}")
                            }
                        }
                        ErrorHandler.showError(errors.joinToString("\n"))
                    }
                    }
                )
            }
        }
    }
}

@Composable
private fun TemplateSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = AppShapes.panelLarge()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun TemplateTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun AttachmentList(
    photos: List<Pair<String, String>>,
    pdfs: List<Pair<String, String>>,
    onRemovePhoto: (String) -> Unit,
    onRemovePdf: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (photos.isNotEmpty()) {
            AttachmentGroup(
                title = "Фото",
                items = photos,
                badgeColor = AppColors.iconAccentBackground(),
                icon = Icons.Default.Photo,
                onRemove = onRemovePhoto
            )
        }
        if (pdfs.isNotEmpty()) {
            AttachmentGroup(
                title = "PDF файлы",
                items = pdfs,
                badgeColor = AppColors.iconAccentBackground(),
                icon = Icons.Default.PictureAsPdf,
                onRemove = onRemovePdf
            )
        }
        if (photos.isEmpty() && pdfs.isEmpty()) {
            Text(
                text = "Файлы не прикреплены",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttachmentGroup(
    title: String,
    items: List<Pair<String, String>>,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRemove: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.panelMedium(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            items.forEach { (uri, name) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.listItem(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(AppShapes.badge())
                                .background(badgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemove(uri) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = AppShapes.secondaryButton(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text)
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = AppShapes.primaryButton(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        )
    ) {
        Text(text)
    }
}
