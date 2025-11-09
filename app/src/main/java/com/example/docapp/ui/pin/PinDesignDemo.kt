package com.example.docapp.ui.pin

import androidx.compose.foundation.background
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
fun PinDesignDemo() {
    Surface(color = PinColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PIN Screen Design Demo",
                color = PinColors.TextPri,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Точный визуал по макету",
                color = PinColors.Neon,
                fontSize = 16.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Отображение PIN экрана
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PinColors.Layer)
                    .padding(8.dp)
            ) {
                PinScreenDesign()
            }
        }
    }
}
