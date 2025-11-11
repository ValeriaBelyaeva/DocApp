package com.example.docapp.ui.document

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.NamingRules
import com.example.docapp.core.PdfPreviewExtractor
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.newId
import com.example.docapp.domain.Attachment
import com.example.docapp.domain.Document
import com.example.docapp.domain.DocumentRepository
import com.example.docapp.domain.Template
import com.example.docapp.domain.TemplateField
import com.example.docapp.domain.usecases.UseCases
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppLayout
import com.example.docapp.ui.theme.AppShapes
import com.example.docapp.ui.theme.AppColors
import com.example.docapp.ui.theme.GlassCard
import kotlinx.coroutines.launch

/**
 * Экраны документов с интеграцией новой системы вложений
 */

@OptIn(ExperimentalLayoutApi::class)
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
    var showMoveDialog by remember { mutableStateOf(false) }

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

    // Функция для открытия PDF
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

    val scrollState = rememberScrollState()

    val viewFields = remember(doc) {
        buildList {
            if (doc.doc.description.isNotBlank()) {
                add("Description" to doc.doc.description)
            }
            doc.fields.forEach { field ->
                add(field.name to (field.valueCipher?.decodeToString().orEmpty()))
            }
        }
    }

    val handleCopyAll = {
        if (viewFields.isNotEmpty()) {
            val allFieldsText = viewFields.joinToString("\n") { (title, value) ->
                "$title: $value"
            }
            clipboardManager.setText(AnnotatedString(allFieldsText))
            ErrorHandler.showSuccess("Все поля скопированы в буфер обмена")
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = AppLayout.appScreenInsets(Modifier.fillMaxSize())
                .verticalScroll(scrollState)
                .padding(AppDimens.screenPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonCircleButton(
                    icon = Icons.Outlined.Edit,
                    description = "Edit document",
                    containerColor = MaterialTheme.colorScheme.primary,
                    iconColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onEdit
                )
                Spacer(modifier = Modifier.width(AppDimens.spaceLg))
                Text(
                    text = doc.doc.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (viewFields.isNotEmpty()) {
                    Text(
                        text = "Copy all",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable(onClick = handleCopyAll)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))

            viewFields.forEach { (label, value) ->
                ViewFieldCard(
                    label = label,
                    value = value,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(value))
                        ErrorHandler.showSuccess("Скопировано: $label")
                    }
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            }

            if (doc.photos.isNotEmpty()) {
                SectionTitle("Photos")
                Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                doc.photos.forEach { photo ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { openPhoto(photo.uri.toString()) },
                        shape = EditorShapes.section
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photo.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = photo.displayName ?: "Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(EditorShapes.section),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                }
            }

            if (doc.pdfs.isNotEmpty()) {
                SectionTitle("Attached files")
                Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = EditorShapes.section) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = AppDimens.panelPaddingHorizontal,
                            vertical = AppDimens.panelPaddingVertical
                        )
                    ) {
                        Text(
                            text = "Attached files",
                            style = MaterialTheme.typography.titleSmall,
                            color = EditorPalette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                        doc.pdfs.forEach { pdf ->
                            AttachmentChip(
                                name = pdf.displayName ?: "PDF",
                                onOpen = { openPdf(pdf.uri.toString()) }
                            )
                            Spacer(modifier = Modifier.height(AppDimens.listSpacing))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.listSpacing)
            ) {
                NeonOutlineButton(
                    text = "Delete",
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                )
                NeonOutlineButton(
                    text = "Move to folder",
                    onClick = { showMoveDialog = true },
                    modifier = Modifier.weight(1f)
                )
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

    // Диалог перемещения документа в папку
    if (showMoveDialog) {
        MoveToFolderDialog(
            docId = docId,
            currentFolderId = doc.doc.folderId,
            onClose = { showMoveDialog = false }
        )
    }
}

private object EditorPalette {
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val section: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val item: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val iconBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val controlBackground: Color
        @Composable get() = AppColors.iconAccentBackground()
    val badgeBackground: Color
        @Composable get() = AppColors.iconAccentBackground()
    val neon: Color
        @Composable get() = AppColors.iconAccent()
    val textPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val textSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val muted: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val danger: Color
        @Composable get() = MaterialTheme.colorScheme.error
}

private object EditorShapes {
    val section
        @Composable get() = AppShapes.panelLarge()
    val row
        @Composable get() = AppShapes.listItem()
    val badge
        @Composable get() = AppShapes.badge()
    val icon
        @Composable get() = AppShapes.iconButton()
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
    var description by remember { mutableStateOf("") }
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
                description = doc?.doc?.description ?: ""
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
            description = ""
            fields.clear()
            currentPhotos = emptyList()
            currentPdfs = emptyList()
            importedAttachments = emptyList()
        }
    }

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0f) }
    var showImportDialog by remember { mutableStateOf(false) }

    suspend fun handleImportedAttachments(attachmentIds: List<String>) {
        if (existingDocId == null) {
            importedAttachments = importedAttachments + attachmentIds
            try {
                val attachments = attachmentIds.mapNotNull { id ->
                    try {
                        val entity = repos.attachments.getAttachment(id)
                        if (entity != null) {
                            val kind = when {
                                entity.mime.startsWith("image/") -> com.example.docapp.domain.AttachmentKind.photo
                                entity.mime == "application/pdf" -> com.example.docapp.domain.AttachmentKind.pdf
                                else -> com.example.docapp.domain.AttachmentKind.photo
                            }
                            com.example.docapp.domain.Attachment(
                                id = entity.id,
                                documentId = entity.docId ?: "",
                                kind = kind,
                                fileName = entity.name,
                                displayName = entity.name,
                                uri = android.net.Uri.parse(entity.uri),
                                createdAt = entity.createdAt,
                                requiresPersist = false
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                val photos = attachments.filter { it.kind == com.example.docapp.domain.AttachmentKind.photo }
                val pdfs = attachments.filter { it.kind == com.example.docapp.domain.AttachmentKind.pdf }
                currentPhotos = currentPhotos + photos
                currentPdfs = currentPdfs + pdfs
            } catch (e: Exception) {
                ErrorHandler.showError("Не удалось обновить список файлов: ${e.message}")
            }
        } else {
            try {
                val doc = useCases.getDoc(existingDocId)
                currentPhotos = doc?.photos ?: emptyList()
                currentPdfs = doc?.pdfs ?: emptyList()
            } catch (e: Exception) {
                ErrorHandler.showError("Не удалось обновить список файлов: ${e.message}")
            }
        }
        ErrorHandler.showSuccess("Файлы импортированы успешно")
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                try {
                    isImporting = true
                    showImportDialog = true
                    importProgress = 0f
                    val result = useCases.importAttachments(context, existingDocId, uris)
                    importProgress = 1f
                    handleImportedAttachments(result.attachments.map { it.id })
                } catch (e: Exception) {
                    ErrorHandler.showError("Ошибка импорта фотографий: ${e.message}")
                } finally {
                    isImporting = false
                    showImportDialog = false
                }
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                try {
                    isImporting = true
                    showImportDialog = true
                    importProgress = 0f
                    val result = useCases.importAttachments(context, existingDocId, uris)
                    importProgress = 1f
                    handleImportedAttachments(result.attachments.map { it.id })
                } catch (e: Exception) {
                    ErrorHandler.showError("Ошибка импорта файлов: ${e.message}")
                } finally {
                    isImporting = false
                    showImportDialog = false
                }
            }
        }
    }

    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.getBottom(density) / density.density
    val topInset = WindowInsets.statusBars.getTop(density) / density.density

    Surface(color = EditorPalette.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing),
            contentPadding = PaddingValues(
                start = AppDimens.screenPadding,
                end = AppDimens.screenPadding,
                top = AppDimens.sectionSpacing + topInset.dp,
                bottom = AppDimens.bottomButtonsSpacer + bottomInset.dp
            )
        ) {
            item {
                EditorHeaderRow(
                    title = if (existingDocId != null) "Редактирование документа" else "Создание документа",
                    onClose = {
                        if (existingDocId == null) {
                            onSaved("")
                        } else {
                            onSaved(existingDocId)
                        }
                    }
                )
            }

            item {
                EditorSectionCard(title = "Основная информация") {
                    EditorFieldInput(
                        label = "Название документа",
                        value = name,
                        placeholder = "Придумай короткое имя",
                        onValueChange = { name = it }
                    )
                    Spacer(Modifier.height(AppDimens.sectionSpacing))
                    EditorFieldInput(
                        label = "Описание",
                        value = description,
                        placeholder = "Добавь пару слов о содержимом",
                        onValueChange = { description = it },
                        singleLine = false
                    )
                }
            }

            item {
                EditorSectionCard(title = "Поля документа") {
                    if (fields.isEmpty()) {
                        EditorEmptyPlaceholder("Пока здесь пусто. Добавь поле, чтобы заполнить документ содержимым.")
                    } else {
                        fields.forEachIndexed { index, (fieldName, fieldValue) ->
                            EditorFieldInput(
                                label = fieldName,
                                value = fieldValue,
                                placeholder = "Значение",
                                onValueChange = { newValue -> fields[index] = fieldName to newValue },
                                removable = true,
                                onRemove = { fields.removeAt(index) }
                            )
                            if (index != fields.lastIndex) {
                                Spacer(Modifier.height(AppDimens.listSpacing))
                            }
                        }
                    }
                    Spacer(Modifier.height(AppDimens.sectionSpacing))
                    NeonOutlineButton(
                        text = "Добавить поле",
                        onClick = {
                            newFieldName = ""
                            showAddFieldDialog = true
                        },
                        enabled = !isSaving
                    )
                }
            }

            item {
                EditorSectionCard(title = "Вложения") {
                    Box {
                        NeonOutlineButton(
                            text = if (isImporting) "Идет импорт..." else "Добавить файл",
                            onClick = { if (!isImporting) showAttachmentMenu = true },
                            enabled = !isImporting
                        )
                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Добавить фото", color = EditorPalette.textPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    if (!isImporting) {
                                        photoPickerLauncher.launch("image/*")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Добавить PDF", color = EditorPalette.textPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    if (!isImporting) {
                                        documentPickerLauncher.launch(arrayOf("application/pdf"))
                                    }
                                }
                            )
                        }
                    }

                    if (currentPhotos.isEmpty() && currentPdfs.isEmpty()) {
                        Spacer(Modifier.height(AppDimens.spaceLg))
                        EditorEmptyPlaceholder("После импорта здесь появятся фото и документы.")
                    } else {
                        if (currentPhotos.isNotEmpty()) {
                            Spacer(Modifier.height(AppDimens.spaceLg))
                            EditorSubsectionTitle("Фото")
                            Spacer(Modifier.height(AppDimens.listSpacing))
                            currentPhotos.forEach { photo ->
                                EditorPhotoCard(
                                    photo = photo,
                                    onOpen = { openPhoto(photo.uri.toString()) },
                                    onDelete = { deletePhoto(photo.id) },
                                    isDeleting = isSaving
                                )
                                Spacer(Modifier.height(AppDimens.spaceLg))
                            }
                        }
                        if (currentPdfs.isNotEmpty()) {
                            Spacer(Modifier.height(AppDimens.spaceLg))
                            EditorSubsectionTitle("PDF файлы")
                            Spacer(Modifier.height(AppDimens.listSpacing))
                            currentPdfs.forEach { pdf ->
                                EditorPdfCard(
                                    pdf = pdf,
                                    preview = pdfPreviews[pdf.id],
                                    onLoadPreview = { loadPdfPreview(pdf) },
                                    onOpen = { openPdf(pdf.uri.toString()) },
                                    onDelete = { deletePdf(pdf.id) },
                                    isDeleting = isSaving
                                )
                                Spacer(Modifier.height(AppDimens.spaceLg))
                            }
                        }
                    }
                }
            }

            item {
                NeonPrimaryButton(
                    text = when {
                        isSaving -> "Сохраняю..."
                        existingDocId != null -> "Сохранить изменения"
                        else -> "Сохранить документ"
                    },
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                isSaving = true
                                try {
                                    val id = if (existingDocId != null) {
                                        val existingDoc = useCases.getDoc(existingDocId)
                                            ?: throw Exception("Документ не найден")
                                        useCases.updateDoc(
                                            existingDoc.copy(
                                                doc = existingDoc.doc.copy(
                                                    name = NamingRules.formatName(name, NamingRules.NameKind.Document),
                                                    description = description
                                                ),
                                                fields = fields.mapIndexed { index, (fieldName, fieldValue) ->
                                                    com.example.docapp.domain.DocumentField(
                                                        id = com.example.docapp.core.newId(),
                                                        documentId = existingDocId,
                                                        name = fieldName,
                                                        valueCipher = fieldValue.encodeToByteArray(),
                                                        preview = if (fieldValue.length > 20) "${fieldValue.take(20)}..." else fieldValue,
                                                        isSecret = false,
                                                        ord = index
                                                    )
                                                },
                                                photos = currentPhotos,
                                                pdfs = currentPdfs
                                            )
                                        )
                                        existingDocId
                                    } else {
                                        val newDocId = useCases.createDoc(
                                            tplId = templateId,
                                            folderId = folderId,
                                            name = NamingRules.formatName(name, NamingRules.NameKind.Document),
                                            description = description,
                                            fields = fields.toList(),
                                            photos = emptyList(),
                                            pdfUris = emptyList()
                                        )

                                        if (importedAttachments.isNotEmpty()) {
                                            useCases.bindAttachmentsToDoc(importedAttachments, newDocId)
                                        }
                                        newDocId
                                    }
                                    ErrorHandler.showSuccess("Документ сохранен")
                                    onSaved(id)
                                } catch (error: Exception) {
                                    ErrorHandler.showError("Не удалось сохранить документ: ${error.message}")
                                } finally {
                                    isSaving = false
                                }
                            }
                        } else {
                            ErrorHandler.showError("Название документа не может быть пустым")
                        }
                    },
                    enabled = !isSaving && name.isNotBlank()
                )
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = EditorPalette.section,
            title = { Text("Импорт файлов", color = EditorPalette.textPrimary) },
            text = {
                Column {
                    Text("Импорт файлов в процессе...", color = EditorPalette.textSecondary)
                    Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                    LinearProgressIndicator(
                        progress = { importProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = EditorPalette.neon,
                        trackColor = EditorPalette.muted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { },
                    enabled = false,
                    colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.neon.copy(alpha = 0.4f))
                ) { Text("Отмена") }
            }
        )
    }

    // Диалог добавления поля
    if (showAddFieldDialog) {
        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            containerColor = EditorPalette.section,
            title = { Text("Добавить поле", color = EditorPalette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newFieldName,
                    onValueChange = { newFieldName = it },
                    label = { Text("Название поля") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorPalette.textPrimary,
                        unfocusedTextColor = EditorPalette.textPrimary,
                        cursorColor = EditorPalette.neon,
                        focusedBorderColor = EditorPalette.neon,
                        unfocusedBorderColor = EditorPalette.muted,
                        focusedLabelColor = EditorPalette.neon,
                        unfocusedLabelColor = EditorPalette.textSecondary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
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
                    enabled = newFieldName.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.neon)
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddFieldDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.textSecondary)
                ) {
                    Text("Отмена")
                }
            }
        )
    }

}

