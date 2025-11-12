package com.example.docapp.ui.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.docapp.ui.theme.AppShapes
import com.example.docapp.ui.theme.AppDimens

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = AppShapes.primaryButton()
    ) {
        Text(text)
    }
}

/**
 * Field tile widget.
 * When showValue=true and value is provided — reveal the full value and hide the title,
 * otherwise show the title with a short preview snippet.
 */
@Composable
fun FieldTile(
    title: String,
    preview: String?,
    value: String? = null,
    showValue: Boolean = false,
    onCopy: (() -> Unit)? = null,
    onToggleSecret: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = AppShapes.listItem(),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                if (showValue && !value.isNullOrEmpty()) {
                    // Show the complete value without the title
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    // Default mode: title plus gray preview (first characters)
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (!preview.isNullOrBlank()) {
                        Text(
                            preview,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.iconRowSpacing)) {
                if (onCopy != null) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }
                if (onToggleSecret != null) {
                    IconButton(onClick = onToggleSecret) {
                        Icon(Icons.Default.RemoveRedEye, contentDescription = "Show or hide")
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

fun copyToClipboard(ctx: Context, label: String, text: String) {
    val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clip.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
    
    // Automatically clear clipboard after 30 seconds for safety
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val runnable = Runnable {
        try {
            clip.setPrimaryClip(ClipData.newPlainText("", ""))
        } catch (e: Exception) {
            // Ignore cleanup failures
        }
    }
    
    // Cancel previous cleanup tasks to avoid stacking them
    handler.removeCallbacks(runnable)
    handler.postDelayed(runnable, 30_000) // 30 seconds
}
