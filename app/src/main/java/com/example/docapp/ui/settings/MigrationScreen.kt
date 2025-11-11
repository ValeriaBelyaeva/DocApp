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
            text = "Управление вложениями",
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
                        text = "Миграция старых URI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                
                Text(
                    text = "Переносит все внешние URI в локальное хранилище приложения для обеспечения стабильного доступа к файлам.",
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
                                    ErrorHandler.showInfo("Запуск миграции старых URI...")
                                    
                                    // Симуляция прогресса
                                    migrationProgress = 0.3f
                                    
                                    val result = useCases.migrateExternalUris(context)
                                    
                                    migrationProgress = 1f
                                    migrationResult = "Миграция завершена: ${result.migratedDocuments} документов, ${result.migratedAttachments} вложений"
                                    
                                    if (result.errors == 0) {
                                        ErrorHandler.showSuccess("Миграция завершена успешно")
                                    } else {
                                        ErrorHandler.showWarning("Миграция завершена с ошибками: ${result.errors}")
                                    }
                                    
                                } catch (e: Exception) {
                                    AppLogger.log("MigrationScreen", "ERROR: Migration failed: ${e.message}")
                                    ErrorHandler.showError("Ошибка миграции: ${e.message}")
                                    migrationResult = "Ошибка миграции: ${e.message}"
                                } finally {
                                    isMigrating = false
                                }
                            }
                        }
                    },
                    enabled = !isMigrating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isMigrating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    }
                    Text(if (isMigrating) "Миграция..." else "Запустить миграцию")
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
                        text = "Очистка неиспользуемых файлов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                
                Text(
                    text = "Удаляет файлы, которые не привязаны ни к одному документу (сироты).",
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
                                    ErrorHandler.showInfo("Запуск очистки неиспользуемых файлов...")
                                    
                                    val result = useCases.cleanupOrphans()
                                    cleanupResult = result
                                    
                                    if (result.errors == 0 && result.deletedFiles > 0) {
                                        ErrorHandler.showSuccess("Очистка завершена: удалено ${result.deletedFiles} файлов")
                                    } else if (result.errors == 0 && result.deletedFiles == 0) {
                                        ErrorHandler.showInfo("Неиспользуемые файлы не найдены")
                                    } else {
                                        ErrorHandler.showWarning("Очистка завершена с ошибками: ${result.errors}")
                                    }
                                    
                                } catch (e: Exception) {
                                    AppLogger.log("MigrationScreen", "ERROR: Cleanup failed: ${e.message}")
                                    ErrorHandler.showError("Ошибка очистки: ${e.message}")
                                } finally {
                                    isCleaningUp = false
                                }
                            }
                        }
                    },
                    enabled = !isCleaningUp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCleaningUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    }
                    Text(if (isCleaningUp) "Очистка..." else "Запустить очистку")
                }
                
                cleanupResult?.let { result ->
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    Text(
                        text = "Результат: ${result.deletedFiles} файлов удалено, ${result.deletedRecords} записей, ${result.errors} ошибок",
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    Text(
                        text = "Информация",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                
                Text(
                    text = "• Миграция выполняется один раз при обновлении приложения\n" +
                            "• Очистку можно запускать многократно для освобождения места\n" +
                            "• Все операции безопасны и не влияют на существующие документы",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Диалог прогресса миграции
    if (showMigrationDialog) {
        AlertDialog(
            onDismissRequest = { /* Нельзя отменить */ },
            title = { Text("Миграция URI") },
            text = {
                Column {
                    Text("Миграция внешних URI в локальное хранилище...")
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
                    Text("Закрыть")
                }
            }
        )
    }
}
