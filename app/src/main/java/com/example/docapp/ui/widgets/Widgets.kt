package com.example.docapp.ui.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(52.dp)) {
        Text(text)
    }
}

/**
 * Плитка поля.
 * Если showValue=true и передан value — показываем ПОЛНОЕ ЗНАЧЕНИЕ и скрываем title,
 * иначе показываем title и его краткий preview.
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
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                if (showValue && !value.isNullOrEmpty()) {
                    // Показать данные полностью, без названия
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    // Обычный режим: название + серый превью (первые символы)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onCopy != null) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Копировать")
                    }
                }
                if (onToggleSecret != null) {
                    IconButton(onClick = onToggleSecret) {
                        Icon(Icons.Default.RemoveRedEye, contentDescription = "Показать/скрыть")
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
    }
}

fun copyToClipboard(ctx: Context, label: String, text: String) {
    val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clip.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(ctx, "Скопировано", Toast.LENGTH_SHORT).show()
}
