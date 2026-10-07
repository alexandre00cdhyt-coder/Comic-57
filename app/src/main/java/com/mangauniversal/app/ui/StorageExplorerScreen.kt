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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { ReaderPreferences(context) }

    var lastComic: HistoryEntry? by remember { mutableStateOf(null) }
    var storages by remember { mutableStateOf<List<StorageItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val history = withContext(Dispatchers.IO) { prefs.getHistory() }
        lastComic = history.firstOrNull()
        storages = withContext(Dispatchers.IO) { detectStorages() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // ── RECIENTES ──
        item { SectionTitle("Recientes") }

        item {
            if (lastComic != null) {
                ExplorerCard(
                    icon = "🕐",
                    title = lastComic!!.title,
                    subtitle = "Continuar leyendo",
                    onClick = onOpenRecientes
                )
            } else {
                ExplorerCard(
                    icon = "🕐",
                    title = "Aún no has leído nada",
                    subtitle = "Toca para ver el historial",
                    onClick = onOpenHistory
                )
            }
        }

        // ── ALMACENAMIENTOS ──
        item {
            Spacer(Modifier.height(6.dp))
            SectionTitle("Almacenamiento")
        }

        items(storages) { storage ->
            ExplorerCard(
                icon = storage.icon,
                title = storage.name,
                subtitle = storage.path,
                onClick = { onOpenStorage(storage.path) }
            )
        }

        // ── FAVORITAS ──
        if (favoriteFolders.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                SectionTitle("Carpetas favoritas")
            }
            items(favoriteFolders) { path ->
                val name = File(path).name.ifEmpty { path }
                ExplorerCard(
                    icon = "⭐",
                    title = name,
                    subtitle = path,
                    onClick = { onOpenFavoriteFolder(path) }
                )
            }
        }

        // ── AÑADIR ──
        item {
            Spacer(Modifier.height(10.dp))
            ExplorerCard(
                icon = "➕",
                title = "Añadir carpeta",
                subtitle = "Explora y marca carpetas como favoritas",
                onClick = onAddFolder
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun ExplorerCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.width(40.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun detectStorages(): List<StorageItem> {
    val result = mutableListOf<StorageItem>()

    val primary = Environment.getExternalStorageDirectory()
    if (primary != null && primary.exists()) {
        result.add(
            StorageItem(
                name = "Almacenamiento interno",
                path = primary.absolutePath,
                icon = "📱"
            )
        )
    }

    val storageDir = File("/storage")
    if (storageDir.exists() && storageDir.isDirectory) {
        storageDir.listFiles()?.forEach { file ->
            if (file.isDirectory &&
                file.canRead() &&
                file.name != "emulated" &&
                file.name != "self"
            ) {
                result.add(
                    StorageItem(
                        name = "SD Card (${file.name})",
                        path = file.absolutePath,
                        icon = "💾"
                    )
                )
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
