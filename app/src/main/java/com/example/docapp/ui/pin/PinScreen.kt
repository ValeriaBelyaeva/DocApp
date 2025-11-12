package com.example.docapp.ui.pin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import kotlinx.coroutines.launch
import kotlin.UninitializedPropertyAccessException
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppLayout

@Composable
fun PinScreen(onSuccess: () -> Unit) {
    // useCases are not initialized until the PIN is entered; access crypto directly
    var stage by remember { mutableStateOf(PinStage.Loading) }
    var pin by remember { mutableStateOf("") }
    var firstNew by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppLogger.log("PinScreen", "PinScreen initialized")
        try {
            // Ensure ServiceLocator has been initialized
            try {
                // Accessing crypto verifies initialization
                ServiceLocator.crypto.isPinSet()
            } catch (e: UninitializedPropertyAccessException) {
                AppLogger.log("PinScreen", "ERROR: ServiceLocator.crypto is not initialized")
                ErrorHandler.showCriticalError("Application initialization error")
                stage = PinStage.Loading
                return@LaunchedEffect
            }
            
            // Debug info
            ErrorHandler.showInfo("Checking PIN status...")
            
            // Query PIN state directly via CryptoManager
            stage = if (ServiceLocator.crypto.isPinSet()) {
                AppLogger.log("PinScreen", "PIN is set, showing enter existing PIN screen")
                ErrorHandler.showInfo("PIN detected. Please enter the current PIN.")
                PinStage.EnterExisting
            } else {
                AppLogger.log("PinScreen", "PIN is not set, showing create new PIN screen")
                ErrorHandler.showInfo("No PIN found. Create a new PIN.")
                PinStage.EnterNew
            }
        } catch (e: Exception) {
            AppLogger.log("PinScreen", "ERROR: Failed to check PIN status: ${e.message}")
            ErrorHandler.showCriticalError("Application initialization error", e)
            // Stay on loading screen
            stage = PinStage.Loading
        }
    }

    Column(
        modifier = AppLayout.appScreenInsets(Modifier.fillMaxSize())
            .padding(AppDimens.spaceXl), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(AppDimens.space2Xl))
        Text(
            when (stage) {
                PinStage.EnterExisting -> "ENTER PIN CODE"
                PinStage.EnterNew -> "CREATE PIN"
                PinStage.ConfirmNew -> "CONFIRM PIN"
                PinStage.Loading -> "..."
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AppDimens.spaceSm))
        Text(
            text = "•".repeat(pin.length),
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(AppDimens.spaceXl))
        
        // Confirm button
        if (pin.isNotEmpty()) {
            Button(
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            when (stage) {
                                PinStage.EnterExisting -> {
                                    AppLogger.log("PinScreen", "Verifying existing PIN...")
                                    ErrorHandler.showInfo("Verifying existing PIN...")
                                    try {
                                        AppLogger.log("PinScreen", "Verifying PIN and initializing database...")
                                        ErrorHandler.showInfo("Calling ServiceLocator.initializeWithPin...")
                                        ServiceLocator.initializeWithPin(pin, isNewPin = false)
                                        AppLogger.log("PinScreen", "PIN verified and database initialized, proceeding to main screen")
                                        ErrorHandler.showSuccess("PIN verified. Opening app.")
                                        onSuccess()
                                    } catch (e: SecurityException) {
                                        AppLogger.log("PinScreen", "ERROR: Invalid PIN entered")
                                        ErrorHandler.showError("Invalid PIN")
                                        error = "Invalid PIN"
                                        pin = ""
                                        isProcessing = false
                                    } catch (e: Exception) {
                                        AppLogger.log("PinScreen", "ERROR: Failed to verify PIN: ${e.message}")
                                        ErrorHandler.showError("PIN verification error: ${e.message}")
                                        error = "PIN verification error"
                                        pin = ""
                                        isProcessing = false
                                    }
                                }
                                PinStage.EnterNew -> {
                                    AppLogger.log("PinScreen", "New PIN entered, moving to confirmation")
                                    ErrorHandler.showInfo("PIN captured. Proceed to confirmation.")
                                    firstNew = pin
                                    pin = ""
                                    stage = PinStage.ConfirmNew
                                    isProcessing = false
                                }
                                PinStage.ConfirmNew -> {
                                    AppLogger.log("PinScreen", "Confirming new PIN...")
                                    ErrorHandler.showInfo("Confirming new PIN...")
                                    if (pin == firstNew) {
                                        AppLogger.log("PinScreen", "New PIN confirmed, initializing database...")
                                        ErrorHandler.showInfo("PIN confirmed. Initializing database...")
                                        try {
                                            ServiceLocator.initializeWithPin(pin, isNewPin = true)
                                            AppLogger.log("PinScreen", "New PIN set and database initialized, proceeding to main screen")
                                            ErrorHandler.showSuccess("New PIN saved. Opening app.")
                                            onSuccess()
                                        } catch (e: Exception) {
                                            AppLogger.log("PinScreen", "ERROR: Failed to set new PIN: ${e.message}")
                                            ErrorHandler.showError("Failed to save PIN: ${e.message}")
                                            error = "Failed to save PIN"
                                            pin = ""
                                            firstNew = null
                                            stage = PinStage.EnterNew
                                            isProcessing = false
                                        }
                                    } else {
                                        AppLogger.log("PinScreen", "ERROR: PIN confirmation failed")
                                        ErrorHandler.showError("PIN codes do not match")
                                        error = "PIN codes do not match"
                                        pin = ""
                                        firstNew = null
                                        stage = PinStage.EnterNew
                                        isProcessing = false
                                    }
                                }
                                PinStage.Loading -> {
                                    isProcessing = false
                                }
                            }
                        }
                    }
                },
                enabled = !isProcessing && pin.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isProcessing) "Processing..." else "Confirm")
            }
            Spacer(Modifier.height(AppDimens.spaceMd))
        }
        
        Numpad(
            onDigit = { pin += it },
            onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
        )
    }
}

enum class PinStage { Loading, EnterExisting, EnterNew, ConfirmNew }

@Composable
private fun Numpad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),            // Fill to allow proper centering
        verticalArrangement = Arrangement.Center,     // Center vertically
        horizontalAlignment = Alignment.CenterHorizontally) {
        val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { label ->
                    Spacer(Modifier.height(AppDimens.spaceSm))
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
            Spacer(Modifier.height(AppDimens.spaceSm))
        }
    }
}

