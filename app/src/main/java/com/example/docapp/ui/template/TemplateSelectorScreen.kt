package com.example.docapp.ui.template

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.docapp.ui.theme.NeonColorScheme
import com.example.docapp.ui.theme.NeonShapes
import com.example.docapp.ui.theme.NeonTokens
import com.example.docapp.core.DataValidator
import com.example.docapp.core.ErrorHandler
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
    val colors = NeonTokens.darkColors
    val shapes = NeonTokens.shapes

    var showDialog by remember { mutableStateOf(false) }
    var tplName by remember { mutableStateOf("") }
    val fieldNames = remember { mutableStateListOf<String>() }
    var newField by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        list = uc.listTemplates()
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(top = 32.dp, bottom = 140.dp)
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Создание документа",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Выбери способ: новый документ или заготовка",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NeonTemplateOptionCard(
                            icon = Icons.Default.Description,
                            title = "Пустой документ",
                            subtitle = "Начать с нуля",
                            onClick = { onCreateEmpty(folderId) },
                            modifier = Modifier.weight(1f),
                            colors = colors,
                            shapes = shapes
                        )
                        NeonTemplateOptionCard(
                            icon = Icons.Default.ContentCopy,
                            title = "Новый шаблон",
                            subtitle = "Сохранить структуру",
                            onClick = { showDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = colors,
                            shapes = shapes
                        )
                    }
                }

                if (list.isNotEmpty()) {
                    item {
                        Text(
                            text = "Доступные шаблоны",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }

                    items(list) { tpl ->
                        NeonTemplateListItem(
                            template = tpl,
                            onClick = { onCreateDocFromTemplate(tpl.id, folderId) },
                            onDelete = {
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
                        )
                    }
                } else {
                    item { NeonTemplateEmptyState() }
                }
            }

            if (showDialog) {
                NeonTemplateDialog(
                    tplName = tplName,
                    onNameChange = { tplName = it },
                    newField = newField,
                    onFieldChange = { newField = it },
                    fields = fieldNames,
                    onAddField = {
                        val trimmed = newField.trim()
                        if (trimmed.isNotEmpty()) {
                            fieldNames.add(trimmed)
                            newField = ""
                        }
                    },
                    onRemoveField = { fieldNames.remove(it) },
                    onDismiss = {
                        tplName = ""
                        newField = ""
                        fieldNames.clear()
                        showDialog = false
                    },
                    onConfirm = {
                        val nameValidation = DataValidator.validateTemplateName(tplName)
                        val fieldValidations = fieldNames.map { DataValidator.validateFieldName(it) }

                        if (nameValidation.isSuccess && fieldValidations.all { it.isSuccess }) {
                            scope.launch {
                                val normalizedName = nameValidation.getValue()!!
                                val normalizedFields = fieldValidations.map { it.getValue()!! }
                                uc.addTemplate(normalizedName, normalizedFields)
                                list = uc.listTemplates()
                                tplName = ""
                                newField = ""
                                fieldNames.clear()
                                showDialog = false
                            }
                        } else {
                            val errors = buildList {
                                if (!nameValidation.isSuccess) add(nameValidation.getError()!!)
                                fieldValidations.forEachIndexed { index, validation ->
                                    if (!validation.isSuccess) add("Поле ${index + 1}: ${validation.getError()}")
                                }
                            }
                            ErrorHandler.showError(errors.joinToString("\n"))
                        }
                    },
                    confirmEnabled = tplName.trim().isNotEmpty() && fieldNames.isNotEmpty(),
                    colors = colors,
                    shapes = shapes
                )
            }
        }
    }
}

@Composable
private fun NeonTemplateOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: NeonColorScheme = NeonTokens.darkColors,
    shapes: NeonShapes = NeonTokens.shapes
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = shapes.largeCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.2.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.badgePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NeonTemplateListItem(
    template: Template,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    colors: NeonColorScheme = NeonTokens.darkColors,
    shapes: NeonShapes = NeonTokens.shapes
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shapes.mediumCard,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceMuted),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Шаблон",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить шаблон",
                    tint = colors.accentWarning
                )
            }
        }
    }
}

@Composable
private fun NeonTemplateEmptyState(
    colors: NeonColorScheme = NeonTokens.darkColors,
    shapes: NeonShapes = NeonTokens.shapes
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.largeCard)
            .background(colors.surface)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Нет доступных шаблонов",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Создай шаблон, чтобы ускорить повторяющиеся документы",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NeonTemplateDialog(
    tplName: String,
    onNameChange: (String) -> Unit,
    newField: String,
    onFieldChange: (String) -> Unit,
    fields: List<String>,
    onAddField: () -> Unit,
    onRemoveField: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    colors: NeonColorScheme = NeonTokens.darkColors,
    shapes: NeonShapes = NeonTokens.shapes
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Новый шаблон",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = tplName,
                    onValueChange = onNameChange,
                    label = { Text("Название шаблона") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accent,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.outline,
                        focusedLabelColor = colors.accent,
                        unfocusedLabelColor = colors.textSecondary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Text(
                    text = "Добавь поля для шаблона",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newField,
                        onValueChange = onFieldChange,
                        label = { Text("Название поля") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.accent,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.outline,
                            focusedLabelColor = colors.accent,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = onAddField,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.badgePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить поле",
                            tint = colors.accent
                        )
                    }
                }

                if (fields.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.mediumCard)
                            .background(colors.surfaceMuted)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Поля шаблона",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        fields.forEach { field ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = field,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onRemoveField(field) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = colors.accentWarning
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
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background,
                    disabledContainerColor = colors.accent.copy(alpha = 0.3f),
                    disabledContentColor = colors.background.copy(alpha = 0.5f)
                ),
                shape = shapes.button
            ) {
                Text("Создать шаблон")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary)
            ) {
                Text("Отмена")
            }
        }
    )
}