@Composable
private fun NeonCircleButton(
    icon: ImageVector,
    description: String,
    containerColor: Color = EditorPalette.controlBackground,
    iconColor: Color = EditorPalette.neon,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(EditorShapes.icon)
            .background(containerColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = EditorPalette.textPrimary
    )
}

@Composable
private fun EditorHeaderRow(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = EditorPalette.textPrimary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        EditorIconButton(
            icon = Icons.Outlined.Close,
            description = "Закрыть",
            onClick = onClose
        )
    }
}

@Composable
private fun EditorSectionCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = EditorShapes.section) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                )
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    color = EditorPalette.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(AppDimens.listSpacing))
            }
            content()
        }
    }
}

@Composable
private fun EditorFieldInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    removable: Boolean = false,
    onRemove: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EditorShapes.row)
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = EditorPalette.textSecondary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.weight(1f))
            if (removable && onRemove != null) {
                EditorIconButton(
                    icon = Icons.Outlined.Close,
                    description = "Удалить поле",
                    onClick = onRemove,
                    background = EditorPalette.controlBackground,
                    tint = EditorPalette.neon.copy(alpha = 0.8f),
                    size = 36.dp
                )
            }
        }
        Spacer(Modifier.height(AppDimens.listSpacing))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = EditorPalette.textPrimary),
            cursorBrush = SolidColor(EditorPalette.neon),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (singleLine) ImeAction.Done else ImeAction.Default
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = EditorPalette.textSecondary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun EditorEmptyPlaceholder(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EditorShapes.row)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            )
    ) {
        Text(
            text = text,
            color = EditorPalette.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EditorSubsectionTitle(text: String) {
    Text(
        text = text,
        color = EditorPalette.textPrimary,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun EditorPhotoCard(
    photo: Attachment,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EditorShapes.row)
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            )
    ) {
        Text(
            text = photo.displayName ?: "Фото",
            color = EditorPalette.textPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(AppDimens.listSpacing))
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName ?: "Фото",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(EditorShapes.row)
                .background(EditorPalette.iconBackground)
                .clickable(onClick = onOpen),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(AppDimens.spaceLg))
        NeonOutlineButton(
            text = "Удалить фото",
            onClick = onDelete,
            enabled = !isDeleting
        )
    }
}

