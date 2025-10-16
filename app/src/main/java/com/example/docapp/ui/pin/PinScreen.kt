package com.example.docapp.ui.pin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import kotlinx.coroutines.launch
import kotlin.UninitializedPropertyAccessException

@Composable
fun PinScreen(onSuccess: () -> Unit) {
    // useCases не инициализированы до ввода PIN, используем crypto напрямую
    var stage by remember { mutableStateOf(PinStage.Loading) }
    var pin by remember { mutableStateOf("") }
    var firstNew by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppLogger.log("PinScreen", "PinScreen initialized")
        try {
            // Проверяем, что ServiceLocator инициализирован
            try {
                // Попытка доступа к crypto проверит инициализацию
                ServiceLocator.crypto.isPinSet()
            } catch (e: UninitializedPropertyAccessException) {
                AppLogger.log("PinScreen", "ERROR: ServiceLocator.crypto is not initialized")
                ErrorHandler.showCriticalError("Ошибка инициализации приложения")
                stage = PinStage.Loading
                return@LaunchedEffect
            }
            
            // Добавляем дебажное сообщение
            ErrorHandler.showInfo("Проверяем состояние PIN...")
            
            // Проверяем PIN через CryptoManager напрямую, а не через useCases
            stage = if (ServiceLocator.crypto.isPinSet()) {
                AppLogger.log("PinScreen", "PIN is set, showing enter existing PIN screen")
                ErrorHandler.showInfo("PIN установлен, введите существующий PIN")
                PinStage.EnterExisting
            } else {
                AppLogger.log("PinScreen", "PIN is not set, showing enter new PIN screen")
                ErrorHandler.showInfo("PIN не установлен, создайте новый PIN")
                PinStage.EnterNew
            }
        } catch (e: Exception) {
            AppLogger.log("PinScreen", "ERROR: Failed to check PIN status: ${e.message}")
            ErrorHandler.showCriticalError("Ошибка инициализации приложения", e)
            // Оставляем на Loading экране
            stage = PinStage.Loading
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Text(
            when (stage) {
                PinStage.EnterExisting -> "ВВЕДИТЕ ПИН-КОД"
                PinStage.EnterNew -> "НОВЫЙ ПИН"
                PinStage.ConfirmNew -> "ПОДТВЕРДИТЕ ПИН"
                PinStage.Loading -> "..."
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = pin.padEnd(4, '•'),
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))
        Numpad(
            onDigit = {
                if (pin.length < 4) pin += it
                if (pin.length == 4 && !isProcessing) {
                    isProcessing = true
                    scope.launch {
                        when (stage) {
                            PinStage.EnterExisting -> {
                                AppLogger.log("PinScreen", "Verifying existing PIN...")
                                ErrorHandler.showInfo("Проверяем существующий PIN...")
                                try {
                                    AppLogger.log("PinScreen", "Verifying PIN and initializing database...")
                                    ErrorHandler.showInfo("Вызываем ServiceLocator.initializeWithPin...")
                                    // initializeWithPin() сам проверит PIN и инициализирует БД (без флага isNewPin)
                                    ServiceLocator.initializeWithPin(pin, isNewPin = false)
                                    AppLogger.log("PinScreen", "PIN verified and database initialized, proceeding to main screen")
                                    ErrorHandler.showSuccess("PIN проверен! Переходим к основному экрану")
                                    onSuccess()
                                } catch (e: SecurityException) {
                                    // Неверный PIN
                                    AppLogger.log("PinScreen", "ERROR: Invalid PIN entered")
                                    ErrorHandler.showWarning("Неверный PIN-код")
                                    error = "Неверный PIN"
                                    pin = ""
                                } catch (e: Exception) {
                                    AppLogger.log("PinScreen", "ERROR: Failed to initialize with PIN: ${e.message}")
                                    ErrorHandler.showError("Ошибка инициализации: ${e.message}", e)
                                    error = "Ошибка инициализации"
                                    pin = ""
                                } finally {
                                    isProcessing = false
                                }
                            }
                            PinStage.EnterNew -> { 
                                AppLogger.log("PinScreen", "New PIN entered, setting PIN and asking for confirmation...")
                                ErrorHandler.showInfo("Устанавливаем новый PIN...")
                                try {
                                    // Устанавливаем PIN и создаем базу данных при первом вводе
                                    ErrorHandler.showInfo("Вызываем ServiceLocator.initializeWithPin...")
                                    ServiceLocator.initializeWithPin(pin, isNewPin = true)
                                    AppLogger.log("PinScreen", "PIN set and database created, asking for confirmation...")
                                    ErrorHandler.showSuccess("PIN установлен! Подтвердите PIN")
                                    firstNew = pin; pin = ""; stage = PinStage.ConfirmNew 
                                } catch (e: Exception) {
                                    AppLogger.log("PinScreen", "ERROR: Failed to set new PIN: ${e.message}")
                                    ErrorHandler.showError("Ошибка установки нового PIN-кода: ${e.message}", e)
                                    error = "Ошибка установки PIN"
                                    pin = ""
                                } finally {
                                    isProcessing = false
                                }
                            }
                            PinStage.ConfirmNew -> {
                                try {
                                    ErrorHandler.showInfo("Проверяем подтверждение PIN...")
                                    if (firstNew == pin) {
                                        AppLogger.log("PinScreen", "PIN confirmed, PIN already set from first entry, proceeding to main screen...")
                                        ErrorHandler.showSuccess("PIN подтвержден! Переходим к основному экрану")
                                        // PIN уже установлен при первом вводе, просто переходим к основному экрану
                                        // База данных уже создана при первом вводе PIN
                                        onSuccess()
                                    } else {
                                        AppLogger.log("PinScreen", "ERROR: PIN confirmation failed")
                                        ErrorHandler.showWarning("PIN-коды не совпадают")
                                        error = "PIN не совпал"
                                        pin = ""; firstNew = null; stage = PinStage.EnterNew
                                    }
                                } catch (e: Exception) {
                                    AppLogger.log("PinScreen", "ERROR: Failed to confirm PIN: ${e.message}")
                                    ErrorHandler.showError("Ошибка подтверждения PIN-кода: ${e.message}", e)
                                    error = "Ошибка подтверждения PIN"
                                    pin = ""; firstNew = null; stage = PinStage.EnterNew
                                } finally {
                                    isProcessing = false
                                }
                            }
                            else -> {}
                        }
                    }
                }
            },
            onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
        )
    }
}

private enum class PinStage { Loading, EnterExisting, EnterNew, ConfirmNew }

@Composable
private fun Numpad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),            // важно: чтобы было что центрировать
        verticalArrangement = Arrangement.Center,     // центр по вертикали
        horizontalAlignment = Alignment.CenterHorizontally) {
        val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { label ->
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            when (label) {
                                "" -> {}
                                "⌫" -> onDelete()
                                else -> onDigit(label)
                            }
                        },
                        modifier = Modifier.size(80.dp, 56.dp)
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
