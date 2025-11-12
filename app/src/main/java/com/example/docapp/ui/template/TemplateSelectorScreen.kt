package com.example.docapp.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.docapp.core.DataValidator
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ServiceLocator
import com.example.docapp.domain.Template
import com.example.docapp.ui.theme.GlassCard
import com.example.docapp.ui.theme.AppShapes
import com.example.docapp.ui.theme.SurfaceTokens
import com.example.docapp.ui.theme.ThemeConfig
import com.example.docapp.ui.theme.AppColors
import com.example.docapp.ui.theme.AppLayout
import com.example.docapp.ui.theme.AppDimens
import kotlinx.coroutines.launch

@Composable
fun TemplateSelectorScreen(
    folderId: String?,
    onCreateDocFromTemplate: (templateId: String, folderId: String?) -> Unit,
    onCreateEmpty: (folderId: String?) -> Unit
) {
    val uc = ServiceLocator.useCases
    var templates by remember { mutableStateOf<List<Template>>(emptyList()) }
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var tplName by remember { mutableStateOf("") }
    val fieldNames = remember { mutableStateListOf<String>() }
    var newField by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        templates = uc.listTemplates()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = AppLayout.appScreenInsets(Modifier.fillMaxSize())
                .padding(AppDimens.screenPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing),
                contentPadding = PaddingValues(
                    top = AppDimens.sectionSpacing,
                    bottom = AppDimens.bottomButtonsSpacer
                )
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Create a document",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(AppDimens.listSpacing))
                        Text(
                        text = "Choose how to start: blank document or template",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                item {
                    Row(
                         modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing)
                    ) {
                        TemplateOptionCard(
                            icon = Icons.Default.Description,
                            title = "Blank document",
                            subtitle = "Start from scratch",
                            modifier = Modifier.weight(1f),
                            onClick = { onCreateEmpty(folderId) }
                        )
                        TemplateOptionCard(
                            icon = Icons.Default.ContentCopy,
                            title = "New template",
                            subtitle = "Save structure",
                            modifier = Modifier.weight(1f),
                            onClick = { showDialog = true }
                        )
                    }
                }

                if (templates.isNotEmpty()) {
                    item {
                        Text(
                        text = "Available templates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(templates) { template ->
                        TemplateListItem(
                            template = template,
                            onOpen = { onCreateDocFromTemplate(template.id, folderId) },
                            onDelete = {
                                scope.launch {
                                    try {
                                        uc.deleteTemplate(template.id)
                                        templates = uc.listTemplates()
                                        ErrorHandler.showSuccess("Template removed")
                                    } catch (e: Exception) {
                                        ErrorHandler.showError("Failed to delete template: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                } else {
                    item { TemplateEmptyState() }
                }
            }

            if (showDialog) {
                TemplateDialog(
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
                                templates = uc.listTemplates()
                                tplName = ""
                                newField = ""
                                fieldNames.clear()
                                showDialog = false
                            }
                        } else {
                            val errors = buildList {
                                if (!nameValidation.isSuccess) add(nameValidation.getError()!!)
                                fieldValidations.forEachIndexed { index, validation ->
                                    if (!validation.isSuccess) add("Field ${index + 1}: ${validation.getError()}")
                                }
                            }
                            ErrorHandler.showError(errors.joinToString("\n"))
                        }
                    },
                    confirmEnabled = tplName.trim().isNotEmpty() && fieldNames.isNotEmpty()
                )
            }
        }
    }
}

@Composable
private fun TemplateOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.heightIn(min = 180.dp),
        onClick = onClick,
        shape = AppShapes.panelMedium()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.iconAccent(),
                modifier = Modifier
                    .size(44.dp)
                    .padding(AppDimens.spaceXs)
            )
            Spacer(Modifier.height(AppDimens.listSpacing))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
            Spacer(Modifier.height(AppDimens.labelSpacing))
                        Text(
                text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
@Composable
private fun TemplateListItem(
    template: Template,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        shape = AppShapes.listItem()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(AppDimens.labelSpacing))
                                Text(
                    text = "Template",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                                    Icon(
                    imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete template",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

@Composable
private fun TemplateEmptyState() {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = AppShapes.panelLarge()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                .padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.spaceXl
                ),
            horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No templates yet",
                        style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
            Spacer(Modifier.height(AppDimens.listSpacing))
                    Text(
                text = "Create a template to speed up repetitive documents",
                        style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

@Composable
private fun TemplateDialog(
    tplName: String,
    onNameChange: (String) -> Unit,
    newField: String,
    onFieldChange: (String) -> Unit,
    fields: List<String>,
    onAddField: () -> Unit,
    onRemoveField: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean
) {
            AlertDialog(
        onDismissRequest = onDismiss,
                title = { 
                    Text(
                        text = "New template",
                        style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)) {
                        OutlinedTextField(
                            value = tplName,
                    onValueChange = onNameChange,
                            label = { Text("Template name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = AppShapes.panelSmall(),
                            colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = AppColors.iconAccent(),
                        focusedBorderColor = AppColors.iconAccent(),
                        unfocusedBorderColor = AppColors.level2Background(),
                        focusedLabelColor = AppColors.iconAccent(),
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                        
                        Text(
                    text = "Add fields for the template",
                            style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.listSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newField,
                        onValueChange = onFieldChange,
                                label = { Text("Field name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = AppShapes.panelSmall(),
                                colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = AppColors.iconAccent(),
                            focusedBorderColor = AppColors.iconAccent(),
                            unfocusedBorderColor = AppColors.level2Background(),
                            focusedLabelColor = AppColors.iconAccent(),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = onAddField,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(AppShapes.iconButton())
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add field",
                            tint = AppColors.iconAccent()
                        )
                            }
                        }
                        
                if (fields.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.panelMedium())
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(
                                horizontal = AppDimens.dialogPaddingHorizontal,
                                vertical = AppDimens.dialogPaddingVertical
                            ),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.listSpacing)
                    ) {
                                    Text(
                            text = "Template fields",
                                        style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                                    )
                        fields.forEach { field ->
                                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                    text = field,
                                                style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onRemoveField(field) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
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
                    containerColor = AppColors.iconAccent(),
                    contentColor = AppColors.background(),
                    disabledContainerColor = AppColors.iconAccent().copy(alpha = 0.3f),
                    disabledContentColor = AppColors.background().copy(alpha = 0.5f)
                ),
                shape = AppShapes.panelMedium()
                        ) {
                            Text("Create template")
                        }
                },
                dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                        Text("Cancel") 
                    }
                }
            )
}
