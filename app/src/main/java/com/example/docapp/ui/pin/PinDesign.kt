package com.example.docapp.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docapp.R
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ServiceLocator
import com.example.docapp.ui.theme.SurfaceTokens
import com.example.docapp.ui.theme.ThemeConfig
import kotlinx.coroutines.launch
import kotlin.UninitializedPropertyAccessException

/* ────────────────────────────────────────────────────────────
   Палитра и формы — как на макете
   ──────────────────────────────────────────────────────────── */

object PinColors {
    val Bg: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val Layer: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val Neon: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    val Outline: Color
        @Composable get() = MaterialTheme.colorScheme.outline
    val TextPri: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
}

private object PinShapes {
    private val tokens
        @Composable get() = SurfaceTokens.current(ThemeConfig.surfaceStyle)

    val Capsule
        @Composable get() = tokens.shapes.largeCard
}

/* ────────────────────────────────────────────────────────────
   Переиспользуемые куски UI
   ──────────────────────────────────────────────────────────── */

@Composable
private fun RoundKey(
    label: String? = null, 
    isBackspace: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        Modifier
            .size(60.dp) // Уменьшил с 78dp до 60dp для соответствия фото
            .clip(CircleShape)
            .border(1.8.dp, PinColors.Neon, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            isBackspace -> Icon(
                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = null,
                tint = PinColors.Neon,
                modifier = Modifier.size(20.dp) // Уменьшил иконку пропорционально
            )
            !label.isNullOrEmpty() -> Text(
                text = label,
                color = PinColors.Neon,
                fontSize = 24.sp, // Уменьшил с 28sp до 24sp
                fontWeight = FontWeight.Medium
            )
            else -> {} // пустой круг (как на макете)
        }
    }
}

@Composable
private fun LogoBlock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = R.drawable.ic_dm_logo),
            contentDescription = null,
            tint = PinColors.Neon, // логотип в акценте
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "DocManager",
            color = PinColors.Neon,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PinCapsule(isVisible: Boolean = false, actualPin: String = "", onVisibilityToggle: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp) // Фиксированная высота капсулы
            .clip(PinShapes.Capsule)
            .background(PinColors.Layer)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.VpnKey,
            contentDescription = null,
            tint = PinColors.Neon,
            modifier = Modifier.size(20.dp) // Фиксированный размер иконки
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = if (isVisible) {
                // Показываем числа с пробелами
                when {
                    actualPin.isEmpty() -> ""
                    actualPin.length == 1 -> actualPin
                    actualPin.length == 2 -> "${actualPin[0]} ${actualPin[1]}"
                    actualPin.length == 3 -> "${actualPin[0]} ${actualPin[1]} ${actualPin[2]}"
                    actualPin.length == 4 -> "${actualPin[0]} ${actualPin[1]} ${actualPin[2]} ${actualPin[3]}"
                    else -> actualPin
                }
            } else {
                // Показываем звездочки без пробелов
                when {
                    actualPin.isEmpty() -> ""
                    actualPin.length == 1 -> "＊"
                    actualPin.length == 2 -> "＊＊"
                    actualPin.length == 3 -> "＊＊＊"
                    actualPin.length == 4 -> "＊＊＊＊"
                    else -> "＊＊＊＊"
                }
            },
            color = PinColors.Neon,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium, // Фиксированный стиль текста
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (isVisible) "Скрыть PIN" else "Показать PIN",
            tint = PinColors.Neon,
            modifier = Modifier
                .size(20.dp) // Фиксированный размер иконки глазика
                .clickable(onClick = onVisibilityToggle)
        )
    }
}

/* ────────────────────────────────────────────────────────────
   Сам экран: логотип → капсула ввода → круглая клавиатура 3×4
   ──────────────────────────────────────────────────────────── */

