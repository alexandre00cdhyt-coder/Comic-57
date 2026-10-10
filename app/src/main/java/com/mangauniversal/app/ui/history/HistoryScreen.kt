package com.mangauniversal.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.settings.HistoryEntry
import com.mangauniversal.app.data.settings.ReaderPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenComic: (Comic, List<Comic>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ReaderPreferences(context) }

    var history by remember { mutableStateOf<List<HistoryEntry>?>(null) }
    var progressMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pageCountMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var entryToDelete by remember { mutableStateOf<HistoryEntry?>(null) }

    suspend fun loadHistory() {
        val h = withContext(Dispatchers.IO) { prefs.getHistory() }
        history = h

        val progress = withContext(Dispatchers.IO) { prefs.getAllProgress() }
        progressMap = progress

        val pages = mutableMapOf<String, Int>()
        for (entry in h) {
            val file = File(entry.path)
            val pageCount = withContext(Dispatchers.IO) {
                try {
                    when {
                        file.isDirectory -> file.listFiles()?.count { it.isFile } ?: 0
                        else -> 0
                    }
                } catch (e: Exception) { 0 }
            }
            pages[entry.comicId] = pageCount
        }
        pageCountMap = pages
    }

    LaunchedEffect(Unit) {
        loadHistory()
    }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Quitar del historial", color = Color.White, fontSize = 16.sp) },
            text = {
                Text(
                    "¿Quitar \"${entryToDelete!!.title}\" del historial?\n\n(El archivo NO se borra)",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = entryToDelete!!.comicId
                    entryToDelete = null
                    scope.launch {
                        prefs.removeFromHistory(id)
                        loadHistory()
                    }
                }) {
                    Text("Quitar", color = Color(0xFFFF5555), fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancelar", color = Color.White, fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp, start = 4.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📖 Historial",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cómics leídos recientemente",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            history == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando historial...", color = Color.White, fontSize = 13.sp)
            }

            history!!.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "📭 No hay cómics leídos todavía",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Los cómics que abras aparecerán aquí",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            else -> {
                Text(
                    text = "✅ ${history!!.size} cómics leídos",
                    color = Color(0xFFFF6B00),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history!!, key = { it.comicId }) { entry ->
                        val progress = progressMap[entry.comicId] ?: 0
                        val pageCount = pageCountMap[entry.comicId] ?: 0
                        HistoryRow(
                            entry = entry,
                            currentPage = progress,
                            totalPages = pageCount,
                            onClick = {
                                val file = File(entry.path)
                                val type = detectType(file.name)
                                val comic = Comic(
                                    id = entry.comicId,
                                    title = entry.title,
                                    path = entry.path,
                                    type = type,
                                    folder = file.parentFile?.name ?: "",
                                    lastModified = file.lastModified(),
                                    sizeBytes = file.length()
                                )
                                onOpenComic(comic, listOf(comic))
                            },
                            onMenu = {
                                entryToDelete = entry
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    currentPage: Int,
    totalPages: Int,
    onClick: () -> Unit,
    onMenu: () -> Unit
) {
    val percentage = if (totalPages > 0) {
        ((currentPage.toFloat() / totalPages) * 100).toInt().coerceIn(0, 100)
    } else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (detectType(entry.path)) {
                    ComicType.CBZ -> "📕"
                    ComicType.CBR -> "📕"
                    ComicType.ZIP -> "📦"
                    ComicType.RAR -> "📦"
                    ComicType.SEVEN_ZIP -> "📦"
                    ComicType.PDF -> "📄"
                    ComicType.FOLDER -> "📁"
                    else -> "❓"
                },
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatSize(File(entry.path).length()),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatRelativeTime(entry.timestamp),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }

            if (currentPage > 0 || totalPages > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (totalPages > 0) {
                        "$currentPage / $totalPages"
                    } else {
                        "Página $currentPage"
                    },
                    color = Color(0xFFFF6B00),
                    fontSize = 11.sp
                )
            }

            if (percentage > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2A2A2A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage / 100f)
                            .height(3.dp)
                            .background(Color(0xFFFF6B00))
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$percentage%",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = "⋮",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable(onClick = onMenu)
                .padding(start = 8.dp, end = 4.dp)
        )
    }
}

private fun detectType(fileName: String): ComicType {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "cbz" -> ComicType.CBZ
        "cbr" -> ComicType.CBR
        "zip" -> ComicType.ZIP
        "rar" -> ComicType.RAR
        "7z" -> ComicType.SEVEN_ZIP
        "pdf" -> ComicType.PDF
        else -> ComicType.UNKNOWN
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024L * 1024L -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024L * 1024L * 1024L)} GB"
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        minutes < 1 -> "Ahora mismo"
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> "Hace $hours h"
        days < 7 -> "Hace $days días"
        days < 30 -> "Hace ${days / 7} semanas"
        else -> "Hace ${days / 30} meses"
    }
}
