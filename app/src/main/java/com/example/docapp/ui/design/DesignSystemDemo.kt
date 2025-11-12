package com.example.docapp.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DesignSystemDemo() {
    var currentScreen by remember { mutableStateOf(DemoScreen.Pin) }
    
    Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Navigation between showcase screens
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoButton("PIN", currentScreen == DemoScreen.Pin) { currentScreen = DemoScreen.Pin }
                DemoButton("Home", currentScreen == DemoScreen.Home) { currentScreen = DemoScreen.Home }
                DemoButton("Document", currentScreen == DemoScreen.Document) { currentScreen = DemoScreen.Document }
                DemoButton("New Doc", currentScreen == DemoScreen.NewDocument) { currentScreen = DemoScreen.NewDocument }
                DemoButton("Template", currentScreen == DemoScreen.Template) { currentScreen = DemoScreen.Template }
                DemoButton("Test", currentScreen == DemoScreen.Test) { currentScreen = DemoScreen.Test }
            }
            
            // Render the selected showcase content
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(DMShapes.lg)
                    .background(DMColors.Surface)
                    .padding(8.dp)
            ) {
                when (currentScreen) {
                    DemoScreen.Pin -> PinScreenDesign()
                    DemoScreen.Home -> HomeScreenDesign()
                    DemoScreen.Document -> DocumentScreenDesign()
                    DemoScreen.NewDocument -> NewDocumentScreenDesign()
                    DemoScreen.Template -> TemplatePickerDesign()
                    DemoScreen.Test -> DesignSystemIntegrationTest()
                }
            }
        }
    }
}

@Composable
private fun DemoButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(DMShapes.pill)
            .background(if (isSelected) DMColors.Accent else DMColors.SurfaceSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else DMColors.TextPri,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private enum class DemoScreen {
    Pin, Home, Document, NewDocument, Template, Test
}
