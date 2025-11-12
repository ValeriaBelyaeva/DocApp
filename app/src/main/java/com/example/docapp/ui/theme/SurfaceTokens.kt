package com.example.docapp.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.docapp.ui.theme.AppRadii
import com.example.docapp.ui.theme.AppBorderWidths

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
        largeCard = RoundedCornerShape(AppRadii.radiusLg),
        mediumCard = RoundedCornerShape(AppRadii.radiusMd),
        smallCard = RoundedCornerShape(AppRadii.radiusSm),
        icon = CircleShape,
        button = RoundedCornerShape(AppRadii.radiusSm),
        buttonSmall = RoundedCornerShape(AppRadii.radiusXs)
    )
    
    private val glassMaterialShapes = Shapes(
        small = RoundedCornerShape(AppRadii.radiusXs),
        medium = RoundedCornerShape(AppRadii.radiusSm),
        large = RoundedCornerShape(AppRadii.radiusMd)
    )

    private val matteShapes = SurfaceShapes(
        largeCard = RoundedCornerShape(AppRadii.radiusXl),
        mediumCard = RoundedCornerShape(AppRadii.radiusXl),
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
        borderWidth = AppBorderWidths.hero,
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
