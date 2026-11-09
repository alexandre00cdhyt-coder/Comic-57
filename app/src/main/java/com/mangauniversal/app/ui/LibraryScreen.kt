package com.mangauniversal.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangauniversal.app.data.files.FileOperations
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
    var sortMode by remember { mutableStateOf(0) }

    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }
    var fileToMove by remember { mutableStateOf<File?>(null) }
    var fileToCopy by remember { mutableStateOf<File?>(null) }
    var isCutOperation by remember { mutableStateOf(false) }

    var comicToExtract by remember { mutableStateOf<Comic?>(null) }
    var extractFolderName by remember { mutableStateOf("") }
    var extractQuality by remember { mutableStateOf("Alta (100%)") }
    var isExtracting by remember { mutableStateOf(false) }
    var extractionProgress by remember { mutableStateOf(0 to 0) }

    var isEditMode by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }

    fun reloadCurrentFolder() {
        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                val folder = File(currentPath)
                subfolders = try {
                    folder.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") && it.canRead() }?.sortedBy { it.name.lowercase() } ?: emptyList()
                } catch (e: Exception) { emptyList() }
                comics = ComicScanner(context).scanFolder(currentPath)
            }
            isLoading = false
        }
    }

    fun toggleSelection(file: File) {
        selectedFiles = if (selectedFiles.contains(file)) selectedFiles - file else selectedFiles + file
    }

    fun pasteFile() {
        val source = fileToCopy ?: return
        val destination = File(currentPath)
        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                if (isCutOperation) FileOperations.move(source, destination) else FileOperations.copy(source, destination)
            }
            fileToCopy = null
            isCutOperation = false
            reloadCurrentFolder()
        }
    }

    LaunchedEffect(Unit) {
        val saved = prefs.getViewMode()
        if (saved != null) viewMode = saved
    }

    LaunchedEffect(currentPath) {
        reloadCurrentFolder()
        selectedFiles = emptySet()
        isEditMode = false
    }

    val displayedComics = remember(comics, searchQuery, sortMode) {
        val base = comics ?: emptyList()
        val filtered = if (searchQuery.isBlank()) base else base.filter { it.title.contains(searchQuery, ignoreCase = true) }
        when (sortMode) {
            0 -> filtered.sortedBy { it.title.lowercase() }
            1 -> filtered.sortedByDescending { it.title.lowercase() }
            2 -> filtered.sortedByDescending { it.lastModified }
            3 -> filtered.sortedByDescending { it.sizeBytes }
            else -> filtered
        }
    }

    val currentDir = remember(currentPath) { File(currentPath) }
    val parentDir = currentDir.parentFile
    val isRootFolder = currentPath == folderPath
    val canGoUp = !isRootFolder && parentDir != null

    BackHandler(enabled = true) {
        if (isEditMode) { isEditMode = false; selectedFiles = emptySet() }
        else if (canGoUp) currentPath = parentDir!!.absolutePath
        else onBack()
    }
        // --- DIÁLOGOS ---
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Eliminar", color = Color.White) },
            text = { Text("¿Seguro que deseas eliminar '${fileToDelete!!.name}'?", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    val f = fileToDelete!!
                    fileToDelete = null
                    scope.launch { FileOperations.delete(f); reloadCurrentFolder() }
                }) { Text("Eliminar", color = Color(0xFFFF5555)) }
            },
            dismissButton = { TextButton(onClick = { fileToDelete = null }) { Text("Cancelar", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Renombrar", color = Color.White) },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, textStyle = TextStyle(color = Color.White), modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val f = fileToRename!!
                    val newName = renameText
                    fileToRename = null
                    if (newName.isNotBlank() && newName != f.name) {
                        scope.launch { FileOperations.rename(f, newName); reloadCurrentFolder() }
                    }
                }) { Text("Guardar", color = Color(0xFFFF6B00)) }
            },
            dismissButton = { TextButton(onClick = { fileToRename = null }) { Text("Cancelar", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (fileToMove != null) {
        val subDirs = File(currentPath).listFiles()?.filter { it.isDirectory } ?: emptyList()
        var selectedMoveTarget by remember { mutableStateOf<File?>(null) }
        AlertDialog(
            onDismissRequest = { fileToMove = null },
            title = { Text("Mover archivo", color = Color.White) },
            text = {
                Column {
                    Text("¿A dónde quieres mover '${fileToMove!!.name}'?", color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(subDirs) { dir ->
                            Text(text = dir.name, color = Color.White, modifier = Modifier.fillMaxWidth().clickable { selectedMoveTarget = dir }.background(if (selectedMoveTarget == dir) Color(0xFF2A2A2A) else Color.Transparent).padding(10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val source = fileToMove!!; val dest = selectedMoveTarget
                    fileToMove = null
                    if (dest != null) scope.launch { FileOperations.move(source, dest); reloadCurrentFolder() }
                }, enabled = selectedMoveTarget != null) { Text("Mover aquí", color = Color(0xFFFF6B00)) }
            },
            dismissButton = { TextButton(onClick = { fileToMove = null }) { Text("Cancelar", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (comicToExtract != null) {
        AlertDialog(
            onDismissRequest = { if (!isExtracting) comicToExtract = null },
            title = { Text("Extraer imágenes", color = Color.White) },
            text = {
                Column {
                    if (isExtracting) {
                        Text("Extrayendo... ${extractionProgress.first} / ${extractionProgress.second}", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(color = Color(0xFFFF6B00), modifier = Modifier.size(40.dp))
                    } else {
                        Text("Nombre de la carpeta:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        OutlinedTextField(value = extractFolderName, onValueChange = { extractFolderName = it }, singleLine = true, textStyle = TextStyle(color = Color.White), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text("Calidad:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Alta (100%)", "Media (75%)", "Baja (50%)").forEach { q ->
                                Button(onClick = { extractQuality = q }, colors = ButtonDefaults.buttonColors(containerColor = if (extractQuality == q) Color(0xFFFF6B00) else Color(0xFF2A2A2A)), modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text(q, fontSize = 11.sp, color = Color.White) }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isExtracting) {
                    Button(onClick = {
                        val comic = comicToExtract!!
                        val calidad = when (extractQuality) { "Alta (100%)" -> 100; "Media (75%)" -> 75; else -> 50 }
                        isExtracting = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                FileOperations.extractImages(context, comic, calidad, reducirTamaño = calidad < 100) { current, total -> extractionProgress = current to total }
                            }
                            isExtracting = false
                            comicToExtract = null
                            reloadCurrentFolder()
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))) { Text("Extraer", color = Color.White) }
                }
            },
            dismissButton = { if (!isExtracting) TextButton(onClick = { comicToExtract = null }) { Text("Cancelar", color = Color.White) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    // --- INTERFAZ PRINCIPAL ---
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1F3A4D).copy(alpha = 0.9f)).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (isEditMode) { isEditMode = false; selectedFiles = emptySet() }
                else if (canGoUp) currentPath = parentDir!!.absolutePath
                else onBack()
            }) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White) }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(text = if (isEditMode) "${selectedFiles.size} seleccionados" else currentDir.name.ifEmpty { folderName }, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = currentPath, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!isEditMode) {
                IconButton(onClick = { searchVisible = !searchVisible }) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White) }
                Box {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Default.Sort, contentDescription = "Orden", tint = Color.White) }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }, modifier = Modifier.background(Color(0xFF2A2A2A))) {
                        DropdownMenuItem(text = { Text("Nombre (A - Z)", color = Color.White) }, onClick = { sortMode = 0; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Nombre (Z - A)", color = Color.White) }, onClick = { sortMode = 1; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Más reciente", color = Color.White) }, onClick = { sortMode = 2; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Más grande", color = Color.White) }, onClick = { sortMode = 3; showSortMenu = false })
                    }
                }
            }
        }

        if (searchVisible && !isEditMode) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Buscar...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = 14.sp))
        }

        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF16293A).copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "✅ ${displayedComics.size} cómics · ${subfolders.size} carpetas", color = Color(0xFFFF6B00), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (!isEditMode) {
                IconButton(onClick = { viewMode = ViewMode.LIST_THUMB; scope.launch { prefs.saveViewMode(ViewMode.LIST_THUMB) } }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ViewList, contentDescription = "Lista", tint = if (viewMode == ViewMode.LIST_THUMB) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.5f)) }
                IconButton(onClick = { viewMode = ViewMode.GRID_2; scope.launch { prefs.saveViewMode(ViewMode.GRID_2) } }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.GridView, contentDescription = "Cuadrícula", tint = if (viewMode == ViewMode.GRID_2) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.5f)) }
                IconButton(onClick = { viewMode = ViewMode.LIST_COMPACT; scope.launch { prefs.saveViewMode(ViewMode.LIST_COMPACT) } }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.List, contentDescription = "Compacto", tint = if (viewMode == ViewMode.LIST_COMPACT) Color(0xFFFF6B00) else Color.White.copy(alpha = 0.5f)) }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFF6B00)) }
                (comics == null || comics!!.isEmpty()) && subfolders.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No se encontraron cómics", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) }
                else -> {
                    when (viewMode) {
                        ViewMode.LIST_THUMB, ViewMode.LIST_COMPACT -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subfolders, key = { it.absolutePath }) { dir ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (isEditMode) toggleSelection(dir) else currentPath = dir.absolutePath }) {
                                    if (isEditMode) Checkbox(checked = selectedFiles.contains(dir), onCheckedChange = { toggleSelection(dir) })
                                    FolderRow(dir, { if (isEditMode) toggleSelection(dir) else currentPath = dir.absolutePath }, { fileToDelete = dir }, { fileToRename = dir; renameText = dir.name }, { fileToMove = dir })
                                }
                            }
                            items(displayedComics, key = { it.id }) { comic ->
                                val file = File(comic.path)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (isEditMode) toggleSelection(file) else onComicClick(comic, displayedComics) }) {
                                    if (isEditMode) Checkbox(checked = selectedFiles.contains(file), onCheckedChange = { toggleSelection(file) })
                                    ComicRowWithThumbnail(comic, thumbnailExtractor, { fileToDelete = file }, { fileToRename = file; renameText = file.name }, { fileToMove = file }, { fileToCopy = file; isCutOperation = false }, { fileToCopy = file; isCutOperation = true }, { comicToExtract = comic; extractFolderName = comic.title })
                                }
                            }
                            item { Spacer(Modifier.height(20.dp)) }
                        }
                        else -> {
                            val columns = when(viewMode) { ViewMode.GRID_2 -> 2; ViewMode.GRID_3 -> 3; ViewMode.GRID_4 -> 4; else -> 6 }
                            LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(subfolders, key = { it.absolutePath }) { dir ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (isEditMode) toggleSelection(dir) else currentPath = dir.absolutePath }) {
                                        if (isEditMode) Checkbox(checked = selectedFiles.contains(dir), onCheckedChange = { toggleSelection(dir) })
                                        FolderGridItem(dir, { if (isEditMode) toggleSelection(dir) else currentPath = dir.absolutePath }, { fileToDelete = dir }, { fileToRename = dir; renameText = dir.name }, { fileToMove = dir })
                                    }
                                }
                                items(displayedComics, key = { it.id }) { comic ->
                                    val file = File(comic.path)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (isEditMode) toggleSelection(file) else onComicClick(comic, displayedComics) }) {
                                        if (isEditMode) Checkbox(checked = selectedFiles.contains(file), onCheckedChange = { toggleSelection(file) })
                                        ComicGridItem(comic, thumbnailExtractor, { fileToDelete = file }, { fileToRename = file; renameText = file.name }, { fileToMove = file }, { fileToCopy = file; isCutOperation = false }, { fileToCopy = file; isCutOperation = true }, { comicToExtract = comic; extractFolderName = comic.title })
                                    }
                                }
                                item { Spacer(Modifier.height(20.dp)) }
                            }
                        }
                    }
                }
            }
        }
        
        if (fileToCopy != null || (isEditMode && selectedFiles.isNotEmpty())) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1F3A4D)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (isEditMode && selectedFiles.isNotEmpty()) {
                    Button(onClick = { scope.launch { withContext(Dispatchers.IO) { selectedFiles.forEach { FileOperations.delete(it) } }; selectedFiles = emptySet(); isEditMode = false; reloadCurrentFolder() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555))) { Text("Eliminar ${selectedFiles.size} seleccionados", color = Color.White) }
                } else if (fileToCopy != null) {
                    Button(onClick = { pasteFile() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))) { Text(if (isCutOperation) "Mover aquí" else "Pegar aquí", color = Color.White) }
                }
            }
        }
    }
}
@Composable
private fun FileOptionsMenu(
    onDelete: () -> Unit, 
    onRename: () -> Unit, 
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onExtract: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White.copy(alpha = 0.5f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF2A2A2A))) {
            DropdownMenuItem(text = { Text("Copiar", color = Color.White) }, onClick = { expanded = false; onCopy() }, leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = Color.White) })
            DropdownMenuItem(text = { Text("Cortar", color = Color.White) }, onClick = { expanded = false; onCut() }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) })
            DropdownMenuItem(text = { Text("Mover a...", color = Color.White) }, onClick = { expanded = false; onMove() }, leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White) })
            DropdownMenuItem(text = { Text("Renombrar", color = Color.White) }, onClick = { expanded = false; onRename() }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) })
            DropdownMenuItem(text = { Text("Extraer imágenes", color = Color.White) }, onClick = { expanded = false; onExtract() }, leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = Color.White) })
            DropdownMenuItem(text = { Text("Eliminar", color = Color(0xFFFF5555)) }, onClick = { expanded = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5555)) })
        }
    }
}

