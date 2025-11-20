package com.example.docapp.ui.pin
import androidx.activity.compose.BackHandler
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
import com.example.docapp.R
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ServiceLocator
import com.example.docapp.ui.theme.AppShapes
import com.example.docapp.ui.theme.AppColors
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppBorderWidths
import com.example.docapp.ui.theme.AppFontSizes
import kotlinx.coroutines.launch
import kotlin.UninitializedPropertyAccessException
/**
 * Color scheme object for PIN screen components.
 * Provides theme-aware colors that adapt to the current Material theme.
 * 
 * Works by accessing MaterialTheme color scheme and app-specific color tokens to provide
 * consistent colors for background, layers, accents, and text throughout the PIN screen.
 */
object PinColors {
    val Bg: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val Layer: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val Neon: Color
        @Composable get() = AppColors.iconAccent()
    val Outline: Color
        @Composable get() = AppColors.iconAccent()
    val TextPri: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
}

/**
 * Shape definitions object for PIN screen components.
 * Provides consistent shape styling for PIN screen UI elements.
 * 
 * Works by accessing app-specific shape tokens to provide consistent rounded corners
 * and styling for PIN screen components.
 */
private object PinShapes {
    val Capsule
        @Composable get() = AppShapes.panelLarge()
}

/**
 * Composable for a round keypad button in the PIN screen.
 * Displays a number, backspace icon, or empty circle based on the provided parameters.
 * 
 * Works by rendering a circular button with border, showing either a text label, backspace icon,
 * or remaining empty, and handling click events through the onClick callback.
 * 
 * arguments:
 *     label - String?: Optional text label to display on the button, usually a digit from 0-9
 *     isBackspace - Boolean: If true, displays backspace icon instead of label, false to show label or empty
 *     onClick - () -> Unit: Callback function invoked when the button is clicked
 * 
 * return:
 *     Unit - No return value
 */
@Composable
private fun RoundKey(
    label: String? = null,
    isBackspace: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        Modifier
            .size(AppDimens.Pin.keySize)
            .clip(CircleShape)
            .border(AppBorderWidths.hero, PinColors.Neon, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            isBackspace -> Icon(
                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = null,
                tint = PinColors.Neon,
                modifier = Modifier.size(AppDimens.Pin.keyIconSize)
            )
            !label.isNullOrEmpty() -> Text(
                text = label,
                color = PinColors.Neon,
                fontSize = AppFontSizes.Pin.keypadTitle,
                fontWeight = FontWeight.Medium
            )
            else -> {}
        }
    }
}
/**
 * Composable for displaying the application logo and name in the PIN screen.
 * Shows the app logo icon and "DocManager" text in a centered column layout.
 * 
 * Works by rendering an icon from resources and text below it, both styled with the PIN screen accent color.
 * 
 * return:
 *     Unit - No return value
 */
@Composable
private fun LogoBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            tint = PinColors.Neon,
            modifier = Modifier.size(AppDimens.Pin.avatarSize)
        )
        Spacer(Modifier.height(AppDimens.spaceSm))
        Text(
            text = "DocManager",
            color = PinColors.Neon,
            fontSize = AppFontSizes.Pin.keypadSubtitle,
            fontWeight = FontWeight.SemiBold
        )
    }
}
/**
 * Composable for displaying the PIN input field with visibility toggle.
 * Shows the entered PIN either as visible digits or masked with asterisks, with a toggle button.
 * 
 * Works by displaying PIN digits with spaces between them when visible, or asterisks when hidden.
 * Provides a toggle button to switch between visible and hidden states.
 * 
 * arguments:
 *     isVisible - Boolean: Whether to show the actual PIN digits or mask them with asterisks
 *     actualPin - String: The current PIN value being entered, empty string if no digits entered
 *     onVisibilityToggle - () -> Unit: Callback function invoked when visibility toggle button is clicked
 * 
 * return:
 *     Unit - No return value
 */
