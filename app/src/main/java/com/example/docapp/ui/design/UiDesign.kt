package com.example.docapp.ui.design
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.docapp.ui.theme.AppBorderWidths
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppFontSizes
object DMColors {
    val Bg = Color(0xFF0C1115)
    val Surface = Color(0xFF171D24)
    val SurfaceSoft = Color(0xFF1E2630)
    val Accent = Color(0xFFC6FF00)
    val AccentDim = Color(0x99C6FF00)
    val TextPri = Color(0xFFEAF0F6)
    val TextSec = Color(0xFFB9C4D0)
    val Outline = Color(0xFF2B3340)
    val Danger = Color(0xFFE53935)
}
object DMShapes {
    val xl = RoundedCornerShape(DemoDim.cornerXl)
    val lg = RoundedCornerShape(DemoDim.cornerLg)
    val md = RoundedCornerShape(DemoDim.cornerMd)
    val pill = RoundedCornerShape(DemoDim.pillCorner)
}
object DMText {
    val H1 = Typography().headlineMedium.copy(color = DMColors.TextPri, fontWeight = FontWeight.SemiBold)
    val H2 = Typography().titleLarge.copy(color = DMColors.TextPri, fontWeight = FontWeight.SemiBold)
    val Title = Typography().titleMedium.copy(color = DMColors.TextPri, fontWeight = FontWeight.Medium)
    val Body = Typography().bodyMedium.copy(color = DMColors.TextSec)
    val Hint = Typography().bodySmall.copy(color = DMColors.TextSec)
}
private val DemoDim = AppDimens.DesignDemo
private val DemoBorder = AppBorderWidths
@Composable
fun NeonIconButton(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(DemoDim.iconPreviewSize)
            .clip(CircleShape)
            .border(DemoDim.iconBorderWidth, DMColors.Accent, CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}
@Composable
fun NeonPrimaryButton(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(DemoDim.fullButtonHeight)
            .clip(DMShapes.pill)
            .background(DMColors.Accent),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = AppFontSizes.DesignDemo.heroSubtitle)
    }
}
@Composable
fun NeonSecondaryButton(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(DemoDim.fullButtonHeight)
            .clip(DMShapes.pill)
            .border(DemoBorder.accent, DMColors.Accent, DMShapes.pill),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = DMColors.Accent, fontWeight = FontWeight.Medium, fontSize = AppFontSizes.DesignDemo.heroSubtitle)
    }
}
@Composable
fun SectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DMShapes.xl)
            .background(DMColors.Surface)
            .padding(DemoDim.showcasePadding)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ExpandLess, null, tint = DMColors.Accent)
                Spacer(Modifier.width(DemoDim.showcaseSpacingSmall))
                Text(title, style = DMText.H2)
            }
            if (trailing != null) Row { trailing() }
        }
        Spacer(Modifier.height(DemoDim.showcaseSpacingSmall))
        content()
    }
}
@Composable
fun DocRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DMShapes.lg)
            .background(DMColors.SurfaceSoft)
            .padding(horizontal = DemoDim.chipHorizontalPadding, vertical = DemoDim.chipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(DemoDim.badgeSizeMedium)
                .clip(CircleShape)
                .background(DMColors.Bg)
                .border(DemoBorder.thin, DMColors.Outline, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Description, null, tint = DMColors.Accent) }
        Spacer(Modifier.width(DemoDim.arrangeSpacingSmall))
        Column(Modifier.weight(1f)) {
            Text(title, style = DMText.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(DemoDim.detailSpacingXs))
            Text(subtitle, style = DMText.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(DemoDim.showcaseSpacingSmall))
        trailing()
    }
}
@Composable
fun PinScreenDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = DemoDim.heroPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(DemoDim.heroBlockSpacing))
            Icon(Icons.Outlined.Description, null, tint = DMColors.Accent, modifier = Modifier.size(DemoDim.heroIconSize))
            Spacer(Modifier.height(DemoDim.rowSpacingSmall))
            Text("DocManager", color = DMColors.Accent, fontSize = AppFontSizes.DesignDemo.brandTitle, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DemoDim.formSpacingLarge))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(DMShapes.xl)
                    .background(DMColors.Surface)
                    .padding(horizontal = DemoDim.keypadPaddingHorizontal, vertical = DemoDim.heroPaddingVertical),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.VpnKey, null, tint = DMColors.Accent)
                Spacer(Modifier.width(DemoDim.rowSpacingMedium))
                Text("•  •  •  •", style = DMText.H2, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent)
            }
            Spacer(Modifier.height(DemoDim.formSpacingHuge))
            val keys = listOf(
                "1","2","3",
                "4","5","6",
                "7","8","9",
                "","0","?"
            )
            Column(verticalArrangement = Arrangement.spacedBy(DemoDim.keypadSpacingLarge), horizontalAlignment = Alignment.CenterHorizontally) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(DemoDim.keypadRowSpacing)) {
                        row.forEach { label ->
                            Box(
                                Modifier
                                    .size(DemoDim.keypadCircleSize)
                                    .clip(CircleShape)
                                    .border(DemoBorder.hero, DMColors.Accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text(label, color = DMColors.Accent, fontSize = AppFontSizes.DesignDemo.heroBadgeLabel, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun HomeScreenDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        Box {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = DemoDim.listPaddingBottom),
                contentPadding = PaddingValues(DemoDim.showcasePadding),
                verticalArrangement = Arrangement.spacedBy(DemoDim.rowSpacingMedium)
            ) {
                item {
                    SectionCard(title = "My documents", trailing = {
                        NeonIconButton { Icon(Icons.Outlined.Tune, null, tint = DMColors.Accent) }
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(DemoDim.showcaseSpacingSmall)) {
                            DocRow("Passport details", "4006 45869721") {
                                NeonIconButton { Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent) }
                            }
                            DocRow("Password mail.ru", "******") {
                                NeonIconButton { Icon(Icons.Outlined.VisibilityOff, null, tint = DMColors.Accent) }
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = "My paswords", trailing = {
                        NeonIconButton { Icon(Icons.Outlined.Tune, null, tint = DMColors.Accent) }
                    }) {  }
                }
                item {
                    SectionCard(title = "Without a folder", trailing = {
                        NeonIconButton { Icon(Icons.Outlined.Tune, null, tint = DMColors.Accent) }
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(DemoDim.showcaseSpacingSmall)) {
                            DocRow("Saw size", "456x589") {
                                NeonIconButton { Icon(Icons.Outlined.VisibilityOff, null, tint = DMColors.Accent) }
                            }
                            DocRow("Router settings", "******") {
                                NeonIconButton { Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent) }
                            }
                            DocRow("Printer cartridge", "HP  650") {
                                NeonIconButton { Icon(Icons.Outlined.VisibilityOff, null, tint = DMColors.Accent) }
                            }
                        }
                    }
                }
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(DemoDim.cardPaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(DemoDim.arrangeSpacingLarge)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(DemoDim.bottomBarHeight)
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .border(DemoBorder.thin, DMColors.Outline, DMShapes.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, null, tint = DMColors.Accent)
                        Spacer(Modifier.width(DemoDim.showcaseSpacingSmall))
                        Icon(Icons.Outlined.AddCircleOutline, null, tint = DMColors.Accent)
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(DemoDim.bottomBarHeight)
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .border(DemoBorder.thin, DMColors.Outline, DMShapes.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Folder, null, tint = DMColors.Accent)
                        Spacer(Modifier.width(DemoDim.showcaseSpacingSmall))
                        Icon(Icons.Outlined.AddCircleOutline, null, tint = DMColors.Accent)
                    }
                }
            }
        }
    }
}
@Composable
private fun FieldTile(title: String, value: String, trailing: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DMShapes.lg)
            .background(DMColors.Surface)
            .padding(DemoDim.cardPadding)
    ) {
        Text(title, style = DMText.Hint)
        Spacer(Modifier.height(DemoDim.detailSpacingMd))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(DemoDim.badgeSizeSmall)
                        .clip(CircleShape)
                        .background(DMColors.Bg)
                        .border(DemoBorder.thin, DMColors.Outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Description, null, tint = DMColors.Accent) }
                Spacer(Modifier.width(DemoDim.showcaseSpacingSmall))
                Text(value, style = DMText.Title)
            }
            NeonIconButton { trailing() }
        }
    }
}
@Composable
private fun PdfChip(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(DemoDim.badgeSizeLarge)
                .clip(DMShapes.md)
                .border(DemoBorder.strong, DMColors.Danger, DMShapes.md)
                .background(DMColors.SurfaceSoft),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.PictureAsPdf, null, tint = DMColors.Danger, modifier = Modifier.size(DemoDim.chipIconSize)) }
        Spacer(Modifier.height(DemoDim.detailSpacingMd))
        Text(name, style = DMText.Body, color = DMColors.TextPri)
    }
}
@Composable
fun DocumentScreenDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(DemoDim.showcasePadding),
            verticalArrangement = Arrangement.spacedBy(DemoDim.showcaseSpacingMedium)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .padding(horizontal = DemoDim.showcasePadding, vertical = DemoDim.chipVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Edit, null, tint = DMColors.Accent)
                    Spacer(Modifier.width(DemoDim.showcaseSpacingSmall))
                    Text("Passport", style = DMText.H2, modifier = Modifier.weight(1f))
                    Text("Copy all", color = DMColors.Accent, fontWeight = FontWeight.Medium)
                }
            }
            item { FieldTile("Passport number", "4006 45869721") { Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent) } }
            item { FieldTile("Issued", "Main Directorate of the Ministry of Internal Affairs, Saint Petersburg region. Issued 24.03.2023") { Icon(Icons.Outlined.VisibilityOff, null, tint = DMColors.Accent) } }
            item { FieldTile("Department code", "780-020") { Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent) } }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .padding(DemoDim.arrangeSpacingSmall)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(DemoDim.heroPhotoHeight)
                            .clip(DMShapes.lg)
                            .background(DMColors.SurfaceSoft),
                        contentAlignment = Alignment.Center
                    ) { Text("Photo", color = DMColors.TextSec) }
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .padding(DemoDim.cardPadding)
                ) {
                    Text("Attached files", style = DMText.Title)
                    Spacer(Modifier.height(DemoDim.showcaseSpacingSmall))
                    Row(horizontalArrangement = Arrangement.spacedBy(DemoDim.showcaseSpacingMedium)) {
                        PdfChip("Polis.pdf")
                        PdfChip("Scan.pdf")
                    }
                }
            }
        }
    }
}
@Composable
private fun AttributeTile(label: String, value: String, removable: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DMShapes.lg)
            .background(DMColors.Surface)
            .padding(DemoDim.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(DemoDim.badgeSizeSmall)
                .clip(CircleShape)
                .background(DMColors.Bg)
                .border(DemoBorder.thin, DMColors.Outline, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Description, null, tint = DMColors.Accent) }
        Spacer(Modifier.width(DemoDim.arrangeSpacingSmall))
        Column(Modifier.weight(1f)) {
            Text(label, style = DMText.Hint)
            Spacer(Modifier.height(DemoDim.detailSpacingSm))
            Text(value, style = DMText.Title)
        }
        if (removable) NeonIconButton { Icon(Icons.Outlined.Cancel, null, tint = DMColors.Accent) }
    }
}
@Composable
fun NewDocumentScreenDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(DemoDim.showcasePadding)
                    .clip(DMShapes.xl)
                    .background(DMColors.Surface)
                    .padding(DemoDim.showcasePadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New document", style = DMText.H1, modifier = Modifier.weight(1f))
                NeonIconButton { Icon(Icons.Outlined.Close, null, tint = DMColors.Accent) }
            }
            LazyColumn(
                Modifier
                    .weight(1f)
                    .padding(horizontal = DemoDim.showcasePadding),
                verticalArrangement = Arrangement.spacedBy(DemoDim.arrangeSpacingSmall)
            ) {
                item { AttributeTile("Document name:", "Passport") }
                item { AttributeTile("Description:", "4067 457703") }
                item { AttributeTile("Passport issue date:", "20.12.2008", removable = true) }
                item { Spacer(Modifier.height(DemoDim.cardPaddingLarge)) }
            }
            Column(Modifier.padding(DemoDim.showcasePadding), verticalArrangement = Arrangement.spacedBy(DemoDim.arrangeSpacingSmall)) {
                NeonSecondaryButton("Add attribut")
                NeonSecondaryButton("Add file")
                NeonPrimaryButton("Save document")
            }
        }
    }
}
@Immutable
data class TemplateRow(val name: String)
@Composable
fun TemplatePickerDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .padding(DemoDim.showcasePadding)
                    .fillMaxWidth()
                    .clip(DMShapes.xl)
                    .background(DMColors.Surface)
                    .padding(DemoDim.showcaseSpacingSmall),
                horizontalArrangement = Arrangement.spacedBy(DemoDim.showcaseSpacingSmall)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(DemoDim.chipHeight)
                        .clip(DMShapes.pill)
                        .border(DemoBorder.accent, DMColors.Accent, DMShapes.pill),
                    contentAlignment = Alignment.Center
                ) { Text("Add file", color = DMColors.Accent, fontWeight = FontWeight.Medium) }
                Box(
                    Modifier
                        .weight(1f)
                        .height(DemoDim.chipHeight)
                        .clip(DMShapes.pill)
                        .background(DMColors.Accent),
                    contentAlignment = Alignment.Center
                ) { Text("Add template", color = Color.Black, fontWeight = FontWeight.SemiBold) }
            }
            val templates = listOf(TemplateRow("Passport"), TemplateRow("Passwords"))
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(DemoDim.showcasePadding),
                verticalArrangement = Arrangement.spacedBy(DemoDim.arrangeSpacingSmall)
            ) {
                items(templates) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(DMShapes.xl)
                            .background(DMColors.Surface)
                            .padding(horizontal = DemoDim.showcasePadding, vertical = DemoDim.heroPaddingVertical),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, style = DMText.H2, modifier = Modifier.weight(1f))
                        NeonIconButton { Icon(Icons.Outlined.Tune, null, tint = DMColors.Accent) }
                    }
                }
            }
        }
    }
}