@Composable
private fun FolderRow(
    dir: File, 
    onOpen: () -> Unit, 
    onDelete: () -> Unit, 
    onRename: () -> Unit, 
    onMove: () -> Unit
) {
    val count = remember(dir.absolutePath) { try { dir.listFiles()?.size ?: 0 } catch (e: Exception) { 0 } }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A).copy(alpha = 0.9f)).clickable(onClick = onOpen).padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📁", fontSize = 22.sp, modifier = Modifier.width(32.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = dir.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (count > 0) Text(text = "$count elementos", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
        }
        FileOptionsMenu(
            onDelete = onDelete, 
            onRename = onRename, 
            onMove = onMove, 
            onCopy = { }, 
            onCut = { }, 
            onExtract = { }
        )
    }
}

@Composable
private fun FolderGridItem(
    dir: File, 
    onOpen: () -> Unit, 
    onDelete: () -> Unit, 
    onRename: () -> Unit, 
    onMove: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A).copy(alpha = 0.9f)).clickable(onClick = onOpen).padding(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A2A).copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
            Text("📁", fontSize = 48.sp)
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                FileOptionsMenu(
                    onDelete = onDelete, 
                    onRename = onRename, 
                    onMove = onMove, 
                    onCopy = { }, 
                    onCut = { }, 
                    onExtract = { }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(text = dir.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ComicRowWithThumbnail(
    comic: Comic, 
    extractor: ThumbnailExtractor, 
    onDelete: () -> Unit, 
    onRename: () -> Unit, 
    onMove: () -> Unit,
    onCopy: () -> Unit, 
    onCut: () -> Unit,
    onExtract: () -> Unit
) {
    var thumbPath by remember(comic.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(comic.id) {
        thumbPath = when {
            comic.type == ComicType.FOLDER && comic.coverPath != null -> comic.coverPath
            comic.type == ComicType.CBZ || comic.type == ComicType.ZIP -> { try { extractor.getThumbnail(comic)?.absolutePath } catch (e: Exception) { null } }
            else -> null
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A).copy(alpha = 0.9f)).padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp, 76.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A2A)), contentAlignment = Alignment.Center) {
            if (thumbPath != null) AsyncImage(model = thumbPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(text = iconFor(comic.type), fontSize = 28.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = comic.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(text = "${comic.type} · ${formatSize(comic.sizeBytes)}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        FileOptionsMenu(
            onDelete = onDelete, 
            onRename = onRename, 
            onMove = onMove, 
            onCopy = onCopy, 
            onCut = onCut, 
            onExtract = onExtract
        )
    }
}

@Composable
private fun ComicGridItem(
    comic: Comic, 
    extractor: ThumbnailExtractor, 
    onDelete: () -> Unit, 
    onRename: () -> Unit, 
    onMove: () -> Unit,
    onCopy: () -> Unit, 
    onCut: () -> Unit,
    onExtract: () -> Unit
) {
    var thumbPath by remember(comic.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(comic.id) {
        thumbPath = when {
            comic.type == ComicType.FOLDER && comic.coverPath != null -> comic.coverPath
            comic.type == ComicType.CBZ || comic.type == ComicType.ZIP -> { try { extractor.getThumbnail(comic)?.absolutePath } catch (e: Exception) { null } }
            else -> null
        }
    }
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A).copy(alpha = 0.9f)).padding(6.dp)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A2A)), contentAlignment = Alignment.Center) {
            if (thumbPath != null) AsyncImage(model = thumbPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(text = iconFor(comic.type), fontSize = 40.sp)
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                FileOptionsMenu(
                    onDelete = onDelete, 
                    onRename = onRename, 
                    onMove = onMove, 
                    onCopy = onCopy, 
                    onCut = onCut, 
                    onExtract = onExtract
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(text = comic.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp)
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
