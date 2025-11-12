package com.example.docapp.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ServiceLocator
import com.example.docapp.data.storage.FileGc
import com.example.docapp.ui.theme.GlassCard
import com.example.docapp.ui.theme.AppColors
import kotlinx.coroutines.launch
import com.example.docapp.ui.theme.AppDimens

@Composable
fun MigrationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useCases = ServiceLocator.useCases
    
    var isMigrating by remember { mutableStateOf(false) }
    var migrationProgress by remember { mutableStateOf(0f) }
    var showMigrationDialog by remember { mutableStateOf(false) }
    var migrationResult by remember { mutableStateOf<String?>(null) }
    
    var isCleaningUp by remember { mutableStateOf(false) }
    var cleanupResult by remember { mutableStateOf<FileGc.CleanupResult?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing)
    ) {
        Text(
            text = "Attachment management",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Карточка миграции
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = AppColors.iconAccent()
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    Text(
                        text = "Migrate legacy URIs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                
                Text(
                    text = "Moves external URIs into local app storage to keep attachments accessible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                
                Button(
                    onClick = {
                        if (!isMigrating) {
                            scope.launch {
                                isMigrating = true
                                showMigrationDialog = true
                                migrationProgress = 0f
                                
                                try {
                                    AppLogger.log("MigrationScreen", "Starting migration...")
                                    ErrorHandler.showInfo("Starting legacy URI migration...")
                                    
                                    // Симуляция прогресса
                                    migrationProgress = 0.3f
                                    
                                    val result = useCases.migrateExternalUris(context)
                                    
                                    migrationProgress = 1f
                                    migrationResult = "Migration finished: ${result.migratedDocuments} documents, ${result.migratedAttachments} attachments"
                                    
                                    if (result.errors == 0) {
                                        ErrorHandler.showSuccess("Migration completed successfully")
                                    } else {
                                        ErrorHandler.showWarning("Migration completed with ${result.errors} errors")
                                    }
                                    
                                } catch (e: Exception) {
                                    AppLogger.log("MigrationScreen", "ERROR: Migration failed: ${e.message}")
                                    ErrorHandler.showError("Migration failed: ${e.message}")
                                    migrationResult = "Migration failed: ${e.message}"
                                } finally {
                                    isMigrating = false
                                }
                            }
                        }
                    },
                    enabled = !isMigrating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.iconAccent(),
                        contentColor = AppColors.background(),
                        disabledContainerColor = AppColors.iconAccent().copy(alpha = 0.3f),
                        disabledContentColor = AppColors.background().copy(alpha = 0.5f)
                    )
                ) {
                    if (isMigrating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    }
                    Text(if (isMigrating) "Migrating..." else "Start migration")
                }
                
                migrationResult?.let { result ->
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Карточка очистки
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = AppColors.iconAccent()
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    Text(
                        text = "Clean unused files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                
                Text(
                    text = "Deletes files that are no longer linked to any document.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                
                Button(
                    onClick = {
                        if (!isCleaningUp) {
                            scope.launch {
                                isCleaningUp = true
                                
                                try {
                                    AppLogger.log("MigrationScreen", "Starting cleanup...")
                                    ErrorHandler.showInfo("Starting orphan cleanup...")
                                    
                                    val result = useCases.cleanupOrphans()
                                    cleanupResult = result
                                    
                                    if (result.errors == 0 && result.deletedFiles > 0) {
                                        ErrorHandler.showSuccess("Cleanup complete: deleted ${result.deletedFiles} files")
                                    } else if (result.errors == 0 && result.deletedFiles == 0) {
                                        ErrorHandler.showInfo("No orphan files found")
                                    } else {
                                        ErrorHandler.showWarning("Cleanup finished with ${result.errors} errors")
                                    }
                                    
                                } catch (e: Exception) {
                                    AppLogger.log("MigrationScreen", "ERROR: Cleanup failed: ${e.message}")
                                    ErrorHandler.showError("Cleanup failed: ${e.message}")
                                } finally {
                                    isCleaningUp = false
                                }
                            }
                        }
                    },
                    enabled = !isCleaningUp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.iconAccent(),
                        contentColor = AppColors.background(),
                        disabledContainerColor = AppColors.iconAccent().copy(alpha = 0.3f),
                        disabledContentColor = AppColors.background().copy(alpha = 0.5f)
                    )
                ) {
                    if (isCleaningUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    }
                    Text(if (isCleaningUp) "Cleaning..." else "Start cleanup")
                }
                
                cleanupResult?.let { result ->
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    Text(
                        text = "Result: ${result.deletedFiles} files removed, ${result.deletedRecords} records, ${result.errors} errors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Информационная карточка
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = AppColors.iconAccent()
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    Text(
                        text = "Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                
                Text(
                    text = "• Migration runs once after updating the app.\n" +
                            "• Cleanup can run multiple times to free storage.\n" +
                            "• Operations are safe and do not affect existing documents.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Диалог прогресса миграции
    if (showMigrationDialog) {
        AlertDialog(
            onDismissRequest = { /* cannot cancel */ },
            title = { Text("URI migration") },
            text = {
                Column {
                    Text("Moving external URIs into local storage...")
                    Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                    LinearProgressIndicator(
                        progress = { migrationProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    Text(
                        text = "${(migrationProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        if (!isMigrating) {
                            showMigrationDialog = false
                        }
                    },
                    enabled = !isMigrating
                ) {
                    Text("Close")
                }
            }
        )
    }
}
