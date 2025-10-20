package com.example.docapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
 

/**
 * Centralized design tokens: spacing, sizes, radii, durations, and semantic colors.
 */
object AppDimens {
    // Screen padding and common paddings
    val screenPadding: Dp = 16.dp
    val contentPaddingVertical: Dp = 16.dp
    val contentPaddingHorizontal: Dp = 16.dp
    val cardPadding: Dp = 12.dp

    // Spacing scale
    val spaceXxs: Dp = 4.dp
    val spaceXs: Dp = 6.dp
    val spaceSm: Dp = 8.dp
    val spaceMd: Dp = 12.dp
    val spaceLg: Dp = 16.dp
    val spaceXl: Dp = 24.dp
    val space2Xl: Dp = 32.dp

    // Specific layout constants
    val listItemContentPadding: Dp = 12.dp
    val bottomButtonsSpacer: Dp = 180.dp
    val bottomButtonsHorizontalPadding: Dp = 32.dp
    val bottomButtonsVerticalPadding: Dp = 24.dp
    val bottomButtonsBetween: Dp = 12.dp
}

object AppRadii {
    val cardCorner: Dp = 12.dp
    val buttonCorner: Dp = 32.dp
    val smallButtonCorner: Dp = 24.dp
    val largeButtonCorner: Dp = 40.dp
    val extraLargeButtonCorner: Dp = 48.dp
}

object AppDurations {
    const val shortAnimMs: Int = 150
    const val mediumAnimMs: Int = 300
    const val longAnimMs: Int = 600
}

/**
 * Semantic colors bound to current MaterialTheme color scheme.
 */
object AppColors {
    @Composable
    fun textPrimary() = MaterialTheme.colorScheme.onSurface

    @Composable
    fun iconPrimary() = MaterialTheme.colorScheme.onSurface

    @Composable
    fun primary() = MaterialTheme.colorScheme.primary

    @Composable
    fun textSecondary() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun background() = MaterialTheme.colorScheme.background

    @Composable
    fun surface() = MaterialTheme.colorScheme.surface

    @Composable
    fun glassContainerTop() = LocalGlassColors.current.containerTop

    @Composable
    fun glassContainerBottom() = LocalGlassColors.current.containerBottom

    @Composable
    fun glassHighlight() = LocalGlassColors.current.highlight

    @Composable
    fun glassBorderBright() = LocalGlassColors.current.borderBright

    @Composable
    fun glassBorderShadow() = LocalGlassColors.current.borderShadow

    @Composable
    fun glassShadow() = LocalGlassColors.current.shadowColor
}

// Kept legacy color constants for possible references
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Typography setup centralized here.
 */
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Layout helpers to minimize duplication and keep args centralized.
 */
object AppLayout {
    @Composable
    fun appScreenInsets(modifier: Modifier): Modifier = modifier.windowInsetsPadding(WindowInsets.systemBars)

    fun appScreenPadding(modifier: Modifier): Modifier = modifier.padding(AppDimens.screenPadding)

    fun appCardPadding(modifier: Modifier): Modifier = modifier.padding(AppDimens.cardPadding)
    
    fun buttonShape(): Shape = RoundedCornerShape(AppRadii.buttonCorner)
    fun smallButtonShape(): Shape = RoundedCornerShape(AppRadii.smallButtonCorner)
    fun largeButtonShape(): Shape = RoundedCornerShape(AppRadii.largeButtonCorner)
    fun extraLargeButtonShape(): Shape = RoundedCornerShape(AppRadii.extraLargeButtonCorner)
}

@Composable
fun VSpace(size: Dp) { Spacer(Modifier.size(height = size, width = 0.dp)) }

@Composable
fun HSpace(size: Dp) { Spacer(Modifier.size(width = size, height = 0.dp)) }

/**
 * OOP facade for design system to access tokens via a single object.
 */
object DesignSystem {
    val dimens: AppDimensRef = AppDimensRef
    val layout: AppLayoutRef = AppLayoutRef
    val colors: AppColorsRef = AppColorsRef
    val typography = AppTypography
}

// Thin wrappers to keep API stable while using OOP accessor
object AppDimensRef {
    val screenPadding get() = AppDimens.screenPadding
    val contentPaddingVertical get() = AppDimens.contentPaddingVertical
    val contentPaddingHorizontal get() = AppDimens.contentPaddingHorizontal
    val cardPadding get() = AppDimens.cardPadding
    val spaceXxs get() = AppDimens.spaceXxs
    val spaceXs get() = AppDimens.spaceXs
    val spaceSm get() = AppDimens.spaceSm
    val spaceMd get() = AppDimens.spaceMd
    val spaceLg get() = AppDimens.spaceLg
    val spaceXl get() = AppDimens.spaceXl
    val space2Xl get() = AppDimens.space2Xl
    val listItemContentPadding get() = AppDimens.listItemContentPadding
    val bottomButtonsSpacer get() = AppDimens.bottomButtonsSpacer
    val bottomButtonsHorizontalPadding get() = AppDimens.bottomButtonsHorizontalPadding
    val bottomButtonsVerticalPadding get() = AppDimens.bottomButtonsVerticalPadding
    val bottomButtonsBetween get() = AppDimens.bottomButtonsBetween
}

object AppLayoutRef {
    @Composable
    fun appScreenInsets(modifier: Modifier) = AppLayout.appScreenInsets(modifier)
    fun appScreenPadding(modifier: Modifier) = AppLayout.appScreenPadding(modifier)
    fun appCardPadding(modifier: Modifier) = AppLayout.appCardPadding(modifier)
}

object AppColorsRef {
    @Composable
    fun textPrimary() = AppColors.textPrimary()
    @Composable
    fun iconPrimary() = AppColors.iconPrimary()
    @Composable
    fun primary() = AppColors.primary()
    @Composable
    fun textSecondary() = AppColors.textSecondary()
    @Composable
    fun background() = AppColors.background()
    @Composable
    fun surface() = AppColors.surface()
    @Composable
    fun glassContainerTop() = AppColors.glassContainerTop()
    @Composable
    fun glassContainerBottom() = AppColors.glassContainerBottom()
    @Composable
    fun glassHighlight() = AppColors.glassHighlight()
    @Composable
    fun glassBorderBright() = AppColors.glassBorderBright()
    @Composable
    fun glassBorderShadow() = AppColors.glassBorderShadow()
    @Composable
    fun glassShadow() = AppColors.glassShadow()
}

@Stable
val GlassShape: Shape = RoundedCornerShape(AppRadii.cardCorner)

@Composable
fun glassGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        AppColors.glassContainerTop(),
        AppColors.glassContainerBottom()
    ),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY
)

