package com.example.docapp.ui.pin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.docapp.core.ServiceLocator
import kotlinx.coroutines.launch

@Composable
fun PinScreen(onSuccess: () -> Unit) {
    val uc = ServiceLocator.useCases
    var stage by remember { mutableStateOf(PinStage.Loading) }
    var pin by remember { mutableStateOf("") }
    var firstNew by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        stage = if (uc.isPinSet()) PinStage.EnterExisting else PinStage.EnterNew
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
                if (pin.length == 4) {
                    scope.launch {
                        when (stage) {
                            PinStage.EnterExisting -> {
                                val ok = uc.verifyPin(pin)
                                if (ok) onSuccess() else { error = "Неверный PIN"; pin = "" }
                            }
                            PinStage.EnterNew -> { firstNew = pin; pin = ""; stage = PinStage.ConfirmNew }
                            PinStage.ConfirmNew -> {
                                if (firstNew == pin) {
                                    uc.setNewPin(pin)
                                    onSuccess()
                                } else {
                                    error = "PIN не совпал"
                                    pin = ""; firstNew = null; stage = PinStage.EnterNew
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
