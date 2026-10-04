package com.mangauniversal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun LibraryScreen(
    folderPath: String,
    folderName: String,
    onBack: () -> Unit,
    onComicClick: (Comic) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ViewPreferences(context) }

    var comics by remember { mutableStateOf<List<Comic>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST_THUMB) }

    LaunchedEffect(Unit) {
        val saved = prefs.getViewMode()
        if (saved != null) viewMode = saved
    }

    LaunchedEffect(folderPath) {
        isLoading = true
        comics = withContext(Dispatchers.IO) {
            ComicScanner(context).scanFolder(folderPath)
        }
        isLoading = false
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
                    text = folderName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folderPath,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (comics != null && comics!!.isNotEmpty()) {
                Text(
                    text = "✅ ${comics!!.size}",
                    color = Color(0xFFFF6B00),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ViewButton(
                text = "☰",
                active = viewMode == ViewMode.LIST_THUMB,
                onClick = {
                    viewMode = ViewMode.LIST_THUMB
                    scope.launch { prefs.saveViewMode(ViewMode.LIST_THUMB) }
                }
            )
            ViewButton(
                text = "≡",
                active = viewMode == ViewMode.LIST_COMPACT,
                onClick = {
                    viewMode = ViewMode.LIST_COMPACT
                    scope.launch { prefs.saveViewMode(ViewMode.LIST_COMPACT) }
                }
            )
            ViewButton(
                text = "⊞",
                active = viewMode == ViewMode.GRID_2,
                onClick = {
                    viewMode = ViewMode.GRID_2
                    scope.launch { prefs.saveViewMode(ViewMode.GRID_2) }
                }
            )
            ViewButton(
                text = "⊞⊞",
                active = viewMode == ViewMode.GRID_4,
                onClick = {
                    viewMode = ViewMode.GRID_4
                    scope.launch { prefs.saveViewMode(ViewMode.GRID_4) }
                }
            )
            ViewButton(
                text = "⊞⊞⊞",
                active = viewMode == ViewMode.GRID_6,
                onClick = {
                    viewMode = ViewMode.GRID_6
                    scope.launch { prefs.saveViewMode(ViewMode.GRID_6) }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF6B00))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Escaneando carpeta...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            comics == null || comics!!.isEmpty() -> {
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
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(comics!!, key = { it.id }) { comic ->
                            Box(modifier = Modifier.clickable { onComicClick(comic) }) {
                                ComicRowWithThumbnail(comic)
                            }
                        }
                    }

                    ViewMode.LIST_COMPACT -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(comics!!, key = { it.id }) { comic ->
                            Box(modifier = Modifier.clickable { onComicClick(comic) }) {
                                ComicRowCompact(comic)
                            }
                        }
                    }

                    ViewMode.GRID_2 -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(comics!!, key = { it.id }) { comic ->
                            Box(modifier = Modifier.clickable { onComicClick(comic) }) {
                                ComicGridItem(comic)
                            }
                        }
                    }

                    ViewMode.GRID_4 -> LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(comics!!, key = { it.id }) { comic ->
                            Box(modifier = Modifier.clickable { onComicClick(comic) }) {
                                ComicGridItem(comic)
                            }
                        }
                    }

                    ViewMode.GRID_6 -> LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(comics!!, key = { it.id }) { comic ->
                            Box(modifier = Modifier.clickable { onComicClick(comic) }) {
                                ComicGridSmall(comic)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewButton(text: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFFFF6B00) else Color(0xFF333333)
        ),
        modifier = Modifier
            .width(44.dp)
            .padding(horizontal = 1.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
private fun ComicRowWithThumbnail(comic: Comic) {
    val context = LocalContext.current
    var thumbPath by remember(comic.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(comic.id) {
        thumbPath = if (comic.type == ComicType.FOLDER && comic.coverPath != null) {
            comic.coverPath
        } else {
            ThumbnailExtractor(context).getThumbnail(comic)?.absolutePath
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
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
                Text(text = iconFor(comic.type), fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comic.title,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
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
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = iconFor(comic.type), fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = comic.title,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ComicGridItem(comic: Comic) {
    val context = LocalContext.current
    var thumbPath by remember(comic.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(comic.id) {
        thumbPath = if (comic.type == ComicType.FOLDER && comic.coverPath != null) {
            comic.coverPath
        } else {
            ThumbnailExtractor(context).getThumbnail(comic)?.absolutePath
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
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
        Spacer(modifier = Modifier.height(6.dp))
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

@Composable
private fun ComicGridSmall(comic: Comic) {
    val context = LocalContext.current
    var thumbPath by remember(comic.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(comic.id) {
        thumbPath = if (comic.type == ComicType.FOLDER && comic.coverPath != null) {
            comic.coverPath
        } else {
            ThumbnailExtractor(context).getThumbnail(comic)?.absolutePath
        }
    }

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
            Text(text = iconFor(comic.type), fontSize = 24.sp)
        }
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