@Composable
private fun EditorPdfCard(
    pdf: Attachment,
    preview: String?,
    onLoadPreview: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EditorShapes.row)
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(EditorShapes.badge)
                    .background(EditorPalette.badgeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = EditorPalette.neon,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(AppDimens.spaceLg))
            Text(
                text = pdf.displayName ?: "PDF",
                color = EditorPalette.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(AppDimens.listSpacing))
        if (preview != null) {
            Text(
                text = preview,
                color = EditorPalette.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            LaunchedEffect(pdf.id) {
                onLoadPreview()
            }
            Text(
                text = "Готовлю превью...",
                color = EditorPalette.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }
        Spacer(Modifier.height(AppDimens.listSpacing))
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.listSpacing)) {
            NeonOutlineButton(
                text = "Открыть",
                onClick = onOpen,
                modifier = Modifier.weight(1f)
            )
            NeonOutlineButton(
                text = "Удалить",
                onClick = onDelete,
                enabled = !isDeleting,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EditorIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    background: Color = EditorPalette.controlBackground,
    tint: Color = EditorPalette.neon,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(EditorShapes.icon)
            .background(background)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

@Composable
private fun ViewFieldCard(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EditorShapes.row)
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(EditorPalette.controlBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = EditorPalette.neon,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(AppDimens.iconRowSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = EditorPalette.textSecondary
            )
            Spacer(modifier = Modifier.height(AppDimens.labelSpacing))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = EditorPalette.textPrimary
            )
        }

        Spacer(modifier = Modifier.width(AppDimens.iconRowSpacing))
        NeonCircleButton(
            icon = Icons.Default.ContentCopy,
            description = "Copy value",
            onClick = onCopy
        )
    }
}

