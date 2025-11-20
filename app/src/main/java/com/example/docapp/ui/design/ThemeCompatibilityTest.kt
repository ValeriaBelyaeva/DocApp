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
import com.example.docapp.ui.theme.AppBorderWidths
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.DocTheme
@Composable
fun ThemeCompatibilityTest() {
    DocTheme {
        Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(AppDimens.DesignDemo.showcasePadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingMedium)
            ) {
                Text("Theme compatibility test", style = DMText.H1)
                SectionCard(title = "Color scheme comparison") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.arrangeSpacingSmall)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("New design system", style = DMText.Title)
                                Box(
                                    Modifier
                                        .size(AppDimens.DesignDemo.heroIconSize)
                                        .background(DMColors.Bg)
                                        .border(AppBorderWidths.thin, DMColors.Accent, RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
                                )
                                Text("Bg", style = DMText.Body)
                            }
                            Column {
                                Text("Legacy theme", style = DMText.Title)
                                Box(
                                    Modifier
                                        .size(AppDimens.DesignDemo.heroIconSize)
                                        .background(MaterialTheme.colorScheme.background)
                                        .border(AppBorderWidths.thin, MaterialTheme.colorScheme.primary, RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
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
                                        .size(AppDimens.DesignDemo.heroIconSize)
                                        .background(DMColors.Surface)
                                        .border(AppBorderWidths.thin, DMColors.Accent, RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
                                )
                                Text("Surface", style = DMText.Body)
                            }
                            Column {
                                Box(
                                    Modifier
                                        .size(AppDimens.DesignDemo.heroIconSize)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(AppBorderWidths.thin, MaterialTheme.colorScheme.primary, RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
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
                                        .size(AppDimens.DesignDemo.heroIconSize)
                                        .background(DMColors.Accent)
                                )
                                Text("Accent", style = DMText.Body, color = Color.Black)
                            }
                            Column {
                                Box(
                                    Modifier
                                        .size(AppDimens.DesignDemo.heroIconSize)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text("Primary", style = DMText.Body, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                SectionCard(title = "Components with legacy theme") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.arrangeSpacingSmall)) {
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Standard Material3 button")
                        }
                        NeonPrimaryButton("Neon button")
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
                SectionCard(title = "Typography comparison") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingSmall)) {
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
