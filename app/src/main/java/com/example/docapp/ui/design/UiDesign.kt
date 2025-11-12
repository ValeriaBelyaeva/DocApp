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
import androidx.compose.ui.unit.sp

/* -----------------------------------------------------------
   ПАЛИТРА / ФОРМЫ / ТЕКСТ
----------------------------------------------------------- */

object DMColors {
    val Bg = Color(0xFF0C1115)           // общий фон
    val Surface = Color(0xFF171D24)      // крупные панели
    val SurfaceSoft = Color(0xFF1E2630)  // внутренние элементы
    val Accent = Color(0xFFC6FF00)       // лаймовый
    val AccentDim = Color(0x99C6FF00)
    val TextPri = Color(0xFFEAF0F6)
    val TextSec = Color(0xFFB9C4D0)
    val Outline = Color(0xFF2B3340)
    val Danger = Color(0xFFE53935)       // PDF
}

object DMShapes {
    val xl = RoundedCornerShape(28.dp)
    val lg = RoundedCornerShape(22.dp)
    val md = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(50)
}

object DMText {
    val H1 = Typography().headlineMedium.copy(color = DMColors.TextPri, fontWeight = FontWeight.SemiBold)
    val H2 = Typography().titleLarge.copy(color = DMColors.TextPri, fontWeight = FontWeight.SemiBold)
    val Title = Typography().titleMedium.copy(color = DMColors.TextPri, fontWeight = FontWeight.Medium)
    val Body = Typography().bodyMedium.copy(color = DMColors.TextSec)
    val Hint = Typography().bodySmall.copy(color = DMColors.TextSec)
}

/* -----------------------------------------------------------
   БАЗОВЫЕ ВИЗУАЛЬНЫЕ КОМПОНЕНТЫ
----------------------------------------------------------- */

@Composable
fun NeonIconButton(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.2.dp, DMColors.Accent, CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun NeonPrimaryButton(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(DMShapes.pill)
            .background(DMColors.Accent),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun NeonSecondaryButton(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(DMShapes.pill)
            .border(1.2.dp, DMColors.Accent, DMShapes.pill),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = DMColors.Accent, fontWeight = FontWeight.Medium, fontSize = 16.sp)
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
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ExpandLess, null, tint = DMColors.Accent)
                Spacer(Modifier.width(10.dp))
                Text(title, style = DMText.H2)
            }
            if (trailing != null) Row { trailing() }
        }
        Spacer(Modifier.height(10.dp))
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(DMColors.Bg)
                .border(1.dp, DMColors.Outline, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Description, null, tint = DMColors.Accent) }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(title, style = DMText.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = DMText.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.width(10.dp))
        trailing()
    }
}

/* -----------------------------------------------------------
   PIN SCREEN
----------------------------------------------------------- */

@Composable
fun PinScreenDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Outlined.Description, null, tint = DMColors.Accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(8.dp))
            Text("DocManager", color = DMColors.Accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(26.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(DMShapes.xl)
                    .background(DMColors.Surface)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.VpnKey, null, tint = DMColors.Accent)
                Spacer(Modifier.width(16.dp))
                Text("•  •  •  •", style = DMText.H2, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent)
            }

            Spacer(Modifier.height(40.dp))

            val keys = listOf(
                "1","2","3",
                "4","5","6",
                "7","8","9",
                "","0","⌫"
            )
            Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        row.forEach { label ->
                            Box(
                                Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, DMColors.Accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text(label, color = DMColors.Accent, fontSize = 28.sp, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
            }
        }
    }
}

/* -----------------------------------------------------------
   HOME SCREEN
----------------------------------------------------------- */

@Composable
fun HomeScreenDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        Box {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // My documents
                item {
                    SectionCard(title = "My documents", trailing = {
                        NeonIconButton { Icon(Icons.Outlined.Tune, null, tint = DMColors.Accent) }
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DocRow("Passport details", "4006 45869721") {
                                NeonIconButton { Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent) }
                            }
                            DocRow("Password mail.ru", "******") {
                                NeonIconButton { Icon(Icons.Outlined.VisibilityOff, null, tint = DMColors.Accent) }
                            }
                        }
                    }
                }
                // My paswords
                item {
                    SectionCard(title = "My paswords", trailing = {
                        NeonIconButton { Icon(Icons.Outlined.Tune, null, tint = DMColors.Accent) }
                    }) { /* пусто для вида */ }
                }
                // Without a folder
                item {
                    SectionCard(title = "Without a folder", trailing = {
                        NeonIconButton { Icon(Icons.Outlined.Tune, null, tint = DMColors.Accent) }
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

            // Нижняя панель
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(66.dp)
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .border(1.dp, DMColors.Outline, DMShapes.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, null, tint = DMColors.Accent)
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Outlined.AddCircleOutline, null, tint = DMColors.Accent)
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(66.dp)
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .border(1.dp, DMColors.Outline, DMShapes.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Folder, null, tint = DMColors.Accent)
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Outlined.AddCircleOutline, null, tint = DMColors.Accent)
                    }
                }
            }
        }
    }
}

