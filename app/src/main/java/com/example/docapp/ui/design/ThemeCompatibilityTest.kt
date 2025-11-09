package com.example.docapp.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.docapp.ui.theme.DocTheme

/**
 * Тест совместимости новой дизайн-системы с существующей темой DocTheme
 */
@Composable
fun ThemeCompatibilityTest() {
    DocTheme {
        Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Тест совместимости тем", style = DMText.H1)
                
                // Сравнение цветов
                SectionCard(title = "Сравнение цветовых схем") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Новая дизайн-система", style = DMText.Title)
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .background(DMColors.Bg)
                                        .border(1.dp, DMColors.Accent, RoundedCornerShape(8.dp))
                                )
                                Text("Bg", style = DMText.Body)
                            }
                            Column {
                                Text("Существующая тема", style = DMText.Title)
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .background(MaterialTheme.colorScheme.background)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                )
                                Text("Background", style = DMText.Body)
                            }
                        }
                        
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .background(DMColors.Surface)
                                        .border(1.dp, DMColors.Accent, RoundedCornerShape(8.dp))
                                )
                                Text("Surface", style = DMText.Body)
                            }
                            Column {
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                )
                                Text("Surface", style = DMText.Body)
                            }
                        }
                        
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .background(DMColors.Accent)
                                )
                                Text("Accent", style = DMText.Body, color = Color.Black)
                            }
                            Column {
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text("Primary", style = DMText.Body, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                
                // Тест компонентов с существующей темой
                SectionCard(title = "Компоненты с существующей темой") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Стандартная кнопка Material3
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Стандартная кнопка Material3")
                        }
                        
                        // Наша неоновая кнопка
                        NeonPrimaryButton("Неоновая кнопка")
                        
                        // Смешанное использование
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { }) {
                                Text("Outlined")
                            }
                            NeonSecondaryButton("Neon")
                        }
                    }
                }
                
                // Тест типографики
                SectionCard(title = "Сравнение типографики") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Material3 Typography", style = MaterialTheme.typography.headlineMedium)
                        Text("Наша типографика", style = DMText.H1)
                        
                        Text("Material3 Body", style = MaterialTheme.typography.bodyMedium)
                        Text("Наш Body", style = DMText.Body)
                    }
                }
            }
        }
    }
}
