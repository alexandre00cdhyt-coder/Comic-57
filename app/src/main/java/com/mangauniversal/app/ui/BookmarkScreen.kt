package com.mangauniversal.app.ui

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.settings.Bookmark
import com.mangauniversal.app.data.settings.BookmarkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BookmarkScreen(
    onOpenBookmark: (Comic, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { BookmarkManager(context) }

    var bookmarks by remember { mutableStateOf<List<Bookmark>?>(null) }
    var bookmarkToDelete by remember { mutableStateOf<Bookmark?>(null) }

    suspend fun load() {
        bookmarks = withContext(Dispatchers.IO) { manager.getAll() }
    }

    LaunchedEffect(Unit) {
        load()
    }

    if (bookmarkToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookmarkToDelete = null },
            title = { Text("Eliminar marcador", color = Color.White, fontSize = 16.sp) },
            text = {
                Text(
                    "¿Eliminar el marcador de \"${bookmarkToDelete!!.comicTitle}\" (página ${bookmarkToDelete!!.page + 1})?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val b = bookmarkToDelete!!
                    bookmarkToDelete = null
                    scope.launch {
                        manager.remove(b.comicId, b.page)
                        load()
                    }
                }) {
                    Text("Eliminar", color = Color(0xFFFF5555), fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToDelete = null }) {
                    Text("Cancelar", color = Color.White, fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    when {
        bookmarks == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Cargando marcadores...", color = Color.White, fontSize = 13.sp)
        }

        bookmarks!!.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No hay marcadores todavía",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Abre un cómic y añade un marcador con 🔖",
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
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${bookmarks!!.size} marcadores",
                        color = Color(0xFFFF6B00),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }

                items(bookmarks!!, key = { "${it.comicId}_${it.page}" }) { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        onClick = {
                            val file = File(bookmark.comicPath)
                            val comic = Comic(
                                id = bookmark.comicId,
                                title = bookmark.comicTitle,
                                path = bookmark.comicPath,
                                type = detectType(file.name),
                                folder = file.parentFile?.name ?: "",
                                lastModified = file.lastModified(),
                                sizeBytes = file.length()
                            )
                            onOpenBookmark(comic, bookmark.page)
                        },
                        onDelete = { bookmarkToDelete = bookmark }
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Text("🔖", fontSize = 20.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.comicTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Página ${bookmark.page + 1}",
                color = Color(0xFFFF6B00),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (bookmark.note.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = bookmark.note,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatRelativeTime(bookmark.addedAt),
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 10.sp
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
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
