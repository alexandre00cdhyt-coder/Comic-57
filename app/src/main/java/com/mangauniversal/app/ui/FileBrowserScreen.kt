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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val isFavorite = favoriteFolders.contains(currentPath)

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

    val parent = currentDir.parentFile
    val canGoUp = parent != null &&
            parent.absolutePath != "/" &&
            parent.absolutePath.length >= "/storage".length

    BackHandler(enabled = true) {
        if (canGoUp) {
            currentPath = parent!!.absolutePath
        } else {
            onBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F3A4D))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (canGoUp) currentPath = parent!!.absolutePath else onBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
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
            IconButton(onClick = { onToggleFavorite(currentPath) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.7f)
                )
            }
        }

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
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Spacer(Modifier.height(6.dp)) }

                if (subDirs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Carpetas (${subDirs.size})",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(subDirs, key = { it.absolutePath }) { dir ->
                        BrowserRow(
                            icon = "📁",
                            title = dir.name,
                            subtitle = "${dir.listFiles()?.size ?: 0} elementos",
                            isFavorite = favoriteFolders.contains(dir.absolutePath),
                            onOpen = { currentPath = dir.absolutePath },
                            onToggleFav = { onToggleFavorite(dir.absolutePath) }
                        )
                    }
                }

                if (comics.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Cómics (${comics.size})",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(comics, key = { it.absolutePath }) { file ->
                        BrowserRow(
                            icon = iconFor(detectType(file.name)),
                            title = file.nameWithoutExtension,
                            subtitle = "${file.extension.uppercase()} · ${formatSize(file.length())}",
                            isFavorite = false,
                            onOpen = {
                                val comic = Comic(
                                    id = file.absolutePath,
                                    title = file.nameWithoutExtension,
                                    path = file.absolutePath,
                                    type = detectType(file.name),
                                    folder = file.parentFile?.name ?: "",
                                    lastModified = file.lastModified(),
                                    sizeBytes = file.length()
                                )
                                onOpenComic(comic)
                            },
                            onToggleFav = null
                        )
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun BrowserRow(
    icon: String,
    title: String,
    subtitle: String,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFav: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 22.sp, modifier = Modifier.width(32.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp
                )
            }
        }
        if (onToggleFav != null) {
            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.4f)
                )
            }
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