@Composable
private fun AttachmentChip(
    name: String,
    onOpen: () -> Unit
) {
    val shape = AppShapes.chip()
    Surface(
        shape = shape,
        color = EditorPalette.item,
        border = BorderStroke(1.dp, EditorPalette.neon.copy(alpha = 0.3f)),
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.iconRowSpacing)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp, 52.dp)
                    .clip(CircleShape)
                    .background(EditorPalette.badgeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = EditorPalette.neon,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = EditorPalette.textPrimary
            )
        }
    }
}

@Composable
private fun NeonOutlineButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(54.dp),
        shape = AppShapes.secondaryButton(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = EditorPalette.neon,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = EditorPalette.neon.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.6.dp, EditorPalette.neon)
    ) {
        Text(text)
    }
}

@Composable
private fun NeonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(58.dp),
        shape = AppShapes.primaryButton(),
        colors = ButtonDefaults.buttonColors(
            containerColor = EditorPalette.neon,
            contentColor = EditorPalette.background,
            disabledContainerColor = EditorPalette.neon.copy(alpha = 0.3f),
            disabledContentColor = EditorPalette.background.copy(alpha = 0.5f)
        )
    ) {
        Text(text)
    }
}

/* ===== Диалог перемещения документа в папку ===== */
@Composable
private fun MoveToFolderDialog(
    docId: String,
    currentFolderId: String?,
    onClose: () -> Unit
) {
    val useCases = ServiceLocator.useCases
    val scope = rememberCoroutineScope()
    var folders by remember { mutableStateOf<List<com.example.docapp.domain.Folder>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(currentFolderId) }

    LaunchedEffect(Unit) {
        folders = useCases.listFolders()
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = EditorPalette.section,
        title = { Text("Переместить в папку", color = EditorPalette.textPrimary) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selected == null,
                        onClick = { selected = null },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = EditorPalette.neon,
                            unselectedColor = EditorPalette.textSecondary
                        )
                    )
                    Text("Без папки", color = EditorPalette.textSecondary)
                }
                Spacer(Modifier.height(AppDimens.spaceSm))
                folders.forEach { folder ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selected == folder.id,
                            onClick = { selected = folder.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = EditorPalette.neon,
                                unselectedColor = EditorPalette.textSecondary
                            )
                        )
                        Text(folder.name, color = EditorPalette.textSecondary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            useCases.moveDocToFolder(docId, selected)
                            ErrorHandler.showSuccess("Документ перемещен")
                            onClose()
                        } catch (e: Exception) {
                            ErrorHandler.showError("Не удалось переместить документ: ${e.message}")
                        }
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.neon)
            ) {
                Text("Переместить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.textSecondary)
            ) {
                Text("Отмена")
            }
        }
    )
}