@Composable
private fun PinCapsule(isVisible: Boolean = false, actualPin: String = "", onVisibilityToggle: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(AppDimens.Pin.pinButtonHeight)
            .clip(PinShapes.Capsule)
            .background(PinColors.Layer)
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.VpnKey,
            contentDescription = null,
            tint = PinColors.Neon,
            modifier = Modifier.size(AppDimens.Pin.keyIconSize)
        )
        Spacer(Modifier.width(AppDimens.spaceLg))
        Text(
            text = if (isVisible) {
                when {
                    actualPin.isEmpty() -> ""
                    actualPin.length == 1 -> actualPin
                    actualPin.length == 2 -> "${actualPin[0]} ${actualPin[1]}"
                    actualPin.length == 3 -> "${actualPin[0]} ${actualPin[1]} ${actualPin[2]}"
                    actualPin.length == 4 -> "${actualPin[0]} ${actualPin[1]} ${actualPin[2]} ${actualPin[3]}"
                    else -> actualPin
                }
            } else {
                when {
                    actualPin.isEmpty() -> ""
                    actualPin.length == 1 -> "*"
                    actualPin.length == 2 -> "**"
                    actualPin.length == 3 -> "***"
                    actualPin.length == 4 -> "****"
                    else -> "****"
                }
            },
            color = PinColors.Neon,
            fontSize = AppFontSizes.Pin.keypadSubtitle,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (isVisible) "Hide PIN" else "Show PIN",
            tint = PinColors.Neon,
            modifier = Modifier
                .size(AppDimens.Pin.keyIconSize)
                .clickable(onClick = onVisibilityToggle)
        )
    }
}
/**
 * Design showcase composable for the PIN screen.
 * Used for design system demonstration and preview purposes, not for actual PIN entry.
 * 
 * Works by rendering a static PIN screen layout with sample data (PIN "1234") to demonstrate
 * the visual design without requiring actual PIN functionality.
 * 
 * return:
 *     Unit - No return value
 */
@Composable
fun PinScreenDesign() {
    Surface(color = PinColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(AppDimens.spaceXl))
            LogoBlock()
            Spacer(Modifier.height(AppDimens.spaceXl))
            PinCapsule(isVisible = false, actualPin = "1234")
            Spacer(Modifier.height(AppDimens.spaceXl))
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
                    RoundKey("1"); RoundKey("2"); RoundKey("3")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
                    RoundKey("4"); RoundKey("5"); RoundKey("6")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
                    RoundKey("7"); RoundKey("8"); RoundKey("9")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
                    RoundKey()
                    RoundKey("0")
                    RoundKey(isBackspace = true)
                }
            }
        }
    }
}
/**
 * Main PIN screen composable that handles PIN entry for existing PINs and PIN creation for new users.
 * Manages PIN verification, ServiceLocator initialization, and navigation on success.
 * Blocks back navigation to prevent leaving the PIN screen.
 * 
 * Works by checking if PIN is set on initialization, then showing either PIN entry or PIN creation screen.
 * Automatically processes PIN when 4 digits are entered, verifying existing PINs or creating new ones.
 * Calls onSuccess callback after successful PIN verification or creation.
 * 
 * arguments:
 *     onSuccess - () -> Unit: Callback function invoked when PIN is successfully verified or created
 * 
 * return:
 *     Unit - No return value
 */
