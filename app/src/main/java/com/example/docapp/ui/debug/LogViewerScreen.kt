package com.example.docapp.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.docapp.core.AppLogger
import com.example.docapp.ui.theme.GlassCard
import java.io.File

@Composable
fun LogViewerScreen() {
    var logContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            val logFile = AppLogger.getLogFile()
            if (logFile?.exists() == true) {
                logContent = logFile.readText()
            } else {
                logContent = "Лог файл не найден"
            }
        } catch (e: Exception) {
            logContent = "Ошибка чтения логов: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Логи приложения",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            GlassCard(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = logContent,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}
