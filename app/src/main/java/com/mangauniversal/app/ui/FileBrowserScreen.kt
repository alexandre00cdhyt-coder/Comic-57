package com.mangauniversal.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.model.isComicFile
import java.io.File

@Composable
fun FileBrowserScreen(
    startPath: String,
    favoriteFolders: List<String>,
    onBack: () -> Unit,
    onOpenComic: (Comic) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var currentPath by remember(startPath) { mutableStateOf(startPath) }
    val currentDir = remember(currentPath) { File(currentPath) }

    val isFavorite = favoriteFolders.contains(currentPath)

    // Subcarpetas
    val subDirs = remember(currentPath) {
        try {
            currentDir.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") && it.canRead() }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Cómics en la carpeta actual
    val comics = remember(currentPath) {
        try {
            currentDir.listFiles()
                ?.filter { it.isFile && it.name.isComicFile() && it.canRead() }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ¿Podemos subir un nivel más arriba?
    val parent = currentDir.parentFile
    val canGoUp = parent != null && parent.absolutePath != "/" && parent.absolutePath.length >= "/storage".length

    // Gesto de atrás: sube un nivel o sale
    BackHandler(enabled = true) {
        if (canGoUp) {
            currentPath = parent!!.absolutePath
        } else {
            onBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable {
                        if (canGoUp) {
                            currentPath = parent!!.absolutePath
                        } else {
                            onBack()
                        }
                    }
                    .padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentDir.name.ifEmpty { currentPath },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentPath,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (isFavorite) "⭐" else "☆",
                fontSize = 22.sp,
                color = if (isFavorite) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable { onToggleFavorite(currentPath) }
                    .padding(start = 12.dp)
            )
        }

        // Contenido
        if (subDirs.isEmpty() && comics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Esta carpeta está vacía.\n\nMarca esta carpeta con ⭐ para añadirla como favorita.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Subcarpetas primero
                if (subDirs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Carpetas",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(subDirs, key = { it.absolutePath }) { dir ->
                        FolderCard(
                            dir = dir,
                            isFavorite = favoriteFolders.contains(dir.absolutePath),
                            onOpen = { currentPath = dir.absolutePath },
                            onToggleFav = { onToggleFavorite(dir.absolutePath) }
                        )
                    }
                }

                // Cómics después
                if (comics.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Cómics (${comics.size})",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(comics, key = { it.absolutePath }) { file ->
                        ComicCard(
                            file = file,
                            onClick = {
                                val type = detectType(file.name)
                                val comic = Comic(
                                    id = file.absolutePath,
                                    title = file.nameWithoutExtension,
                                    path = file.absolutePath,
                                    type = type,
                                    folder = file.parentFile?.name ?: "",
                                    lastModified = file.lastModified(),
                                    sizeBytes = file.length()
                                )
                                onOpenComic(comic)
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun FolderCard(
    dir: File,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit
) {
    val childCount = remember(dir.absolutePath) {
        try { dir.listFiles()?.size ?: 0 } catch (e: Exception) { 0 }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "📁", fontSize = 22.sp, modifier = Modifier.width(36.dp))
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dir.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (childCount > 0) {
                Text(
                    text = "$childCount elementos",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }
        Text(
            text = if (isFavorite) "⭐" else "☆",
            fontSize = 20.sp,
            color = if (isFavorite) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .clickable(onClick = onToggleFav)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = "›",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun ComicCard(
    file: File,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (file.extension.lowercase()) {
                "cbz", "cbr" -> "📕"
                "zip", "rar", "7z" -> "📦"
                "pdf" -> "📄"
                else -> "❓"
            },
            fontSize = 22.sp,
            modifier = Modifier.width(36.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.nameWithoutExtension,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${file.extension.uppercase()} · ${formatSize(file.length())}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp
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

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024L * 1024L -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024L * 1024L * 1024L)} GB"
    }
}
