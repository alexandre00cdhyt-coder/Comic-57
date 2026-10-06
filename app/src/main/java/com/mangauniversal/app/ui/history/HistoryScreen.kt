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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mangauniversal.app.data.model.detectComicType
import com.mangauniversal.app.data.settings.HistoryEntry
import com.mangauniversal.app.data.settings.ReaderPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenComic: (Comic, List<Comic>) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { ReaderPreferences(context) }

    var history by remember { mutableStateOf<List<HistoryEntry>?>(null) }
    var progressMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val h = withContext(Dispatchers.IO) { prefs.getHistory() }
        history = h

        // Cargar progreso de cada cómic del historial
        val map = mutableMapOf<String, Int>()
        for (entry in h) {
            val p = withContext(Dispatchers.IO) { prefs.getProgress(entry.comicId) }
            map[entry.comicId] = p
        }
        progressMap = map
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
            .padding(12.dp)
    ) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EAA)),
                modifier = Modifier.width(60.dp)
            ) {
                Text("←", fontSize = 18.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📖 Historial",
                    color = Color.White,
                    fontSize = 16.sp,
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
                // Contador
                Text(
                    text = "✅ ${history!!.size} cómics leídos",
                    color = Color(0xFFFF6B00),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lista de cómics del historial
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(history!!, key = { it.comicId }) { entry ->
                        val currentProgress = progressMap[entry.comicId] ?: 0
                        val comic = remember(entry) {
                            entryToComic(entry)
                        }
                        HistoryRow(
                            entry = entry,
                            comic = comic,
                            currentPage = currentProgress,
                            onClick = {
                                // Al tocar, abrir el cómic
                                onOpenComic(comic, listOf(comic))
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
    comic: Comic,
    currentPage: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono según tipo
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (comic.type) {
                    com.mangauniversal.app.data.model.ComicType.CBZ -> "📕"
                    com.mangauniversal.app.data.model.ComicType.CBR -> "📕"
                    com.mangauniversal.app.data.model.ComicType.ZIP -> "📦"
                    com.mangauniversal.app.data.model.ComicType.RAR -> "📦"
                    com.mangauniversal.app.data.model.ComicType.SEVEN_ZIP -> "📦"
                    com.mangauniversal.app.data.model.ComicType.PDF -> "📄"
                    com.mangauniversal.app.data.model.ComicType.FOLDER -> "📁"
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
            Text(
                text = if (currentPage > 0) {
                    "Página ${currentPage + 1} · ${formatRelativeTime(entry.timestamp)}"
                } else {
                    formatRelativeTime(entry.timestamp)
                },
                color = Color(0xFFFF6B00),
                fontSize = 11.sp
            )
        }

        Text(
            text = "›",
            fontSize = 22.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

private fun entryToComic(entry: HistoryEntry): Comic {
    val path = entry.path
    val fileName = File(path).name
    val type = detectComicType(fileName)
    return Comic(
        id = entry.comicId,
        title = entry.title,
        path = path,
        type = type,
        folder = File(path).parentFile?.name ?: ""
    )
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
