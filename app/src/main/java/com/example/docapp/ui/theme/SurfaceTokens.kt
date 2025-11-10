package com.example.docapp.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SurfaceShapes(
    val largeCard: Shape,
    val mediumCard: Shape,
    val smallCard: Shape,
    val icon: Shape,
    val button: Shape,
    val buttonSmall: Shape
)

data class SurfaceStyleTokens(
    val shapes: SurfaceShapes,
    val materialShapes: Shapes,
    val useGradient: Boolean,
    val borderWidth: Dp
)

object SurfaceTokens {
    private val glassShapes = SurfaceShapes(
        largeCard = RoundedCornerShape(28.dp),
        mediumCard = RoundedCornerShape(24.dp),
        smallCard = RoundedCornerShape(20.dp),
        icon = CircleShape,
        button = RoundedCornerShape(24.dp),
        buttonSmall = RoundedCornerShape(18.dp)
    )

    private val glassMaterialShapes = Shapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(28.dp)
    )

    private val matteShapes = SurfaceShapes(
        largeCard = RoundedCornerShape(20.dp),
        mediumCard = RoundedCornerShape(16.dp),
        smallCard = RoundedCornerShape(12.dp),
        icon = RoundedCornerShape(12.dp),
        button = RoundedCornerShape(18.dp),
        buttonSmall = RoundedCornerShape(14.dp)
    )

    private val matteMaterialShapes = Shapes(
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp)
    )

    val glass = SurfaceStyleTokens(
        shapes = glassShapes,
        materialShapes = glassMaterialShapes,
        useGradient = true,
        borderWidth = 1.5.dp
    )

    val matte = SurfaceStyleTokens(
        shapes = matteShapes,
        materialShapes = matteMaterialShapes,
        useGradient = false,
        borderWidth = 0.dp
    )

    fun current(style: SurfaceStyle): SurfaceStyleTokens = when (style) {
        SurfaceStyle.Glass -> glass
        SurfaceStyle.Matte -> matte
    }
}
