package com.example.docapp.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = GlassShape,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current
    val isInteractive = onClick != null || onLongClick != null
    val interactionSource = remember { MutableInteractionSource() }
    val ripple = rememberRipple(
        bounded = true,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    )
    val clickableModifier = if (isInteractive) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = ripple,
            enabled = enabled,
            onClick = onClick ?: {},
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.shadow(
            elevation = 20.dp,
            shape = shape,
            clip = false,
            ambientColor = glassColors.shadowColor,
            spotColor = glassColors.shadowColor
        ),
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.linearGradient(
                colors = listOf(glassColors.borderBright, glassColors.borderShadow)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(glassGradient(), shape)
                .then(clickableModifier)
                .drawBehind {
                    val highlightBrush = Brush.linearGradient(
                        colors = listOf(glassColors.highlight, Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height / 1.4f)
                    )
                    drawRoundRect(
                        brush = highlightBrush,
                        cornerRadius = CornerRadius(AppRadii.cardCorner.toPx(), AppRadii.cardCorner.toPx())
                    )
                },
            content = content
        )
    }
}
