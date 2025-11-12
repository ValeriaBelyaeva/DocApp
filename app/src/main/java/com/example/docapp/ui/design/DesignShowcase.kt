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
import com.example.docapp.ui.pin.PinScreenDesign

@Composable
fun DesignShowcase() {
    var currentScreen by remember { mutableStateOf(ShowcaseScreen.Pin) }
    
    Surface(color = Color(0xFF0B1014), modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Simple navigation without extra headers
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShowcaseButton("PIN", currentScreen == ShowcaseScreen.Pin) { 
                    currentScreen = ShowcaseScreen.Pin 
                }
                ShowcaseButton("Home", currentScreen == ShowcaseScreen.Home) { 
                    currentScreen = ShowcaseScreen.Home 
                }
                ShowcaseButton("Document", currentScreen == ShowcaseScreen.Document) { 
                    currentScreen = ShowcaseScreen.Document 
                }
                ShowcaseButton("New Doc", currentScreen == ShowcaseScreen.NewDocument) { 
                    currentScreen = ShowcaseScreen.NewDocument 
                }
                ShowcaseButton("Template", currentScreen == ShowcaseScreen.Template) { 
                    currentScreen = ShowcaseScreen.Template 
                }
            }
            
            // Render the selected screen
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161D24))
                    .padding(8.dp)
            ) {
                when (currentScreen) {
                    ShowcaseScreen.Pin -> PinScreenDesign()
                    ShowcaseScreen.Home -> Text("Home Screen — in progress", color = Color(0xFFE9EFF6))
                    ShowcaseScreen.Document -> Text("Document Screen — in progress", color = Color(0xFFE9EFF6))
                    ShowcaseScreen.NewDocument -> Text("New Document Screen — in progress", color = Color(0xFFE9EFF6))
                    ShowcaseScreen.Template -> Text("Template Screen — in progress", color = Color(0xFFE9EFF6))
                }
            }
        }
    }
}

@Composable
private fun ShowcaseButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(100))
            .background(if (isSelected) Color(0xFFC6FF00) else Color(0xFF1E2630))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color(0xFFE9EFF6),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private enum class ShowcaseScreen {
    Pin, Home, Document, NewDocument, Template
}
