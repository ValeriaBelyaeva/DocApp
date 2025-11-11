package com.example.docapp.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.docapp.ui.theme.AppRadii

data class SurfaceShapes(
    val largeCard: Shape,
    val mediumCard: Shape,
    val smallCard: Shape,
    val icon: Shape,
    val button: Shape,
    val buttonSmall: Shape
)

data class SurfaceShapeRefs(
    val panelLarge: Shape,
    val panelMedium: Shape,
    val panelSmall: Shape,
    val listItem: Shape,
    val icon: Shape,
    val button: Shape,
    val buttonSmall: Shape,
    val chip: Shape,
    val badge: Shape
)

data class SurfaceStyleTokens(
    val shapes: SurfaceShapes,
    val materialShapes: Shapes,
    val useGradient: Boolean,
    val useInnerGlow: Boolean,
    val borderWidth: Dp,
    val refs: SurfaceShapeRefs
)

object SurfaceTokens {
    private val glassShapes = SurfaceShapes(
        largeCard = RoundedCornerShape(24.dp),
        mediumCard = RoundedCornerShape(20.dp),
        smallCard = RoundedCornerShape(16.dp),
        icon = CircleShape,
        button = RoundedCornerShape(16.dp),
        buttonSmall = RoundedCornerShape(12.dp)
    )
    
    private val glassMaterialShapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp)
    )

    private val matteShapes = SurfaceShapes(
        largeCard = RoundedCornerShape(48.dp),
        mediumCard = RoundedCornerShape(48.dp),
        smallCard = RoundedCornerShape(AppRadii.radiusMd),
        icon = RoundedCornerShape(AppRadii.radiusSm),
        button = RoundedCornerShape(AppRadii.radiusLg * 2),
        buttonSmall = RoundedCornerShape(AppRadii.radiusLg)
    )

    private val matteMaterialShapes = Shapes(
        small = RoundedCornerShape(AppRadii.radiusLg),
        medium = RoundedCornerShape(AppRadii.radiusLg * 2),
        large = RoundedCornerShape(AppRadii.radiusLg * 2)
    )

    private fun SurfaceShapes.toRefs(): SurfaceShapeRefs = SurfaceShapeRefs(
        panelLarge = largeCard,
        panelMedium = mediumCard,
        panelSmall = smallCard,
        listItem = mediumCard,
        icon = icon,
        button = button,
        buttonSmall = buttonSmall,
        chip = mediumCard,
        badge = CircleShape
    )

    val glass = SurfaceStyleTokens(
        shapes = glassShapes,
        materialShapes = glassMaterialShapes,
        useGradient = true,
        useInnerGlow = false,
        borderWidth = 1.5.dp,
        refs = glassShapes.toRefs()
    )

    val matte = SurfaceStyleTokens(
        shapes = matteShapes,
        materialShapes = matteMaterialShapes,
        useGradient = false,
        useInnerGlow = true,
        borderWidth = 0.dp,
        refs = matteShapes.toRefs()
    )

    fun current(style: SurfaceStyle): SurfaceStyleTokens = when (style) {
        SurfaceStyle.Glass -> glass
        SurfaceStyle.Matte -> matte
    }
}
