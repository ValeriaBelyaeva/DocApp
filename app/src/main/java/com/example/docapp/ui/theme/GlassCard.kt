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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.docapp.ui.theme.AppShapes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current
    val surfaceTokens = SurfaceTokens.current(ThemeConfig.surfaceStyle)
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val fallbackGlow = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val targetShape = shape ?: AppShapes.panelLarge()
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

    val borderStroke = if (surfaceTokens.borderWidth > 0.dp &&
        (glassColors.borderBright.alpha > 0f || glassColors.borderShadow.alpha > 0f)
    ) {
        BorderStroke(
            width = surfaceTokens.borderWidth,
            brush = Brush.linearGradient(
                colors = listOf(glassColors.borderBright, glassColors.borderShadow)
            )
        )
    } else null

    val shadowElevation = if (glassColors.shadowColor.alpha > 0f) 20.dp else 6.dp

    Surface(
        modifier = modifier.shadow(
            elevation = shadowElevation,
            shape = targetShape,
            clip = false,
            ambientColor = glassColors.shadowColor,
            spotColor = glassColors.shadowColor
        ),
        shape = targetShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .clip(targetShape)
                .then(
                    if (surfaceTokens.useGradient) {
                        Modifier.background(glassGradient(), targetShape)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surface, targetShape)
                    }
                )
                .then(clickableModifier)
                .drawBehind {
                    val fallbackRadius = size.minDimension * 0.12f
                    val cornerRadius = when (val outline = targetShape.createOutline(size, layoutDirection, this)) {
                        is Outline.Rounded -> {
                            val radius = outline.roundRect.topLeftCornerRadius
                            CornerRadius(radius.x, radius.y)
                        }
                        else -> CornerRadius(fallbackRadius, fallbackRadius)
                    }

                    if (surfaceTokens.useGradient && glassColors.highlight.alpha > 0f) {
                        val highlightBrush = Brush.linearGradient(
                            colors = listOf(glassColors.highlight, Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height / 1.4f)
                        )
                        drawRoundRect(
                            brush = highlightBrush,
                            cornerRadius = cornerRadius
                        )
                    } else if (surfaceTokens.useInnerGlow) {
                        // Matte or fallback surfaces rely on a subtle neutral inner glow to avoid flat fills.
                        val glowCore = if (glassColors.highlight.alpha > 0f) glassColors.highlight else fallbackGlow
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(glowCore, Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension
                        )
                        drawRoundRect(brush = glowBrush, cornerRadius = cornerRadius)
                    }
                },
            content = content
        )
    }
}
