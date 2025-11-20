package com.example.docapp.ui.theme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
object AppDimens {
    val screenPadding: Dp = 12.dp
    val contentPaddingVertical: Dp = 16.dp
    val contentPaddingHorizontal: Dp = 16.dp
    val cardPadding: Dp = 12.dp
    val cardPaddingLarge: Dp = 24.dp
    val spaceXxs: Dp = 4.dp
    val spaceXs: Dp = 6.dp
    val spaceSm: Dp = 8.dp
    val spaceMd: Dp = 12.dp
    val spaceLg: Dp = 16.dp
    val spaceXl: Dp = 24.dp
    val space2Xl: Dp = 32.dp
    val spaceHuge: Dp = 80.dp
    val sectionSpacing: Dp = spaceMd
    val listSpacing: Dp = spaceSm
    val panelPaddingHorizontal: Dp = 16.dp
    val panelPaddingVertical: Dp = 12.dp
    val dialogPaddingHorizontal: Dp = 16.dp
    val dialogPaddingVertical: Dp = 12.dp
    val iconRowSpacing: Dp = spaceSm
    val dockSpacing: Dp = spaceXl
    val labelSpacing: Dp = spaceXs
    val listItemContentPadding: Dp = 12.dp
    val bottomButtonsSpacer: Dp = 128.dp
    val bottomButtonsHorizontalPadding: Dp = 24.dp
    val bottomButtonsVerticalPadding: Dp = 20.dp
    val bottomButtonsBetween: Dp = 12.dp
    val dockBottomPadding: Dp = 36.dp
    object Home {
        val folderToggleIconButton: Dp = 48.dp
        val folderToggleIcon: Dp = 32.dp
        val folderGlyphIcon: Dp = 32.dp
        val documentLeadingIcon: Dp = 24.dp
        val documentActionIconButton: Dp = 40.dp
        val dockButton: Dp = 56.dp
        val dockButtonIcon: Dp = 28.dp
        val selectionBorderWidth: Dp = 1.5.dp
    }
    object Document {
        val toolbarActionIcon: Dp = 40.dp
        val metadataIcon: Dp = 20.dp
        val fieldActionIcon: Dp = 36.dp
        val previewCardHeight: Dp = 220.dp
        val actionChip: Dp = 44.dp
        val actionChipTallHeight: Dp = 52.dp
        val actionChipIcon: Dp = 22.dp
        val editorToolbarHeight: Dp = 54.dp
        val editorBottomBarHeight: Dp = 58.dp
    }
    object Attachments {
        val listMaxHeight: Dp = 300.dp
    }
    object TemplateFill {
        val optionIcon: Dp = 40.dp
        val buttonHeight: Dp = 52.dp
    }
    object DesignDemo {
        val cornerXl: Dp = 28.dp
        val cornerLg: Dp = 22.dp
        val cornerMd: Dp = 16.dp
        val pillCorner: Dp = 50.dp
        val iconPreviewSize: Dp = 44.dp
        val iconBorderWidth: Dp = 1.2.dp
        val fullButtonHeight: Dp = 56.dp
        val chipHorizontalPadding: Dp = 14.dp
        val chipVerticalPadding: Dp = 12.dp
        val heroIconSize: Dp = 56.dp
        val heroBlockSpacing: Dp = 32.dp
        val heroPaddingHorizontal: Dp = 24.dp
        val heroPaddingVertical: Dp = 14.dp
        val heroCircleSize: Dp = 76.dp
        val heroCircleBorder: Dp = 1.5.dp
        val heroPhotoHeight: Dp = 230.dp
        val showcasePadding: Dp = 16.dp
        val showcaseSpacingLarge: Dp = 18.dp
        val showcaseSpacingMedium: Dp = 14.dp
        val showcaseSpacingSmall: Dp = 10.dp
        val detailSpacingXs: Dp = 2.dp
        val detailSpacingSm: Dp = 4.dp
        val detailSpacingMd: Dp = 6.dp
        val badgeSizeSmall: Dp = 34.dp
        val badgeSizeMedium: Dp = 42.dp
        val badgeSizeLarge: Dp = 72.dp
        val badgeBorderSmall: Dp = 1.dp
        val badgeBorderLarge: Dp = 2.dp
        val cardPadding: Dp = 14.dp
        val cardPaddingLarge: Dp = 24.dp
        val listPaddingBottom: Dp = 100.dp
        val arrangeSpacingLarge: Dp = 28.dp
        val arrangeSpacingMedium: Dp = 20.dp
        val arrangeSpacingSmall: Dp = 12.dp
        val formSpacingLarge: Dp = 26.dp
        val formSpacingHuge: Dp = 40.dp
        val rowSpacingLarge: Dp = 26.dp
        val rowSpacingMedium: Dp = 16.dp
        val rowSpacingSmall: Dp = 8.dp
        val chipHeight: Dp = 54.dp
        val chipBorder: Dp = 1.2.dp
        val chipIconSize: Dp = 36.dp
        val railPadding: Dp = 16.dp
        val railSpacing: Dp = 12.dp
        val keypadSpacingLarge: Dp = 18.dp
        val keypadCircleSize: Dp = 76.dp
        val keypadPaddingHorizontal: Dp = 20.dp
        val keypadRowSpacing: Dp = 26.dp
        val bottomBarHeight: Dp = 66.dp
    }
    object Pin {
        val keySize: Dp = 60.dp
        val keyIconSize: Dp = 20.dp
        val avatarSize: Dp = 56.dp
        val pinButtonSize: Dp = 56.dp
        val pinButtonHeight: Dp = 56.dp
        val legacyClearButtonWidth: Dp = 80.dp
    }
    object TemplateSelector {
        val tileIconSize: Dp = 44.dp
        val minBottomSheetHeight: Dp = 180.dp
    }
    object Widgets {
        val compactButtonHeight: Dp = 52.dp
    }
    object Glass {
        val shadowHigh: Dp = 20.dp
        val shadowLow: Dp = 6.dp
    }
    object Progress {
        val indicatorSmall: Dp = 16.dp
    }
    object Elevation {
        val tonalLow: Dp = 1.dp
    }
}
object AppBorderWidths {
    val thin: Dp = 1.dp
    val medium: Dp = 1.6.dp
    val accent: Dp = 1.2.dp
    val hero: Dp = 1.5.dp
    val strong: Dp = 2.dp
    val progress: Dp = 2.dp
}
object AppAlphas {
    object Home {
        const val selectedRowBackground: Float = 0.9f
        const val selectedBorderAccent: Float = 0.6f
    }
    object Document {
        const val infoBorder: Float = 0.3f
        const val fieldActionTint: Float = 0.8f
        const val outlineDisabledContent: Float = 0.4f
        const val primaryDisabledContainer: Float = 0.3f
        const val primaryDisabledContent: Float = 0.5f
    }
    object TemplateFill {
        const val inputOutline: Float = 0.2f
        const val optionOutline: Float = 0.16f
        const val primaryContent: Float = 0.95f
    }
}
object AppDurations {
    const val inactivityTimeoutMs: Long = 180_000L
    const val clipboardAutoClearMs: Long = 30_000L
    const val shortAnimMs: Int = 150
    const val mediumAnimMs: Int = 300
    const val longAnimMs: Int = 600
}
object AppFontSizes {
    object Pin {
        val keypadTitle = 24.sp
        val keypadSubtitle = 22.sp
        val keypadLabel = 16.sp
    }
    object DesignDemo {
        val heroTitle = 24.sp
        val heroSubtitle = 16.sp
        val badgeLabel = 12.sp
        val brandTitle = 22.sp
        val heroBadgeLabel = 28.sp
    }
}
object AppRadii {
    val radiusLg: Dp = 24.dp
    val radiusMd: Dp = 20.dp
    val radiusSm: Dp = 16.dp
    val radiusXs: Dp = 12.dp
    val radiusXl: Dp = 48.dp
    val radiusPill: Dp = 9999.dp
    val cardCorner: Dp = radiusMd
    val buttonCorner: Dp = radiusMd
    val smallButtonCorner: Dp = radiusSm
    val largeButtonCorner: Dp = radiusLg
}
object ThemePalette {
    private val lightBackground = Color(0xFFF5F0EB)
    private val lightSurface = Color(0xFFFFFFFF)
    private val lightSurfaceVariant = Color(0xFFF0E3D8)
    private val lightOutline = Color(0xFF7A4A33)
    private val lightOutlineSoft = Color(0xFFE0D2C6)
    private val textPrimaryLight = Color(0xFF2B2220)
    private val textSecondaryLight = Color(0xFF6F6058)
    private val textDisabledLight = Color(0xFFB5A7A0)
    private val accentPink = Color(0xFFD2667A)
    private val accentPinkDark = Color(0xFFB04E63)
    private val accentPinkSoft = Color(0xFFF7DFE5)
    private val accentBrown = Color(0xFF9C5A3C)
    private val accentBrownDark = Color(0xFF6D3C28)
    private val accentBrownSoft = Color(0xFFF2E1D7)
    private val successLight = Color(0xFF3B8F6A)
    private val warningLight = Color(0xFFD98A32)
    private val errorLight = Color(0xFFD64545)
    private val glassTintTop = Color(0xFFFFFFFF)
    private val glassTintBottom = lightSurfaceVariant
    private val glassHighlight = Color(0x66FFFFFF)
    private val glassShadow = Color(0x33000000)
    private val darkPrimary = Color(0xFFBCED57)
    private val darkOnPrimary = Color(0xFF10140A)
    private val darkPrimaryContainer = Color(0xFF1A2A11)
    private val darkOnPrimaryContainer = Color(0xFFBCED57)
    private val darkSecondary = Color(0xFFA4CF49)
    private val darkOnSecondary = Color(0xFF0E1406)
    private val darkSecondaryContainer = Color(0xFF233016)
    private val darkOnSecondaryContainer = Color(0xFFA4CF49)
    private val darkTertiary = Color(0xFFBCED57)
    private val darkOnTertiary = Color(0xFF10140A)
    private val darkTertiaryContainer = Color(0xFF233016)
    private val darkOnTertiaryContainer = Color(0xFFBCED57)
    private val darkBackground = Color(0xFF0F121A)
    private val darkOnBackground = Color(0xFFFBFBFB)
    private val darkSurface = Color(0xFF27292F)
    private val darkOnSurface = Color(0xFFFBFBFB)
    private val darkSurfaceVariant = Color(0xFF1C1F26)
    private val darkOnSurfaceVariant = Color(0xFF8B8C8E)
    private val darkOutline = Color(0xFF3A3D44)
    private val darkOutlineVariant = Color(0xFF2B2E34)
    private val darkGlassTintTop = Color(0x3327292F)
    private val darkGlassTintBottom = Color(0x33181B22)
    private val darkGlassHighlight = Color(0x268B8C8E)
    private val darkGlassShadow = Color(0x40000000)
    private val darkGlassBorderBright = Color(0x33BCED57)
    private val darkGlassBorderShadow = Color(0x33202127)
    val lightColorScheme: ColorScheme = lightColorScheme(
        primary = accentPink,
        onPrimary = Color.White,
        primaryContainer = accentPinkSoft,
        onPrimaryContainer = accentPinkDark,
        secondary = accentBrown,
        onSecondary = Color.White,
        secondaryContainer = accentBrownSoft,
        onSecondaryContainer = accentBrownDark,
        tertiary = accentPinkDark,
        onTertiary = Color.White,
        tertiaryContainer = accentPinkSoft,
        onTertiaryContainer = accentPinkDark,
        background = lightBackground,
        onBackground = textPrimaryLight,
        surface = lightSurface,
        onSurface = textPrimaryLight,
        surfaceVariant = lightSurfaceVariant,
        onSurfaceVariant = textSecondaryLight,
        outline = lightOutline,
        outlineVariant = lightOutlineSoft,
        error = errorLight,
        onError = Color.White,
        errorContainer = errorLight.copy(alpha = 0.12f),
        onErrorContainer = errorLight,
        inverseSurface = accentBrownDark,
        inverseOnSurface = lightSurface,
        inversePrimary = accentPinkDark,
        surfaceTint = accentPink,
        scrim = Color(0x88000000)
    )
    val darkColorScheme: ColorScheme = darkColorScheme(
        primary = darkPrimary,
        onPrimary = darkOnPrimary,
        primaryContainer = darkPrimaryContainer,
        onPrimaryContainer = darkOnPrimaryContainer,
        secondary = darkSecondary,
        onSecondary = darkOnSecondary,
        secondaryContainer = darkSecondaryContainer,
        onSecondaryContainer = darkOnSecondaryContainer,
        tertiary = darkSecondary,
        onTertiary = darkOnSecondary,
        tertiaryContainer = darkSecondaryContainer,
        onTertiaryContainer = darkOnSecondaryContainer,
        background = darkBackground,
        onBackground = darkOnBackground,
        surface = darkSurface,
        onSurface = darkOnSurface,
        surfaceVariant = darkSurfaceVariant,
        onSurfaceVariant = darkOnSurfaceVariant,
        outline = darkOutline,
        outlineVariant = darkOutlineVariant,
        error = errorLight,
        onError = Color.White,
        errorContainer = errorLight.copy(alpha = 0.2f),
        onErrorContainer = errorLight,
        inverseSurface = darkSurfaceVariant,
        inverseOnSurface = darkOnSurface,
        inversePrimary = darkPrimary,
        surfaceTint = darkPrimary,
        scrim = Color(0x88000000)
    )
    val lightGlassTokens = GlassColors(
        containerTop = glassTintTop,
        containerBottom = glassTintBottom.copy(alpha = 0.8f),
        highlight = glassHighlight,
        borderBright = accentPink.copy(alpha = 0.45f),
        borderShadow = lightOutlineSoft.copy(alpha = 0.6f),
        shadowColor = glassShadow
    )
    val darkGlassTokens = GlassColors(
        containerTop = darkGlassTintTop,
        containerBottom = darkGlassTintBottom.copy(alpha = 0.8f),
        highlight = darkGlassHighlight,
        borderBright = darkGlassBorderBright,
        borderShadow = darkGlassBorderShadow,
        shadowColor = darkGlassShadow
    )
    fun matteGlassTokens(colorScheme: ColorScheme): GlassColors = GlassColors(
        containerTop = colorScheme.surface,
        containerBottom = colorScheme.surface,
        highlight = Color.Transparent,
        borderBright = Color.Transparent,
        borderShadow = Color.Transparent,
        shadowColor = Color.Transparent
    )
}
object AppShapes {
    @Composable
    private fun tokens(): SurfaceShapeRefs = SurfaceTokens.current(ThemeConfig.surfaceStyle).refs
    @Composable
    fun panelLarge(): Shape = tokens().panelLarge
    @Composable
    fun panelMedium(): Shape = tokens().panelMedium
    @Composable
    fun panelSmall(): Shape = tokens().panelSmall
    @Composable
    fun listItem(): Shape = tokens().listItem
    @Composable
    fun iconButton(): Shape = tokens().icon
    @Composable
    fun primaryButton(): Shape = tokens().button
    @Composable
    fun secondaryButton(): Shape = tokens().buttonSmall
    @Composable
    fun chip(): Shape = tokens().chip
    @Composable
    fun badge(): Shape = tokens().badge
}
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
    @Composable
    fun iconAccent() = MaterialTheme.colorScheme.primary
    @Composable
    fun iconAccentBackground(): Color {
        val scheme = MaterialTheme.colorScheme
        val isDark = scheme.background.luminance() < 0.5f
        return if (isDark) {
            scheme.primaryContainer.copy(alpha = 0.85f)
        } else {
            scheme.secondaryContainer.copy(alpha = 0.55f)
        }
    }
    @Composable
    fun level2Background() = MaterialTheme.colorScheme.surfaceVariant
    @Composable
    fun level3Background(): Color {
        val scheme = MaterialTheme.colorScheme
        val isDark = scheme.background.luminance() < 0.5f
        return if (isDark) {
            scheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            scheme.secondaryContainer.copy(alpha = 0.35f)
        }
    }
}
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
object AppLayout {
    @Composable
    fun appScreenInsets(modifier: Modifier): Modifier = modifier.windowInsetsPadding(WindowInsets.systemBars)
    fun appScreenPadding(modifier: Modifier): Modifier = modifier.padding(AppDimens.screenPadding)
    fun appCardPadding(modifier: Modifier): Modifier = modifier.padding(AppDimens.cardPadding)
    @Composable
    fun buttonShape(): Shape = AppShapes.primaryButton()
    @Composable
    fun smallButtonShape(): Shape = AppShapes.secondaryButton()
    @Composable
    fun largeButtonShape(): Shape = AppShapes.panelLarge()
    @Composable
    fun extraLargeButtonShape(): Shape = AppShapes.panelLarge()
}
@Composable
fun VSpace(size: Dp) { Spacer(Modifier.size(height = size, width = 0.dp)) }
@Composable
fun HSpace(size: Dp) { Spacer(Modifier.size(width = size, height = 0.dp)) }
object DesignSystem {
    val dimens: AppDimensRef = AppDimensRef
    val layout: AppLayoutRef = AppLayoutRef
    val colors: AppColorsRef = AppColorsRef
    val typography = AppTypography
}
enum class SurfaceStyle { Glass, Matte }
object ThemeConfig {
    var surfaceStyle: SurfaceStyle by mutableStateOf(SurfaceStyle.Matte)
}
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
    @Composable
    fun iconAccent() = AppColors.iconAccent()
    @Composable
    fun iconAccentBackground() = AppColors.iconAccentBackground()
    @Composable
    fun level2Background() = AppColors.level2Background()
    @Composable
    fun level3Background() = AppColors.level3Background()
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
