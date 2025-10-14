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
                    OutlinedButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(photo.uri, "image/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(intent)
                    }) { Text("ОТКРЫТЬ ФОТО ${index + 1}") }
                }
            }

            // PDF
            item { Spacer(Modifier.height(12.dp)); Text("PDF", style = MaterialTheme.typography.titleMedium) }
            full?.pdfs?.let { pdfs ->
                itemsIndexed(pdfs) { index, pdf ->
                    OutlinedButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(pdf.uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(intent)
                    }) { Text("ОТКРЫТЬ PDF ${index + 1}") }
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

    // несколько фото
    val photoUris = remember { mutableStateListOf<Uri>() }
    val pdfsUris = remember { mutableStateListOf<Uri>() }

    var confirmDeleteIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(existingDocId, templateId) {
        fields.clear()
        photoUris.clear()
        pdfsUris.clear()

        if (existingDocId != null) {
            val f = uc.getDoc(existingDocId)
            if (f != null) {
                name = f.doc.name
                fields.addAll(f.fields.map { it.name to (it.valueCipher?.toString(Charsets.UTF_8) ?: "") })
                photoUris.addAll(f.photos.map { it.uri })
                pdfsUris.addAll(f.pdfs.map { it.uri })
            }
        } else if (templateId != null) {
            val tf = uc.listTemplateFields(templateId)
            fields.addAll(tf.sortedBy { it.ord }.map { it.name to "" })
            name = "Документ"
        }
    }

    // pickers
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris != null) photoUris.addAll(uris)
    }
    val pickpdfs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris != null) pdfsUris.addAll(uris)
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
                Text("ФОТО (${photoUris.size}):", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickImages.launch(arrayOf("image/*")) }) { Text("ДОБАВИТЬ ФОТО") }
                    if (photoUris.isNotEmpty()) {
                        OutlinedButton(onClick = { photoUris.clear() }) { Text("ОЧИСТИТЬ ФОТО") }
                    }
                }
                if (photoUris.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    photoUris.forEachIndexed { idx, uri ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("• ${uri}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { photoUris.removeAt(idx) }) { Text("УБРАТЬ") }
                        }
                    }
                }
            }
            item {
                /*
                Spacer(Modifier.height(12.dp))
                Text("pdfs: ${pdfsUri?.toString() ?: "не прикреплён"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickpdfs.launch(arrayOf("application/pdfs")) }) { Text("ПРИКРЕПИТЬ pdfs") }
                    if (pdfsUri != null) OutlinedButton(onClick = { pdfsUri = null }) { Text("УБРАТЬ") }
                }
                */

                Text("PDF (${pdfsUris.size}):", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickpdfs.launch(arrayOf("application/pdf")) }) { Text("ДОБАВИТЬ PDF") }
                    if (pdfsUris.isNotEmpty()) {
                        OutlinedButton(onClick = { pdfsUris.clear() }) { Text("ОЧИСТИТЬ PDF") }
                    }
                }
                if (pdfsUris.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    pdfsUris.forEachIndexed { idx, uri ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("• ${uri}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { pdfsUris.removeAt(idx) }) { Text("УБРАТЬ") }
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
                            photoUris = photoUris.map { it.toString() },
                            pdfsUri   = pdfsUris.map { it.toString() }
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
                        val attachments =
                            photoUris.map { Attachment(newId(), existingDocId, AttachmentKind.photo, null, it, System.currentTimeMillis()) } +
                                    pdfsUris.map { Attachment(newId(), existingDocId, AttachmentKind.pdfs, "document.pdfs", it, System.currentTimeMillis()) } //listOfNotNull(pdfsUri?.let { Attachment(newId(), existingDocId, AttachmentKind.pdfs, "document.pdfs", it, System.currentTimeMillis()) })
                        uc.updateDoc(DocumentRepository.FullDocument(updatedDoc, updatedFields, attachments.filter { it.kind == AttachmentKind.photo }, attachments.filter {it.kind == AttachmentKind.pdfs}))
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
}