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
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppFontSizes

@Composable
fun PinDesignDemo() {
    Surface(color = PinColors.Bg, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(AppDimens.DesignDemo.showcasePadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PIN Screen Design Demo",
                color = PinColors.TextPri,
                fontSize = AppFontSizes.DesignDemo.heroTitle,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Exact layout replica",
                color = PinColors.Neon,
                fontSize = AppFontSizes.DesignDemo.heroSubtitle
            )
            
            Spacer(Modifier.height(AppDimens.DesignDemo.showcaseSpacingMedium))
            
            // Отображение PIN экрана
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
                    .background(PinColors.Layer)
                    .padding(AppDimens.DesignDemo.showcaseSpacingSmall)
            ) {
                PinScreenDesign()
            }
        }
    }
}
