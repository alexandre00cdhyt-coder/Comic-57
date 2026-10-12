package com.mangauniversal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.scanner.ComicScanner
import com.mangauniversal.app.data.settings.ViewPreferences
import com.mangauniversal.app.data.thumbnail.ThumbnailExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LibraryScreen(
    folderPath: String,
    folderName: String,
    onBack: () -> Unit,
    onComicClick: (Comic, List<Comic>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ViewPreferences(context) }
    val thumbnailExtractor = remember { ThumbnailExtractor(context) }

    var comics by remember { mutableStateOf<List<Comic>?>(null) }
    var subfolders by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentPath by remember(folderPath) { mutableStateOf(folderPath) }
    var isLoading by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST_THUMB) }
    var searchQuery by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val saved = prefs.getViewMode()
        if (saved != null) viewMode = saved
    }

    LaunchedEffect(currentPath) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val folder = File(currentPath)
            subfolders = try {
                folder.listFiles()
                    ?.filter { it.isDirectory && !it.name.startsWith(".") && it.canRead() }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            comics = ComicScanner(context).scanFolder(currentPath)
        }
        isLoading = false
    }

    val displayedComics = remember(comics, searchQuery, sortAscending) {
        val base = comics ?: emptyList()
        val filtered = if (searchQuery.isBlank()) base
        else base.filter { it.title.contains(searchQuery, ignoreCase = true) }
        val sorted = filtered.sortedBy { it.title.lowercase() }
        if (sortAscending) sorted else sorted.reversed()
    }

    val currentDir = remember(currentPath) { File(currentPath) }
    val parentDir = currentDir.parentFile
    val isRootFolder = currentPath == folderPath
    val canGoUp = !isRootFolder && parentDir != null

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
            IconButton(onClick = {
                if (canGoUp) {
                    currentPath = parentDir!!.absolutePath
                } else {
                    onBack()
                }
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = currentDir.name.ifEmpty { folderName },
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
            IconButton(onClick = { searchVisible = !searchVisible }) {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
            }
            IconButton(onClick = { sortAscending = !sortAscending }) {
                Icon(Icons.Default.Sort, contentDescription = "Orden", tint = Color.White)
            }
        }

        if (searchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Buscar...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16293A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✅ ${displayedComics.size} cómics · ${subfolders.size} carpetas",
                color = Color(0xFFFF6B00),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    viewMode = ViewMode.LIST_THUMB
                    scope.launch { prefs.saveViewMode(ViewMode.LIST_THUMB) }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.ViewList,
                    contentDescription = "Lista",
                    tint = if (viewMode == ViewMode.LIST_THUMB) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.5f)
                )
            }
            IconButton(
                onClick = {
                    viewMode = ViewMode.GRID_2
                    scope.launch { prefs.saveViewMode(ViewMode.GRID_2) }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = "Cuadrícula",
                    tint = if (viewMode == ViewMode.GRID_2) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.5f)
                )
            }
            IconButton(
                onClick = {
                    viewMode = ViewMode.LIST_COMPACT
                    scope.launch { prefs.saveViewMode(ViewMode.LIST_COMPACT) }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = "Compacto",
                    tint = if (viewMode == ViewMode.LIST_COMPACT) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.5f)
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF6B00))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Escaneando...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            (comics == null || comics!!.isEmpty()) && subfolders.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron cómics",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                when (viewMode) {
                    ViewMode.LIST_THUMB -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        if (subfolders.isNotEmpty()) {
                            item {
                                Text(
                                    "Carpetas",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                                )
                            }
                            items(subfolders, key = { it.absolutePath }) { dir ->
                                FolderRow(dir) { currentPath = dir.absolutePath }
                            }
                        }
                        if (displayedComics.isNotEmpty()) {
                            item {
                                Text(
                                    "Cómics",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                                )
                            }
                            items(displayedComics, key = { it.id }) { comic ->
                                Row(Modifier.clickable { onComicClick(comic, displayedComics) }) {
                                    ComicRowWithThumbnail(comic, thumbnailExtractor)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }

                    ViewMode.LIST_COMPACT -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        if (subfolders.isNotEmpty()) {
                            items(subfolders, key = { it.absolutePath }) { dir ->
                                FolderRow(dir) { currentPath = dir.absolutePath }
                            }
                        }
                        items(displayedComics, key = { it.id }) { comic ->
                            Row(Modifier.clickable { onComicClick(comic, displayedComics) }) {
                                ComicRowCompact(comic)
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }

                    ViewMode.GRID_2 -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        if (subfolders.isNotEmpty()) {
                            items(subfolders, key = { it.absolutePath }) { dir ->
                                FolderGridItem(dir) { currentPath = dir.absolutePath }
                            }
                        }
                        items(displayedComics, key = { it.id }) { comic ->
                            Row(Modifier.clickable { onComicClick(comic, displayedComics) }) {
                                ComicGridItem(comic, thumbnailExtractor)
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                    ) {
                        items(displayedComics, key = { it.id }) { comic ->
                            Row(Modifier.clickable { onComicClick(comic, displayedComics) }) {
                                ComicRowWithThumbnail(comic, thumbnailExtractor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(dir: File, onClick: () -> Unit) {
    val count = remember(dir.absolutePath) {
        try { dir.listFiles()?.size ?: 0 } catch (e: Exception) { 0 }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📁", fontSize = 22.sp, modifier = Modifier.width(32.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dir.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count > 0) {
                Text(
                    text = "$count elementos",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun FolderGridItem(dir: File, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Text("📁", fontSize = 48.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = dir.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ComicRowWithThumbnail(comic: Comic, extractor: ThumbnailExtractor) {
    var thumbPath by remember(comic.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(comic.id) {
        thumbPath = if (comic.type == ComicType.FOLDER && comic.coverPath != null) {
            comic.coverPath
        } else {
            extractor.getThumbnail(comic)?.absolutePath
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp, 76.dp)
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
                Text(text = iconFor(comic.type), fontSize = 28.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comic.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${comic.type} · ${formatSize(comic.sizeBytes)}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ComicRowCompact(comic: Comic) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = iconFor(comic.type), fontSize = 18.sp, modifier = Modifier.width(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = comic.title,
            color = Color.White,
            fontSize = 13.sp,
                        maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ComicGridItem(comic: Comic, extractor: ThumbnailExtractor) {
    var thumbPath by remember(comic.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(comic.id) {
        thumbPath = if (comic.type == ComicType.FOLDER && comic.coverPath != null) {
            comic.coverPath
        } else {
            extractor.getThumbnail(comic)?.absolutePath
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
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
                Text(text = iconFor(comic.type), fontSize = 40.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = comic.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
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
            