@Composable
fun PinScreenDesign() {
    Surface(color = PinColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            LogoBlock()

            Spacer(Modifier.height(28.dp))
            PinCapsule(isVisible = false, actualPin = "1234")

            Spacer(Modifier.height(38.dp))

            // сетка 3×4
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RoundKey("1"); RoundKey("2"); RoundKey("3")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RoundKey("4"); RoundKey("5"); RoundKey("6")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RoundKey("7"); RoundKey("8"); RoundKey("9")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RoundKey()            // пустой слева
                    RoundKey("0")
                    RoundKey(isBackspace = true)
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────
   ФУНКЦИОНАЛЬНАЯ ВЕРСИЯ PIN ЭКРАНА С НОВЫМ ДИЗАЙНОМ
   ──────────────────────────────────────────────────────────── */

@Composable
fun PinScreenNew(onSuccess: () -> Unit) {
    var stage by remember { mutableStateOf(PinStageNew.Loading) }
    var pin by remember { mutableStateOf("") }
    var firstNew by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isPinVisible by remember { mutableStateOf(false) } // Состояние видимости PIN
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppLogger.log("PinScreen", "PinScreen initialized")
        try {
            try {
                ServiceLocator.crypto.isPinSet()
            } catch (e: UninitializedPropertyAccessException) {
                AppLogger.log("PinScreen", "ERROR: ServiceLocator.crypto is not initialized")
                ErrorHandler.showCriticalError("Ошибка инициализации приложения")
                stage = PinStageNew.Loading
                return@LaunchedEffect
            }
            
            ErrorHandler.showInfo("Проверяем состояние PIN...")
            
            stage = if (ServiceLocator.crypto.isPinSet()) {
                AppLogger.log("PinScreen", "PIN is set, showing enter existing PIN screen")
                ErrorHandler.showInfo("PIN установлен, введите существующий PIN")
                PinStageNew.EnterExisting
            } else {
                AppLogger.log("PinScreen", "PIN is not set, showing create new PIN screen")
                ErrorHandler.showInfo("PIN не установлен, создайте новый PIN")
                PinStageNew.EnterNew
            }
        } catch (e: Exception) {
            AppLogger.log("PinScreen", "ERROR: Failed to check PIN status: ${e.message}")
            ErrorHandler.showCriticalError("Ошибка инициализации приложения", e)
            stage = PinStageNew.Loading
        }
    }

    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.getBottom(density).div(density.density).dp

    Surface(color = PinColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = bottomInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            
            // Логотип с заголовком
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_dm_logo),
                    contentDescription = null,
                    tint = PinColors.Neon,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (stage) {
                        PinStageNew.EnterExisting -> "ВВЕДИТЕ ПИН-КОД"
                        PinStageNew.EnterNew -> "НОВЫЙ ПИН"
                        PinStageNew.ConfirmNew -> "ПОДТВЕРДИТЕ ПИН"
                        PinStageNew.Loading -> "DocManager"
                    },
                    color = PinColors.Neon,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(28.dp))
            
            // Капсула с PIN - показываем звездочки или реальный PIN
            PinCapsule(
                isVisible = isPinVisible,
                actualPin = pin,
                onVisibilityToggle = { isPinVisible = !isPinVisible }
            )

            // Ошибка
            error?.let { errorText ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(80.dp)) // Увеличил расстояние до клавиатуры

            // Область между капсулой и клавиатурой, чтобы опустить блок вниз
            Spacer(Modifier.weight(1f))

            // Функциональная клавиатура 3×4 в нижней части экрана
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RoundKey("1") {
                        pin += "1"
                        error = null
                    }
                    RoundKey("2") {
                        pin += "2"
                        error = null
                    }
                    RoundKey("3") {
                        pin += "3"
                        error = null
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RoundKey("4") {
                        pin += "4"
                        error = null
                    }
                    RoundKey("5") {
                        pin += "5"
                        error = null
                    }
                    RoundKey("6") {
                        pin += "6"
                        error = null
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RoundKey("7") {
                        pin += "7"
                        error = null
                    }
                    RoundKey("8") {
                        pin += "8"
                        error = null
                    }
                    RoundKey("9") {
                        pin += "9"
                        error = null
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RoundKey() // пустая кнопка слева
                    RoundKey("0") {
                        pin += "0"
                        error = null
                    }
                    RoundKey(isBackspace = true) {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            error = null
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp + bottomInset))
        }
    }

    // Автоматическая проверка PIN при вводе 4 цифр
    LaunchedEffect(pin) {
        if (pin.length == 4 && !isProcessing) {
            isProcessing = true
            scope.launch {
                try {
                    when (stage) {
                        PinStageNew.EnterExisting -> {
                            AppLogger.log("PinScreen", "Verifying existing PIN...")
                            ErrorHandler.showInfo("Проверяем существующий PIN...")
                            ServiceLocator.initializeWithPin(pin, isNewPin = false)
                            AppLogger.log("PinScreen", "PIN verified and database initialized")
                            ErrorHandler.showSuccess("PIN проверен!")
                            onSuccess()
                        }
                        PinStageNew.EnterNew -> {
                            AppLogger.log("PinScreen", "New PIN entered, moving to confirmation")
                            ErrorHandler.showInfo("PIN введен, переходим к подтверждению")
                            firstNew = pin
                            pin = ""
                            stage = PinStageNew.ConfirmNew
                        }
                        PinStageNew.ConfirmNew -> {
                            AppLogger.log("PinScreen", "Confirming new PIN...")
                            ErrorHandler.showInfo("Подтверждаем новый PIN...")
                            if (pin == firstNew) {
                                AppLogger.log("PinScreen", "New PIN confirmed, initializing database...")
                                ErrorHandler.showInfo("PIN подтвержден, инициализируем базу данных...")
                                ServiceLocator.initializeWithPin(pin, isNewPin = true)
                                AppLogger.log("PinScreen", "New PIN set and database initialized")
                                ErrorHandler.showSuccess("Новый PIN установлен!")
                                onSuccess()
                            } else {
                                AppLogger.log("PinScreen", "ERROR: PIN confirmation failed")
                                ErrorHandler.showError("PIN не совпадает")
                                error = "PIN не совпадает"
                                pin = ""
                                firstNew = null
                                stage = PinStageNew.EnterNew
                            }
                        }
                        PinStageNew.Loading -> {
                            // Ничего не делаем в состоянии загрузки
                        }
                    }
                } catch (e: SecurityException) {
                    AppLogger.log("PinScreen", "ERROR: Invalid PIN entered")
                    ErrorHandler.showError("Неверный PIN")
                    error = "Неверный PIN"
                    pin = ""
                } catch (e: Exception) {
                    AppLogger.log("PinScreen", "ERROR: Failed to process PIN: ${e.message}")
                    ErrorHandler.showError("Ошибка обработки PIN: ${e.message}")
                    error = "Ошибка обработки PIN"
                    pin = ""
                    firstNew = null
                    stage = PinStageNew.EnterNew
                } finally {
                    isProcessing = false
                }
            }
        }
    }
}

enum class PinStageNew { Loading, EnterExisting, EnterNew, ConfirmNew }