@Composable
fun PinScreenNew(onSuccess: () -> Unit) {
    var stage by remember { mutableStateOf(PinStageNew.Loading) }
    var pin by remember { mutableStateOf("") }
    var firstNew by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isPinVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = true) {
    }
    LaunchedEffect(Unit) {
        AppLogger.log("PinScreen", "PinScreen initialized")
        try {
            try {
                ServiceLocator.crypto.isPinSet()
            } catch (e: UninitializedPropertyAccessException) {
                AppLogger.log("PinScreen", "ERROR: ServiceLocator.crypto is not initialized")
                ErrorHandler.showCriticalError("Application initialization error")
                stage = PinStageNew.Loading
                return@LaunchedEffect
            }
            ErrorHandler.showInfo("Checking PIN status...")
            stage = if (ServiceLocator.crypto.isPinSet()) {
                AppLogger.log("PinScreen", "PIN is set, showing enter existing PIN screen")
                ErrorHandler.showInfo("PIN set, enter your current PIN")
                PinStageNew.EnterExisting
            } else {
                AppLogger.log("PinScreen", "PIN is not set, showing create new PIN screen")
                ErrorHandler.showInfo("PIN not set, create a new PIN")
                PinStageNew.EnterNew
            }
        } catch (e: Exception) {
            AppLogger.log("PinScreen", "ERROR: Failed to check PIN status: ${e.message}")
            ErrorHandler.showCriticalError("Application initialization error", e)
            stage = PinStageNew.Loading
        }
    }
    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.getBottom(density).div(density.density).dp
    Surface(color = PinColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(AppDimens.screenPadding)
                .padding(bottom = bottomInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(AppDimens.spaceXl))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    tint = PinColors.Neon,
                    modifier = Modifier.size(AppDimens.Pin.avatarSize)
                )
                Spacer(Modifier.height(AppDimens.spaceSm))
                Text(
                    text = when (stage) {
                        PinStageNew.EnterExisting -> "ENTER PIN"
                        PinStageNew.EnterNew -> "NEW PIN"
                        PinStageNew.ConfirmNew -> "CONFIRM PIN"
                        PinStageNew.Loading -> "DocManager"
                    },
                    color = PinColors.Neon,
                    fontSize = AppFontSizes.Pin.keypadSubtitle,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(AppDimens.spaceXl))
            PinCapsule(
                isVisible = isPinVisible,
                actualPin = pin,
                onVisibilityToggle = { isPinVisible = !isPinVisible }
            )
            error?.let { errorText ->
                Spacer(Modifier.height(AppDimens.spaceLg))
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = AppFontSizes.Pin.keypadLabel,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(AppDimens.spaceHuge))
            Spacer(Modifier.weight(1f))
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
                    RoundKey()
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
        Spacer(Modifier.height(AppDimens.spaceXl + bottomInset))
        }
    }
    LaunchedEffect(pin) {
        if (pin.length == 4 && !isProcessing) {
            isProcessing = true
            scope.launch {
                try {
                    when (stage) {
                        PinStageNew.EnterExisting -> {
                            AppLogger.log("PinScreen", "Verifying existing PIN...")
                            ErrorHandler.showInfo("Verifying current PIN...")
                            ServiceLocator.initializeWithPin(pin, isNewPin = false)
                            AppLogger.log("PinScreen", "PIN verified and database initialized")
                            ErrorHandler.showSuccess("PIN verified!")
                            onSuccess()
                        }
                        PinStageNew.EnterNew -> {
                            AppLogger.log("PinScreen", "New PIN entered, moving to confirmation")
                            ErrorHandler.showInfo("PIN entered, proceed to confirmation")
                            firstNew = pin
                            pin = ""
                            stage = PinStageNew.ConfirmNew
                        }
                        PinStageNew.ConfirmNew -> {
                            AppLogger.log("PinScreen", "Confirming new PIN...")
                            ErrorHandler.showInfo("Confirming new PIN...")
                            if (pin == firstNew) {
                                AppLogger.log("PinScreen", "New PIN confirmed, initializing database...")
                                ErrorHandler.showInfo("PIN confirmed, initializing database...")
                                ServiceLocator.initializeWithPin(pin, isNewPin = true)
                                AppLogger.log("PinScreen", "New PIN set and database initialized")
                                ErrorHandler.showSuccess("New PIN set!")
                                onSuccess()
                            } else {
                                AppLogger.log("PinScreen", "ERROR: PIN confirmation failed")
                                ErrorHandler.showError("PINs do not match")
                                error = "PINs do not match"
                                pin = ""
                                firstNew = null
                                stage = PinStageNew.EnterNew
                            }
                        }
                        PinStageNew.Loading -> {
                        }
                    }
                } catch (e: SecurityException) {
                    AppLogger.log("PinScreen", "ERROR: Invalid PIN entered")
                    ErrorHandler.showError("Invalid PIN")
                    error = "Invalid PIN"
                    pin = ""
                } catch (e: Exception) {
                    AppLogger.log("PinScreen", "ERROR: Failed to process PIN: ${e.message}")
                    ErrorHandler.showError("PIN processing failed: ${e.message}")
                    error = "PIN processing failed"
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
/**
 * Enumeration of possible stages in the PIN screen flow.
 * Represents the current state of PIN entry or creation process.
 * 
 * Works by tracking which stage the user is in: loading (checking PIN status), entering existing PIN,
 * entering new PIN, or confirming new PIN.
 */
enum class PinStageNew { Loading, EnterExisting, EnterNew, ConfirmNew }
