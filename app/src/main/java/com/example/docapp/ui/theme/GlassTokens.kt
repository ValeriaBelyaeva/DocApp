package com.example.docapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GlassColors(
    val containerTop: Color,
    val containerBottom: Color,
    val highlight: Color,
    val borderBright: Color,
    val borderShadow: Color,
    val shadowColor: Color
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        containerTop = Color.Transparent,
        containerBottom = Color.Transparent,
        highlight = Color.Transparent,
        borderBright = Color.Transparent,
        borderShadow = Color.Transparent,
        shadowColor = Color.Transparent
    )
}
