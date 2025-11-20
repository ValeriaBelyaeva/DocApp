package com.example.docapp.ui.document
import androidx.activity.compose.BackHandler
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
import com.example.docapp.ui.theme.AppAlphas
import com.example.docapp.ui.theme.AppBorderWidths
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppLayout
import com.example.docapp.ui.theme.AppShapes
import com.example.docapp.ui.theme.AppColors
import com.example.docapp.ui.theme.GlassCard
import kotlinx.coroutines.launch
/**
 * Main document view screen that displays document details, fields, and attachments.
 * Provides options to edit, delete, move, and interact with document content.
 * 
 * Works by loading the full document data from the repository, displaying all fields and attachments,
 * and providing interactive buttons for various document operations. Blocks back navigation to prevent returning to PIN screen.
 * 
 * arguments:
 *     docId - String: The unique identifier of the document to display
 *     onEdit - () -> Unit: Callback function invoked when the user wants to edit the document
 *     onDeleted - () -> Unit: Callback function invoked when the document is deleted
 *     navigator - AppNavigator: Navigation helper for safe back navigation
 * 
 * return:
 *     Unit - No return value
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentViewScreen(
    docId: String,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    navigator: com.example.docapp.ui.navigation.AppNavigator
) {
    BackHandler(enabled = true) {
        navigator.safePopBack()
    }
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
            ErrorHandler.showError("Failed to load document: ${e.message}")
        }
    }
    val doc = fullDoc
    if (doc == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val openPdf = { pdfUri: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.parse(pdfUri), "application/pdf")
            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(android.content.Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            ErrorHandler.showError("Failed to open PDF: ${e.message}")
        }
    }
    val openPhoto = { photoUri: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.parse(photoUri), "image/*")
            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(android.content.Intent.createChooser(intent, "Open photo"))
        } catch (e: Exception) {
            ErrorHandler.showError("Failed to open photo: ${e.message}")
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
            ErrorHandler.showSuccess("All fields copied to clipboard")
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
            containerColor = AppColors.iconAccent(),
            iconColor = AppColors.background(),
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
                        color = AppColors.iconAccent(),
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
                        ErrorHandler.showSuccess("Copied: $label")
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
            title = { Text("Delete document") },
            text = { Text("Are you sure you want to delete this document?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                useCases.deleteDoc(docId)
                                ErrorHandler.showSuccess("Document deleted")
                                onDeleted()
                            } catch (e: Exception) {
                                ErrorHandler.showError("Failed to delete document: ${e.message}")
                            }
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
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
/**
 * Document editor screen for creating new documents or editing existing ones.
 * Allows editing document name, description, custom fields, and managing attachments.
 * 
 * Works by loading existing document data if editing, or starting with empty form if creating.
 * Provides input fields for all document properties and handles saving changes back to the repository.
 * Blocks back navigation to prevent returning to PIN screen.
 * 
 * arguments:
 *     existingDocId - String?: Optional ID of existing document to edit, null to create a new document
 *     templateId - String?: Optional ID of template to use for new document creation, null if no template
 *     folderId - String?: Optional folder ID where the document will be created/edited, null for root folder
 *     onSaved - (String) -> Unit: Callback function invoked when document is saved, receives the document ID
 *     navigator - AppNavigator: Navigation helper for safe back navigation
 * 
 * return:
 *     Unit - No return value
 */
