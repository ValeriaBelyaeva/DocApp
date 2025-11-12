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
 * Compatibility test between the new design system and the legacy DocTheme.
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
                Text("Theme compatibility test", style = DMText.H1)
                
                // Compare colors
                SectionCard(title = "Color scheme comparison") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("New design system", style = DMText.Title)
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .background(DMColors.Bg)
                                        .border(1.dp, DMColors.Accent, RoundedCornerShape(8.dp))
                                )
                                Text("Bg", style = DMText.Body)
                            }
                            Column {
                                Text("Legacy theme", style = DMText.Title)
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
                
                // Components with legacy theme
                SectionCard(title = "Components with legacy theme") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Standard Material3 button
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Standard Material3 button")
                        }
                        
                        // Neon button
                        NeonPrimaryButton("Neon button")
                        
                        // Mixed usage
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
                
                // Typography comparison
                SectionCard(title = "Typography comparison") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Material3 Typography", style = MaterialTheme.typography.headlineMedium)
                        Text("Our typography", style = DMText.H1)
                        
                        Text("Material3 Body", style = MaterialTheme.typography.bodyMedium)
                        Text("Our Body", style = DMText.Body)
                    }
                }
            }
        }
    }
}
