package com.example.docapp.ui.template

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.docapp.core.ServiceLocator
import com.example.docapp.domain.Template
import kotlinx.coroutines.launch

@Composable
fun TemplateSelectorScreen(
    folderId: String?,
    onCreateDocFromTemplate: (templateId: String, folderId: String?) -> Unit,
    onCreateEmpty: (folderId: String?) -> Unit
) {
    val uc = ServiceLocator.useCases
    var list by remember { mutableStateOf<List<Template>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Состояния для диалога создания шаблона
    var showDialog by remember { mutableStateOf(false) }
    var tplName by remember { mutableStateOf("") }
    val fieldNames = remember { mutableStateListOf<String>() }
    var newField by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { list = uc.listTemplates() }

    fun resetDialog() {
        tplName = ""
        fieldNames.clear()
        newField = ""
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onCreateEmpty(folderId) }, modifier = Modifier.weight(1f)) { Text("СОЗДАТЬ ДОКУМЕНТ") }
                Button(onClick = { showDialog = true }, modifier = Modifier.weight(1f)) { Text("СОЗДАТЬ ШАБЛОН") }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, true)) {
                items(list) { tpl ->
                    ElevatedCard(onClick = { onCreateDocFromTemplate(tpl.id, folderId) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tpl.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // Диалог создания шаблона
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Новый шаблон") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tplName,
                            onValueChange = { tplName = it },
                            label = { Text("Название шаблона") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newField,
                                onValueChange = { newField = it },
                                label = { Text("Название поля") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val s = newField.trim()
                                    if (s.isNotEmpty()) {
                                        fieldNames.add(s)
                                        newField = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить поле")
                            }
                        }
                        if (fieldNames.isNotEmpty()) {
                            Text("Поля:", style = MaterialTheme.typography.titleSmall)
                            fieldNames.forEach { f ->
                                Text("• $f", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = tplName.trim()
                        if (name.isNotEmpty() && fieldNames.isNotEmpty()) {
                            scope.launch {
                                uc.addTemplate(name, fieldNames.toList())
                                list = uc.listTemplates()
                                resetDialog()
                                showDialog = false
                            }
                        }
                    }) { Text("Создать") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        resetDialog()
                        showDialog = false
                    }) { Text("Отмена") }
                }
            )
        }
    }
}
