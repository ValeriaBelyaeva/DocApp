package com.example.docapp.ui.design
import androidx.activity.compose.BackHandler
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
import com.example.docapp.ui.theme.AppBorderWidths
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppFontSizes
import com.example.docapp.ui.pin.PinScreenDesign
@Composable
fun DesignShowcase(navigator: com.example.docapp.ui.navigation.AppNavigator) {
    BackHandler(enabled = true) {
        navigator.safePopBack()
    }
    var currentScreen by remember { mutableStateOf(ShowcaseScreen.Pin) }
    Surface(color = Color(0xFF0B1014), modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(AppDimens.DesignDemo.showcasePadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingMedium)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingSmall)
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
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
                    .background(Color(0xFF161D24))
                    .padding(AppDimens.DesignDemo.showcaseSpacingSmall)
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
            .clip(RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
            .background(if (isSelected) Color(0xFFC6FF00) else Color(0xFF1E2630))
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.DesignDemo.chipHorizontalPadding, vertical = AppDimens.DesignDemo.chipVerticalPadding)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color(0xFFE9EFF6),
            fontSize = AppFontSizes.DesignDemo.badgeLabel,
            fontWeight = FontWeight.Medium
        )
    }
}
private enum class ShowcaseScreen {
    Pin, Home, Document, NewDocument, Template
}
