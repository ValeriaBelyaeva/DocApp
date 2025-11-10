package com.example.docapp.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

data class NeonColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val outline: Color,
    val badgePrimary: Color,
    val badgeSecondary: Color,
    val control: Color,
    val dock: Color,
    val dockButton: Color,
    val iconBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentWarning: Color
)

data class NeonShapes(
    val largeCard: Shape,
    val mediumCard: Shape,
    val smallCard: Shape,
    val icon: Shape,
    val button: Shape,
    val buttonSmall: Shape
)

object NeonTokens {
    val darkColors = NeonColorScheme(
        background = Color(0xFF060B12),
        surface = Color(0xFF1B2633),
        surfaceMuted = Color(0xFF212E3D),
        outline = Color(0xFF2F3A49),
        badgePrimary = Color(0xFF0F1926),
        badgeSecondary = Color(0xFF231428),
        control = Color(0xFF121C27),
        dock = Color(0xFF070D16),
        dockButton = Color(0xFF152130),
        iconBackground = Color(0xFF0C141D),
        textPrimary = Color(0xFFE8EEF6),
        textSecondary = Color(0xFF93A4B8),
        accent = Color(0xFFC6FF00),
        accentWarning = Color(0xFFFF4D67)
    )

    val shapes = NeonShapes(
        largeCard = RoundedCornerShape(28.dp),
        mediumCard = RoundedCornerShape(24.dp),
        smallCard = RoundedCornerShape(20.dp),
        icon = CircleShape,
        button = RoundedCornerShape(24.dp),
        buttonSmall = RoundedCornerShape(18.dp)
    )
}
