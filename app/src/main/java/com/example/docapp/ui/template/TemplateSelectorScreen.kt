package com.example.docapp.ui.template

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.DataValidator
import com.example.docapp.domain.Template
import com.example.docapp.ui.theme.GlassCard
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

        LaunchedEffect(Unit) {
            list = uc.listTemplates()
        }

    fun resetDialog() {
        tplName = ""
        fieldNames.clear()
        newField = ""
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(20.dp)
        ) {
            // Заголовок
            Text(
                text = "Создание документа",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Выберите способ создания документа",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Кнопки создания
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Кнопка создания пустого документа
                GlassCard(
                    onClick = { onCreateEmpty(folderId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Пустой документ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Начать с нуля",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Кнопка создания шаблона
                GlassCard(
                    onClick = { showDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Новый шаблон",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Создать шаблон",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Секция шаблонов
            if (list.isNotEmpty()) {
                Text(
                    text = "Доступные шаблоны",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                items(list) { tpl ->
                        GlassCard(
                            onClick = { onCreateDocFromTemplate(tpl.id, folderId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tpl.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Кнопка удаления шаблона
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                uc.deleteTemplate(tpl.id)
                                                list = uc.listTemplates()
                                                ErrorHandler.showSuccess("Шаблон удален")
                                            } catch (e: Exception) {
                                                ErrorHandler.showError("Не удалось удалить шаблон: ${e.message}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить шаблон",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Пустое состояние
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Нет доступных шаблонов",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Создайте свой первый шаблон",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Диалог создания шаблона
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { 
                    Text(
                        text = "Новый шаблон",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = tplName,
                            onValueChange = { tplName = it },
                            label = { Text("Название шаблона") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                        
                        Text(
                            text = "Добавьте поля для шаблона:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newField,
                                onValueChange = { newField = it },
                                label = { Text("Название поля") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                )
                            )
                            FilledIconButton(
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
                            GlassCard {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Поля шаблона:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                            fieldNames.forEach { f ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "• ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = f,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                        Button(
                            onClick = {
                                val nameValidation = DataValidator.validateTemplateName(tplName)
                                val fieldValidations = fieldNames.map { DataValidator.validateFieldName(it) }
                                
                                if (nameValidation.isSuccess && fieldValidations.all { it.isSuccess }) {
                                    scope.launch {
                                        val normalizedName = nameValidation.getValue()!!
                                        val normalizedFields = fieldValidations.map { it.getValue()!! }
                                        uc.addTemplate(normalizedName, normalizedFields)
                                        list = uc.listTemplates()
                                        resetDialog()
                                        showDialog = false
                                    }
                                } else {
                                    val errors = mutableListOf<String>()
                                    if (!nameValidation.isSuccess) {
                                        errors.add(nameValidation.getError()!!)
                                    }
                                    fieldValidations.forEachIndexed { index, validation ->
                                        if (!validation.isSuccess) {
                                            errors.add("Поле ${index + 1}: ${validation.getError()}")
                                        }
                                    }
                                    ErrorHandler.showError(errors.joinToString("\n"))
                                }
                            },
                            enabled = tplName.trim().isNotEmpty() && fieldNames.isNotEmpty()
                        ) {
                            Text("Создать шаблон")
                        }
                },
                dismissButton = {
                    TextButton(onClick = {
                        resetDialog()
                        showDialog = false
                    }) { 
                        Text("Отмена") 
                    }
                }
            )
        }
    }
}
