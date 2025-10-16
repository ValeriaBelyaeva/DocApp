package com.example.docapp.ui.document
import android.content.ClipDescription
import android.provider.OpenableColumns

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.newId
import com.example.docapp.domain.Attachment
import com.example.docapp.domain.AttachmentKind
import com.example.docapp.domain.DocumentField
import com.example.docapp.domain.DocumentRepository
import com.example.docapp.ui.widgets.FieldTile
import com.example.docapp.ui.widgets.PrimaryButton
import com.example.docapp.ui.widgets.copyToClipboard
import kotlinx.coroutines.launch

// Модель для файлов с именами
data class NamedFile(
    val uri: Uri,
    val displayName: String,
    val isPhoto: Boolean
)

/* -------- Просмотр ---------- */
@Composable
fun DocumentViewScreen(
    docId: String,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    val uc = ServiceLocator.useCases
    val ctx = LocalContext.current
    var full by remember { mutableStateOf<DocumentRepository.FullDocument?>(null) }
    val scope = rememberCoroutineScope()

    val revealed = remember { mutableStateMapOf<String, Boolean>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(docId) {
        full = uc.getDoc(docId)
        uc.touchOpened(docId)
        revealed.clear()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(full?.doc?.name ?: "Документ", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = "Удалить документ") }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            full?.fields?.let { fields ->
                itemsIndexed(fields, key = { _, f -> f.id }) { _, f ->
                    val fullValue = (f.valueCipher ?: ByteArray(0)).toString(Charsets.UTF_8)
                    val show = revealed[f.id] == true
                    FieldTile(
                        title = f.name,
                        preview = if (show) null else (f.preview ?: ""),
                        value = if (show) fullValue else null,
                        showValue = show,
                        onCopy = { copyToClipboard(ctx, f.name, fullValue) },
                        onToggleSecret = { revealed[f.id] = !show }
                    )
                }
            }

            // Фото: список всех кнопок
            item { Spacer(Modifier.height(12.dp)); Text("ФОТО", style = MaterialTheme.typography.titleMedium) }
            full?.photos?.let { photos ->
                itemsIndexed(photos) { index, photo ->
                    val fileStore = ServiceLocator.files as com.example.docapp.core.EncryptedAttachmentStore
                    val isAvailable = fileStore.retrieve(photo.uri) != null
                    
                    if (isAvailable) {
                        OutlinedButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(photo.uri, "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(intent)
                        }) { Text("ОТКРЫТЬ ФОТО ${index + 1}") }
                    } else {
                        Text(
                            "ФОТО ${index + 1} - ФАЙЛ НЕ НАЙДЕН", 
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // PDF
            item { Spacer(Modifier.height(12.dp)); Text("PDF", style = MaterialTheme.typography.titleMedium) }
            full?.pdfs?.let { pdfs ->
                itemsIndexed(pdfs) { index, pdf ->
                    val fileStore = ServiceLocator.files as com.example.docapp.core.EncryptedAttachmentStore
                    val isAvailable = fileStore.retrieve(pdf.uri) != null
                    
                    if (isAvailable) {
                        OutlinedButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(pdf.uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(intent)
                        }) { Text("ОТКРЫТЬ PDF ${index + 1}") }
                    } else {
                        Text(
                            "PDF ${index + 1} - ФАЙЛ НЕ НАЙДЕН", 
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } ?: item { Text("PDF не прикреплён") }

        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить документ?") },
            text = { Text("Действие необратимо. Подтвердить удаление?") },
            confirmButton = {
                val scope = rememberCoroutineScope()
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        uc.deleteDoc(docId)
                        onDeleted()
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }
}

/* -------- Создание/Редактирование ---------- */
@Composable
fun DocumentEditScreen(
    existingDocId: String?,
    templateId: String?,
    folderId: String?,
    onSaved: (String) -> Unit
) {
    val uc = ServiceLocator.useCases
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    val fields = remember { mutableStateListOf<Pair<String, String>>() }

    // несколько фото и PDF с именами
    val photoFiles = remember { mutableStateListOf<NamedFile>() }
    val pdfFiles = remember { mutableStateListOf<NamedFile>() }

    var confirmDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var renameFileIndex by remember { mutableStateOf<Int?>(null) }
    var newFileName by remember { mutableStateOf("") }

    LaunchedEffect(existingDocId, templateId) {
        fields.clear()
        photoFiles.clear()
        pdfFiles.clear()

        if (existingDocId != null) {
            val f = uc.getDoc(existingDocId)
            if (f != null) {
                name = f.doc.name
                fields.addAll(f.fields.map { it.name to (it.valueCipher?.toString(Charsets.UTF_8) ?: "") })
                
                // Загружаем фото с именами
                f.photos.forEachIndexed { index, photo ->
                    photoFiles.add(NamedFile(photo.uri, "Фото ${index + 1}", true))
                }
                
                // Загружаем PDF с именами
                f.pdfs.forEachIndexed { index, pdf ->
                    pdfFiles.add(NamedFile(pdf.uri, "PDF ${index + 1}", false))
                }
            }
        } else if (templateId != null) {
            val tf = uc.listTemplateFields(templateId)
            fields.addAll(tf.sortedBy { it.ord }.map { it.name to "" })
            name = "Документ"
        }
    }

    // pickers
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEachIndexed { index, uri ->
            val nextPhotoNumber = photoFiles.count { it.isPhoto } + 1
            photoFiles.add(NamedFile(uri, "Фото $nextPhotoNumber", true))
        }
    }
    val pickpdfs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEachIndexed { index, uri ->
            val nextPdfNumber = pdfFiles.count { !it.isPhoto } + 1
            pdfFiles.add(NamedFile(uri, "PDF $nextPdfNumber", false))
        }
    }

    /*
    val pickpdfs = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pdfsUri = it }
    }*/

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Название документа") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        var newFieldName by remember { mutableStateOf("") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newFieldName, onValueChange = { newFieldName = it },
                label = { Text("Введите название поля") },
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (newFieldName.isNotBlank()) {
                    fields.add(newFieldName to "")
                    newFieldName = ""
                }
            }) { Text("+") }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            itemsIndexed(fields, key = { index, _ -> "field-$index" }) { i, (t, v) ->
                OutlinedTextField(
                    value = v,
                    onValueChange = { nv -> fields[i] = t to nv },
                    label = { Text(t) },
                    trailingIcon = {
                        IconButton(onClick = { confirmDeleteIndex = i }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить поле")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text("ФОТО (${photoFiles.size}):", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickImages.launch(arrayOf("image/*")) }) { Text("ДОБАВИТЬ ФОТО") }
                    if (photoFiles.isNotEmpty()) {
                        OutlinedButton(onClick = { photoFiles.clear() }) { Text("ОЧИСТИТЬ ФОТО") }
                    }
                }
                if (photoFiles.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    photoFiles.forEachIndexed { idx, file ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("• ${file.displayName}", modifier = Modifier.weight(1f))
                                Column {
                                    TextButton(onClick = { 
                                        renameFileIndex = idx
                                        newFileName = file.displayName
                                    }) { 
                                        Text("ПЕРЕИМЕНОВАТЬ", fontSize = 12.sp) 
                                    }
                                    TextButton(onClick = { photoFiles.removeAt(idx) }) { 
                                        Text("УБРАТЬ", fontSize = 12.sp) 
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text("PDF (${pdfFiles.size}):", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickpdfs.launch(arrayOf("application/pdf")) }) { Text("ДОБАВИТЬ PDF") }
                    if (pdfFiles.isNotEmpty()) {
                        OutlinedButton(onClick = { pdfFiles.clear() }) { Text("ОЧИСТИТЬ PDF") }
                    }
                }
                if (pdfFiles.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    pdfFiles.forEachIndexed { idx, file ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("• ${file.displayName}", modifier = Modifier.weight(1f))
                                Column {
                                    TextButton(onClick = { 
                                        renameFileIndex = idx + photoFiles.size // Смещение для PDF файлов
                                        newFileName = file.displayName
                                    }) { 
                                        Text("ПЕРЕИМЕНОВАТЬ", fontSize = 12.sp) 
                                    }
                                    TextButton(onClick = { pdfFiles.removeAt(idx) }) { 
                                        Text("УБРАТЬ", fontSize = 12.sp) 
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        PrimaryButton(
            text = if (existingDocId == null) "СОХРАНИТЬ ДОКУМЕНТ" else "СОХРАНИТЬ ИЗМЕНЕНИЯ",
            onClick = {
                scope.launch {
                    if (existingDocId == null) {

                        val id = uc.createDoc(
                            tplId = templateId,
                            folderId = folderId,
                            name = name.ifBlank { "Документ" },
                            fields = fields.toList(),
                            photos = photoFiles.map { it.uri.toString() },
                            pdfUris = pdfFiles.map { it.uri.toString() }
                        )
                        onSaved(id)
                    } else {
                        val full = uc.getDoc(existingDocId) ?: return@launch
                        val updatedDoc = full.doc.copy(name = name)
                        val updatedFields = fields.mapIndexed { idx, (t, v) ->
                            DocumentField(
                                id = full.fields.getOrNull(idx)?.id ?: newId(),
                                documentId = existingDocId,
                                name = t,
                                valueCipher = v.toByteArray(Charsets.UTF_8),
                                preview = v.take(8),
                                isSecret = false,
                                ord = idx
                            )
                        }
                        val photoAttachments = photoFiles.map { Attachment(newId(), existingDocId, AttachmentKind.photo, null, it.uri, System.currentTimeMillis()) }
                        val pdfAttachments = pdfFiles.map { Attachment(newId(), existingDocId, AttachmentKind.pdfs, "document.pdfs", it.uri, System.currentTimeMillis()) }
                        val fullDocument = DocumentRepository.FullDocument(updatedDoc, updatedFields, photoAttachments, pdfAttachments)
                        uc.updateDoc(fullDocument)
                        onSaved(existingDocId)
                    }
                }
            }
        )
    }

    // Подтверждение удаления поля
    confirmDeleteIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { confirmDeleteIndex = null },
            title = { Text("Удалить поле?") },
            text = { Text("Поле будет удалено из документа.") },
            confirmButton = {
                TextButton(onClick = {
                    if (idx in 0 until fields.size) fields.removeAt(idx)
                    confirmDeleteIndex = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteIndex = null }) { Text("Отмена") }
            }
        )
    }

    // Диалог переименования файла
    renameFileIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { renameFileIndex = null; newFileName = "" },
            title = { Text("Переименовать файл") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Название файла") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFileName.isNotBlank()) {
                        if (idx < photoFiles.size) {
                            // Переименовываем фото
                            val oldFile = photoFiles[idx]
                            photoFiles[idx] = oldFile.copy(displayName = newFileName)
                        } else {
                            // Переименовываем PDF
                            val pdfIdx = idx - photoFiles.size
                            val oldFile = pdfFiles[pdfIdx]
                            pdfFiles[pdfIdx] = oldFile.copy(displayName = newFileName)
                        }
                    }
                    renameFileIndex = null
                    newFileName = ""
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { renameFileIndex = null; newFileName = "" }) { Text("Отмена") }
            }
        )
    }
}