/* -----------------------------------------------------------
   DOCUMENT SCREEN
----------------------------------------------------------- */

@Composable
private fun FieldTile(title: String, value: String, trailing: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DMShapes.lg)
            .background(DMColors.Surface)
            .padding(14.dp)
    ) {
        Text(title, style = DMText.Hint)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DMColors.Bg)
                        .border(1.dp, DMColors.Outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Description, null, tint = DMColors.Accent) }
                Spacer(Modifier.width(10.dp))
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
                .size(72.dp)
                .clip(DMShapes.md)
                .border(2.dp, DMColors.Danger, DMShapes.md)
                .background(DMColors.SurfaceSoft),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.PictureAsPdf, null, tint = DMColors.Danger, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(name, style = DMText.Body, color = DMColors.TextPri)
    }
}

@Composable
fun DocumentScreenDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Edit, null, tint = DMColors.Accent)
                    Spacer(Modifier.width(10.dp))
                    Text("Passport", style = DMText.H2, modifier = Modifier.weight(1f))
                    Text("Copy all", color = DMColors.Accent, fontWeight = FontWeight.Medium)
                }
            }

            item { FieldTile("Passport number", "4006 45869721") { Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent) } }
            item { FieldTile("Issued", "Main Directorate of the Ministry of Internal Affairs, Saint Petersburg region. Issued 24.03.2023") { Icon(Icons.Outlined.VisibilityOff, null, tint = DMColors.Accent) } }
            item { FieldTile("Department code", "780-020") { Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent) } }

            // Фото
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .padding(12.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(DMShapes.lg)
                            .background(DMColors.SurfaceSoft),
                        contentAlignment = Alignment.Center
                    ) { Text("Photo", color = DMColors.TextSec) }
                }
            }

            // Прикреплённые файлы
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(DMShapes.xl)
                        .background(DMColors.Surface)
                        .padding(14.dp)
                ) {
                    Text("Attached files", style = DMText.Title)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PdfChip("Polis.pdf")
                        PdfChip("Scan.pdf")
                    }
                }
            }
        }
    }
}

/* -----------------------------------------------------------
   NEW DOCUMENT SCREEN
----------------------------------------------------------- */

@Composable
private fun AttributeTile(label: String, value: String, removable: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DMShapes.lg)
            .background(DMColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DMColors.Bg)
                .border(1.dp, DMColors.Outline, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Description, null, tint = DMColors.Accent) }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(label, style = DMText.Hint)
            Spacer(Modifier.height(4.dp))
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
                    .padding(16.dp)
                    .clip(DMShapes.xl)
                    .background(DMColors.Surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New document", style = DMText.H1, modifier = Modifier.weight(1f))
                NeonIconButton { Icon(Icons.Outlined.Close, null, tint = DMColors.Accent) }
            }

            LazyColumn(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { AttributeTile("Document name:", "Passport") }
                item { AttributeTile("Description:", "4067 457703") }
                item { AttributeTile("Passport issue date:", "20.12.2008", removable = true) }
                item { Spacer(Modifier.height(24.dp)) }
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NeonSecondaryButton("Add attribut")
                NeonSecondaryButton("Add file")
                NeonPrimaryButton("Save document")
            }
        }
    }
}

/* -----------------------------------------------------------
   TEMPLATE PICKER SCREEN
----------------------------------------------------------- */

@Immutable
data class TemplateRow(val name: String)

@Composable
fun TemplatePickerDesign() {
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(DMShapes.xl)
                    .background(DMColors.Surface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(DMShapes.pill)
                        .border(1.2.dp, DMColors.Accent, DMShapes.pill),
                    contentAlignment = Alignment.Center
                ) { Text("Add file", color = DMColors.Accent, fontWeight = FontWeight.Medium) }

                Box(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(DMShapes.pill)
                        .background(DMColors.Accent),
                    contentAlignment = Alignment.Center
                ) { Text("Add template", color = Color.Black, fontWeight = FontWeight.SemiBold) }
            }

            val templates = listOf(TemplateRow("Passport"), TemplateRow("Passwords"))
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(DMShapes.xl)
                            .background(DMColors.Surface)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
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
