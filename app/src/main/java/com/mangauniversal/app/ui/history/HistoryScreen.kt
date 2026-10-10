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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.settings.HistoryEntry
import com.mangauniversal.app.data.settings.ReaderPreferences
import com.mangauniversal.app.data.thumbnail.ThumbnailExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenComic: (Comic, List<Comic>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F3A4D))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = "📖 Historial",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cómics leídos recientemente",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        HistoryScreenContent(onOpenComic = onOpenComic)
    }
}

@Composable
fun HistoryScreenContent(
    onOpenComic: (Comic, List<Comic>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ReaderPreferences(context) }
    val thumbnailExtractor = remember { ThumbnailExtractor(context) }

    var history by remember { mutableStateOf<List<HistoryEntry>?>(null) }
    var progressMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var entryToDelete by remember { mutableStateOf<HistoryEntry?>(null) }

    suspend fun loadHistory() {
        val h = withContext(Dispatchers.IO) { prefs.getHistory() }
        history = h
        progressMap = withContext(Dispatchers.IO) { prefs.getAllProgress() }
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Spacer(Modifier.height(6.dp)) }
                items(history!!, key = { it.comicId }) { entry ->
                    val progress = progressMap[entry.comicId] ?: 0
                    HistoryRowItem(
                        entry = entry,
                        currentPage = progress,
                        extractor = thumbnailExtractor,
                        onClick = {
                            val file = File(entry.path)
                            val comic = Comic(
                                id = entry.comicId,
                                title = entry.title,
                                path = entry.path,
                                type = detectType(file.name),
                                folder = file.parentFile?.name ?: "",
                                lastModified = file.lastModified(),
                                sizeBytes = file.length()
                            )
                            onOpenComic(comic, listOf(comic))
                        },
                        onMenu = { entryToDelete = entry }
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryRowItem(
    entry: HistoryEntry,
    currentPage: Int,
    extractor: ThumbnailExtractor,
    onClick: () -> Unit,
    onMenu: () -> Unit
) {
    var thumbPath by remember(entry.comicId) { mutableStateOf<String?>(null) }

    LaunchedEffect(entry.comicId) {
        val file = File(entry.path)
        val comic = Comic(
            id = entry.comicId,
            title = entry.title,
            path = entry.path,
            type = detectType(file.name),
            folder = file.parentFile?.name ?: "",
            lastModified = file.lastModified(),
            sizeBytes = file.length()
        )
        thumbPath = extractor.getThumbnail(comic)?.absolutePath
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp, 80.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbPath != null) {
                AsyncImage(
                    model = thumbPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = iconFor(detectType(File(entry.path).name)),
                    fontSize = 28.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatSize(File(entry.path).length()),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatRelativeTime(entry.timestamp),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (currentPage > 0) "Página ${currentPage + 1}" else "Sin empezar",
                color = Color(0xFFFF6B00),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        IconButton(onClick = onMenu) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Menú",
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
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

private fun iconFor(type: ComicType): String = when (type) {
    ComicType.CBZ -> "📕"
    ComicType.CBR -> "📕"
    ComicType.ZIP -> "📦"
    ComicType.RAR -> "📦"
    ComicType.SEVEN_ZIP -> "📦"
    ComicType.PDF -> "📄"
    ComicType.FOLDER -> "📁"
    else -> "❓"
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
