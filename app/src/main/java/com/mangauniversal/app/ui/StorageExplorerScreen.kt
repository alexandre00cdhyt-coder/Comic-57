package com.mangauniversal.app.ui

import android.os.Environment
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.settings.HistoryEntry
import com.mangauniversal.app.data.settings.ReaderPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StorageExplorerScreen(
    favoriteFolders: List<String>,
    onOpenRecientes: () -> Unit,
    onOpenStorage: (String) -> Unit,
    onOpenFavoriteFolder: (String) -> Unit,
    onAddFolder: () -> Unit,
    onOpenHistory: () -> Unit,
    onContinueReading: (Comic) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { ReaderPreferences(context) }

    var lastComic by remember { mutableStateOf<HistoryEntry?>(null) }
    var lastPage by remember { mutableStateOf(0) }
    var storages by remember { mutableStateOf<List<StorageItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val history = withContext(Dispatchers.IO) { prefs.getHistory() }
        val first = history.firstOrNull()
        lastComic = first
        if (first != null) {
            lastPage = withContext(Dispatchers.IO) { prefs.getProgress(first.comicId) }
        }
        storages = withContext(Dispatchers.IO) { detectStorages(context) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { Spacer(Modifier.height(6.dp)) }

        if (lastComic != null) {
            item {
                RecentCard(
                    entry = lastComic!!,
                    currentPage = lastPage,
                    onClick = {
                        val file = File(lastComic!!.path)
                        val comic = Comic(
                            id = lastComic!!.comicId,
                            title = lastComic!!.title,
                            path = lastComic!!.path,
                            type = detectType(file.name),
                            folder = file.parentFile?.name ?: "",
                            lastModified = file.lastModified(),
                            sizeBytes = file.length()
                        )
                        onContinueReading(comic)
                    },
                    onViewAll = onOpenHistory
                )
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            SectionTitle("Almacenamiento")
        }

        items(storages) { storage ->
            StorageRow(
                name = storage.name,
                path = storage.path,
                icon = storage.icon,
                onClick = { onOpenStorage(storage.path) }
            )
        }

        if (favoriteFolders.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                SectionTitle("Carpetas favoritas")
            }
            items(favoriteFolders) { path ->
                val name = File(path).name.ifEmpty { path }
                StorageRow(
                    name = name,
                    path = path,
                    icon = "📁",
                    onClick = { onOpenFavoriteFolder(path) }
                )
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            SectionTitle("Acciones")
        }

        item {
            StorageRow(
                name = "Añadir carpeta",
                path = "Explora y marca carpetas como favoritas",
                icon = "➕",
                onClick = onAddFolder
            )
        }

        item {
            StorageRow(
                name = "Historial completo",
                path = "Ver todos los cómics leídos",
                icon = "🕐",
                onClick = onOpenHistory
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RecentCard(
    entry: HistoryEntry,
    currentPage: Int,
    onClick: () -> Unit,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⏱", fontSize = 16.sp, color = Color(0xFFFF6B00))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Continuar leyendo",
                color = Color(0xFFFF6B00),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Ver todo",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconFor(detectType(entry.path)), fontSize = 24.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (currentPage > 0) "Página ${currentPage + 1}" else "Sin empezar",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A90E2))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Continuar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun StorageRow(
    name: String,
    path: String,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 22.sp, modifier = Modifier.width(32.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (path.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = path,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun detectStorages(context: android.content.Context): List<StorageItem> {
    val result = mutableListOf<StorageItem>()

    result.add(
        StorageItem(
            name = "Device",
            path = Environment.getExternalStorageDirectory().absolutePath,
            icon = "📱"
        )
    )

    val pictures = File(Environment.getExternalStorageDirectory(), "Pictures/Screenshots")
    if (pictures.exists()) {
        result.add(StorageItem(name = "Screenshots", path = pictures.absolutePath, icon = "📸"))
    }

    val comics = File(Environment.getExternalStorageDirectory(), "Comics")
    if (comics.exists()) {
        result.add(StorageItem(name = "Comics", path = comics.absolutePath, icon = "📚"))
    }

    val download = File(Environment.getExternalStorageDirectory(), "Download")
    if (download.exists()) {
        result.add(StorageItem(name = "Download", path = download.absolutePath, icon = "⬇️"))
    }

    val telegram = File(Environment.getExternalStorageDirectory(), "Android/media/org.telegram.messenger/Telegram")
    if (telegram.exists()) {
        result.add(StorageItem(name = "Telegram", path = telegram.absolutePath, icon = "💬"))
    }

    val storageDir = File("/storage")
    if (storageDir.exists() && storageDir.isDirectory) {
        storageDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.canRead() && file.name != "emulated" && file.name != "self") {
                if (result.none { it.path == file.absolutePath }) {
                    result.add(StorageItem(name = "SD Card (${file.name})", path = file.absolutePath, icon = "💾"))
                }
            }
        }
    }

    return result
}

data class StorageItem(
    val name: String,
    val path: String,
    val icon: String,
)

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
