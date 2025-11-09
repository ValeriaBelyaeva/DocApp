package com.example.docapp.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.docapp.ui.theme.DocTheme

/**
 * Тестовый экран для проверки интеграции новой дизайн-системы
 * с существующей темой приложения
 */
@Composable
fun DesignSystemIntegrationTest() {
    DocTheme {
        Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Интеграция дизайн-системы", style = DMText.H1)
                
                // Тест компонентов
                SectionCard(title = "Тест компонентов") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NeonPrimaryButton("Основная кнопка")
                        NeonSecondaryButton("Вторичная кнопка")
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NeonIconButton { 
                                Icon(Icons.Outlined.Star, null, tint = DMColors.Accent) 
                            }
                            NeonIconButton { 
                                Icon(Icons.Outlined.Favorite, null, tint = DMColors.Accent) 
                            }
                        }
                        
                        DocRow("Тестовый документ", "123456789") {
                            NeonIconButton { 
                                Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent) 
                            }
                        }
                    }
                }
                
                // Тест цветов
                SectionCard(title = "Тест цветов") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(DMColors.Bg)
                                    .border(1.dp, DMColors.Accent, RoundedCornerShape(8.dp))
                            )
                            Text("Bg", style = DMText.Body)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(DMColors.Surface)
                                    .border(1.dp, DMColors.Accent, RoundedCornerShape(8.dp))
                            )
                            Text("Surface", style = DMText.Body)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(DMColors.Accent)
                            )
                            Text("Accent", style = DMText.Body, color = Color.Black)
                        }
                    }
                }
                
                // Тест типографики
                SectionCard(title = "Тест типографики") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Заголовок H1", style = DMText.H1)
                        Text("Заголовок H2", style = DMText.H2)
                        Text("Заголовок Title", style = DMText.Title)
                        Text("Основной текст Body", style = DMText.Body)
                        Text("Подсказка Hint", style = DMText.Hint)
                    }
                }
            }
        }
    }
}