@Composable
fun DocumentEditScreen(
    existingDocId: String?,
    templateId: String?,
    folderId: String?,
    onSaved: (String) -> Unit,
    navigator: com.example.docapp.ui.navigation.AppNavigator
) {
    BackHandler(enabled = true) {
        navigator.safePopBack()
    }
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
    var showDeletePhotoDialog by remember { mutableStateOf<Attachment?>(null) }
    var showDeletePdfDialog by remember { mutableStateOf<Attachment?>(null) }
    val deletePhoto: (String) -> Unit = { photoId: String ->
        val photo = currentPhotos.find { it.id == photoId }
        if (photo != null) {
            showDeletePhotoDialog = photo
        }
    }
    val deletePdf: (String) -> Unit = { pdfId: String ->
        val pdf = currentPdfs.find { it.id == pdfId }
        if (pdf != null) {
            showDeletePdfDialog = pdf
        }
    }
    val openPdf = { pdfUri: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.parse(pdfUri), "application/pdf")
            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(android.content.Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            ErrorHandler.showError("Failed to open PDF: ${e.message}")
        }
    }
    val openPhoto = { photoUri: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.parse(photoUri), "image/*")
            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(android.content.Intent.createChooser(intent, "Open photo"))
        } catch (e: Exception) {
            ErrorHandler.showError("Failed to open photo: ${e.message}")
        }
    }
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
                currentPhotos = doc?.photos ?: emptyList()
                currentPdfs = doc?.pdfs ?: emptyList()
            } catch (e: Exception) {
                ErrorHandler.showError("Failed to load document: ${e.message}")
            }
        } else {
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
                ErrorHandler.showError("Failed to refresh file list: ${e.message}")
            }
        } else {
            try {
                val doc = useCases.getDoc(existingDocId)
                currentPhotos = doc?.photos ?: emptyList()
                currentPdfs = doc?.pdfs ?: emptyList()
            } catch (e: Exception) {
                ErrorHandler.showError("Failed to refresh file list: ${e.message}")
            }
        }
        ErrorHandler.showSuccess("Files imported successfully")
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
                    ErrorHandler.showError("Failed to import photos: ${e.message}")
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
                    ErrorHandler.showError("Failed to import files: ${e.message}")
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
                    title = if (existingDocId != null) "Edit document" else "Create document",
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
                EditorSectionCard(title = "Main information") {
                    EditorFieldInput(
                        label = "Document name",
                        value = name,
                        placeholder = "Enter a short name",
                        onValueChange = { name = it }
                    )
                    Spacer(Modifier.height(AppDimens.sectionSpacing))
                    EditorFieldInput(
                            label = "Description",
                        value = description,
                            placeholder = "Add a short summary",
                        onValueChange = { description = it },
                        singleLine = false
                    )
                }
            }
            item {
                EditorSectionCard(title = "Document fields") {
                    if (fields.isEmpty()) {
                        EditorEmptyPlaceholder("Nothing here yet. Add a field to fill the document.")
                    } else {
                        fields.forEachIndexed { index, (fieldName, fieldValue) ->
                            EditorFieldInput(
                                label = fieldName,
                                value = fieldValue,
                                placeholder = "Value",
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
                        text = "Add field",
                        onClick = {
                            newFieldName = ""
                            showAddFieldDialog = true
                        },
                        enabled = !isSaving
                    )
                }
            }
            item {
                EditorSectionCard(title = "Attachments") {
                    Box {
                        NeonOutlineButton(
                            text = if (isImporting) "Importing..." else "Add file",
                            onClick = { if (!isImporting) showAttachmentMenu = true },
                            enabled = !isImporting
                        )
                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add photo", color = EditorPalette.textPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    if (!isImporting) {
                                        photoPickerLauncher.launch("image/*")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add PDF", color = EditorPalette.textPrimary) },
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
                        EditorEmptyPlaceholder("Photos and documents will appear here after import.")
                    } else {
                        if (currentPhotos.isNotEmpty()) {
                            Spacer(Modifier.height(AppDimens.spaceLg))
                            EditorSubsectionTitle("Photos")
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
                            EditorSubsectionTitle("PDF files")
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
                        isSaving -> "Saving..."
                        existingDocId != null -> "Save changes"
                        else -> "Save document"
                    },
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                isSaving = true
                                try {
                                    val id = if (existingDocId != null) {
                                        val existingDoc = useCases.getDoc(existingDocId)
                                            ?: throw Exception("Document not found")
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
                                    ErrorHandler.showSuccess("Document saved")
                                    onSaved(id)
                                } catch (error: Exception) {
                                    ErrorHandler.showError("Failed to save document: ${error.message}")
                                } finally {
                                    isSaving = false
                                }
                            }
                        } else {
                            ErrorHandler.showError("Document name cannot be empty")
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
            title = { Text("File import", color = EditorPalette.textPrimary) },
            text = {
                Column {
                    Text("File import in progress...", color = EditorPalette.textSecondary)
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
                    colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.neon.copy(alpha = AppAlphas.Document.outlineDisabledContent))
                ) { Text("Cancel") }
            }
        )
    }
    if (showAddFieldDialog) {
        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            containerColor = EditorPalette.section,
            title = { Text("Add field", color = EditorPalette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newFieldName,
                    onValueChange = { newFieldName = it },
                    label = { Text("Field name") },
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
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddFieldDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.textSecondary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showDeletePhotoDialog != null) {
        val photoToDelete = showDeletePhotoDialog
        if (photoToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeletePhotoDialog = null },
                containerColor = EditorPalette.section,
                title = { Text("Delete photo", color = EditorPalette.textPrimary) },
                text = {
                    Text(
                        "Are you sure you want to delete the photo \"${photoToDelete.displayName ?: "photo"}\"? This action cannot be undone.",
                        color = EditorPalette.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    useCases.deleteAttachment(photoToDelete.id)
                                    currentPhotos = currentPhotos.filter { it.id != photoToDelete.id }
                                    if (existingDocId == null) {
                                        importedAttachments = importedAttachments.filter { it != photoToDelete.id }
                                    }
                                    ErrorHandler.showSuccess("Photo deleted")
                                } catch (e: Exception) {
                                    ErrorHandler.showError("Failed to delete photo: ${e.message}")
                                }
                            }
                            showDeletePhotoDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.danger)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeletePhotoDialog = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.textSecondary)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    if (showDeletePdfDialog != null) {
        val pdfToDelete = showDeletePdfDialog
        if (pdfToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeletePdfDialog = null },
                containerColor = EditorPalette.section,
                title = { Text("Delete PDF", color = EditorPalette.textPrimary) },
                text = {
                    Text(
                        "Are you sure you want to delete the file \"${pdfToDelete.displayName ?: "PDF"}\"? This action cannot be undone.",
                        color = EditorPalette.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    useCases.deleteAttachment(pdfToDelete.id)
                                    currentPdfs = currentPdfs.filter { it.id != pdfToDelete.id }
                                    if (existingDocId == null) {
                                        importedAttachments = importedAttachments.filter { it != pdfToDelete.id }
                                    }
                                    ErrorHandler.showSuccess("PDF deleted")
                                } catch (e: Exception) {
                                    ErrorHandler.showError("Failed to delete PDF: ${e.message}")
                                }
                            }
                            showDeletePdfDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.danger)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeletePdfDialog = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.textSecondary)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
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
            .size(AppDimens.Document.toolbarActionIcon)
            .clip(EditorShapes.icon)
            .background(containerColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = iconColor,
            modifier = Modifier.size(AppDimens.Document.metadataIcon)
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
            description = "Close",
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
                    description = "Remove field",
                    onClick = onRemove,
                    background = EditorPalette.controlBackground,
                    tint = EditorPalette.neon.copy(alpha = AppAlphas.Document.fieldActionTint),
                    size = AppDimens.Document.fieldActionIcon,
                    iconSize = AppDimens.Document.actionChipIcon
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
            text = photo.displayName ?: "Photo",
            color = EditorPalette.textPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(AppDimens.listSpacing))
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName ?: "Photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.Document.previewCardHeight)
                .clip(EditorShapes.row)
                .background(EditorPalette.iconBackground)
                .clickable(onClick = onOpen),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(AppDimens.spaceLg))
        NeonOutlineButton(
            text = "Remove photo",
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
                    .size(AppDimens.Document.actionChip)
                    .clip(EditorShapes.badge)
                    .background(EditorPalette.badgeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = EditorPalette.neon,
                    modifier = Modifier.size(AppDimens.Document.actionChipIcon)
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
                text = "Preparing preview...",
                color = EditorPalette.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }
        Spacer(Modifier.height(AppDimens.listSpacing))
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.listSpacing)) {
            NeonOutlineButton(
                text = "Open",
                onClick = onOpen,
                modifier = Modifier.weight(1f)
            )
            NeonOutlineButton(
                text = "Delete",
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
    size: androidx.compose.ui.unit.Dp = AppDimens.Document.toolbarActionIcon,
    iconSize: androidx.compose.ui.unit.Dp = AppDimens.Document.metadataIcon
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
            modifier = Modifier.size(iconSize)
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
                .size(AppDimens.Document.actionChip)
                .clip(CircleShape)
                .background(EditorPalette.controlBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = EditorPalette.neon,
                modifier = Modifier.size(AppDimens.Document.actionChipIcon)
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
        border = BorderStroke(AppBorderWidths.thin, EditorPalette.neon.copy(alpha = AppAlphas.Document.infoBorder)),
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
                    .size(AppDimens.Document.actionChip, AppDimens.Document.actionChipTallHeight)
                    .clip(CircleShape)
                    .background(EditorPalette.badgeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = EditorPalette.neon,
                    modifier = Modifier.size(AppDimens.Document.actionChipIcon)
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
            .height(AppDimens.Document.editorToolbarHeight),
        shape = AppShapes.secondaryButton(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = AppColors.iconAccent(),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = AppColors.iconAccent().copy(alpha = AppAlphas.Document.outlineDisabledContent)
        ),
        border = BorderStroke(AppBorderWidths.medium, AppColors.iconAccent())
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
            .height(AppDimens.Document.editorBottomBarHeight),
        shape = AppShapes.primaryButton(),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.iconAccent(),
            contentColor = AppColors.background(),
            disabledContainerColor = AppColors.iconAccent().copy(alpha = AppAlphas.Document.primaryDisabledContainer),
            disabledContentColor = AppColors.background().copy(alpha = AppAlphas.Document.primaryDisabledContent)
        )
    ) {
        Text(text)
    }
}
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
        title = { Text("Move to folder", color = EditorPalette.textPrimary) },
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
                    Text("No folder", color = EditorPalette.textSecondary)
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
                            ErrorHandler.showSuccess("Document moved")
                            onClose()
                        } catch (e: Exception) {
                            ErrorHandler.showError("Failed to move document: ${e.message}")
                        }
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.neon)
            ) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                colors = ButtonDefaults.textButtonColors(contentColor = EditorPalette.textSecondary)
            ) {
                Text("Cancel")
            }
        }
    )
}
