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
import com.example.docapp.ui.theme.AppBorderWidths
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.DocTheme
@Composable
fun DesignSystemIntegrationTest() {
    DocTheme {
        Surface(color = DMColors.Bg, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(AppDimens.DesignDemo.showcasePadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Design system integration", style = DMText.H1)
                SectionCard(title = "Component test") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.arrangeSpacingSmall)) {
                        NeonPrimaryButton("Primary button")
                        NeonSecondaryButton("Secondary button")
                        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingSmall)) {
                            NeonIconButton {
                                Icon(Icons.Outlined.Star, null, tint = DMColors.Accent)
                            }
                            NeonIconButton {
                                Icon(Icons.Outlined.Favorite, null, tint = DMColors.Accent)
                            }
                        }
                        DocRow("Sample document", "123456789") {
                            NeonIconButton {
                                Icon(Icons.Outlined.Visibility, null, tint = DMColors.Accent)
                            }
                        }
                    }
                }
                SectionCard(title = "Color test") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingSmall)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingSmall)) {
                            Box(
                                Modifier
                                    .size(AppDimens.DesignDemo.iconPreviewSize)
                                    .background(DMColors.Bg)
                                    .border(AppBorderWidths.thin, DMColors.Accent, RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
                            )
                            Text("Bg", style = DMText.Body)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingSmall)) {
                            Box(
                                Modifier
                                    .size(AppDimens.DesignDemo.iconPreviewSize)
                                    .background(DMColors.Surface)
                                    .border(AppBorderWidths.thin, DMColors.Accent, RoundedCornerShape(AppDimens.DesignDemo.cornerMd))
                            )
                            Text("Surface", style = DMText.Body)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingSmall)) {
                            Box(
                                Modifier
                                    .size(AppDimens.DesignDemo.iconPreviewSize)
                                    .background(DMColors.Accent)
                            )
                            Text("Accent", style = DMText.Body, color = Color.Black)
                        }
                    }
                }
                SectionCard(title = "Typography test") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.DesignDemo.showcaseSpacingSmall)) {
                        Text("Heading H1", style = DMText.H1)
                        Text("Heading H2", style = DMText.H2)
                        Text("Heading Title", style = DMText.Title)
                        Text("Body text", style = DMText.Body)
                        Text("Helper text", style = DMText.Hint)
                    }
                }
            }
        }
    }
}
