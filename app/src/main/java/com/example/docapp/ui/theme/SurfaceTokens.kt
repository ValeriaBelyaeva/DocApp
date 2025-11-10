package com.example.docapp.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val borderWidth: Dp,
    val refs: SurfaceShapeRefs
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
        largeCard = RoundedCornerShape(AppRadii.radiusLg),
        mediumCard = RoundedCornerShape(AppRadii.radiusMd),
        smallCard = RoundedCornerShape(AppRadii.radiusSm),
        icon = RoundedCornerShape(AppRadii.radiusSm),
        button = RoundedCornerShape(AppRadii.radiusMd),
        buttonSmall = RoundedCornerShape(AppRadii.radiusSm)
    )

    private val matteMaterialShapes = Shapes(
        small = RoundedCornerShape(AppRadii.radiusSm),
        medium = RoundedCornerShape(AppRadii.radiusMd),
        large = RoundedCornerShape(AppRadii.radiusLg)
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
        borderWidth = 1.5.dp,
        refs = glassShapes.toRefs()
    )

    val matte = SurfaceStyleTokens(
        shapes = matteShapes,
        materialShapes = matteMaterialShapes,
        useGradient = false,
        borderWidth = 0.dp,
        refs = matteShapes.toRefs()
    )

    fun current(style: SurfaceStyle): SurfaceStyleTokens = when (style) {
        SurfaceStyle.Glass -> glass
        SurfaceStyle.Matte -> matte
    }
}
