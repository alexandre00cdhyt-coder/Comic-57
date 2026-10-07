package com.mangauniversal.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangauniversal.app.data.folder.FolderConfig
import com.mangauniversal.app.data.folder.FolderManager
import com.mangauniversal.app.data.model.Comic
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    MangaUniversalApp()
                }
            }
        }
    }
}

@Composable
fun MangaUniversalApp() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkStoragePermission()) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        hasPermission = checkStoragePermission()
    }

    if (hasPermission) {
        FolderFlowScreen()
    } else {
        PermissionScreen(
            onOpenSettings = { openAllFilesSettings(context.packageName, context) },
            onCheckAgain = { refreshKey++ }
        )
    }
}

@Composable
fun FolderFlowScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folderManager = remember { FolderManager(context) }

    var folders by remember { mutableStateOf<List<FolderConfig>?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<FolderConfig?>(null) }
    var selectedComic by remember { mutableStateOf<Comic?>(null) }
    var comicsInFolder by remember { mutableStateOf<List<Comic>>(emptyList()) }
    var folderToDelete by remember { mutableStateOf<FolderConfig?>(null) }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }
    var showHistory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        folderManager.folders.collectLatest { list ->
            folders = list
        }
    }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Eliminar carpeta", color = Color.White, fontSize = 16.sp) },
            text = {
                Text(
                    "¿Quieres eliminar \"${folderToDelete!!.name}\"?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        folderManager.removeFolder(folderToDelete!!.path)
                        folderToDelete = null
                    }
                }) {
                    Text("Eliminar", color = Color(0xFFFF5555), fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancelar", color = Color.White, fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    when {
        folders == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Cargando...", color = Color.White, fontSize = 13.sp)
        }

        showPicker -> FolderPickerScreen(
            onFolderSelected = { path, name ->
                scope.launch {
                    folderManager.addFolder(path, name)
                    showPicker = false
                }
            },
            onCancel = { showPicker = false }
        )

        showHistory -> com.mangauniversal.app.ui.history.HistoryScreen(
            onBack = { showHistory = false },
            onOpenComic = { comic, list ->
                comicsInFolder = list
                selectedComic = comic
                showHistory = false
            }
        )

        selectedComic != null -> com.mangauniversal.app.ui.reader.ReaderScreen(
            comic = selectedComic!!,
            comicsList = comicsInFolder,
            onBack = { selectedComic = null },
            onOpenComic = { newComic -> selectedComic = newComic }
        )

        selectedFolder != null -> LibraryScreen(
            folderPath = selectedFolder!!.path,
            folderName = selectedFolder!!.name,
            onBack = { selectedFolder = null },
            onComicClick = { comic, allComics ->
                comicsInFolder = allComics
                selectedComic = comic
            }
        )

        folders!!.isEmpty() -> NoFoldersScreen(
            onAddFolder = { showPicker = true }
        )

        else -> FoldersHomeScreen(
            folders = folders!!,
            filterMode = filterMode,
            onFilterChange = { filterMode = it },
            onFolderClick = { selectedFolder = it },
            onAddFolder = { showPicker = true },
            onToggleFavorite = { path ->
                scope.launch { folderManager.toggleFavorite(path) }
            },
            onDeleteRequest = { folder -> folderToDelete = folder },
            onOpenHistory = { showHistory = true }
        )
    }
}

enum class FilterMode {
    ALL,
    FAVORITES_ONLY
}

@Composable
fun NoFoldersScreen(onAddFolder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No tienes carpetas configuradas", color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Elige una carpeta donde tengas tus cómics.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddFolder,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EAA))
        ) {
            Text("➕ Añadir carpeta", fontSize = 13.sp)
        }
    }
}

@Composable
fun FoldersHomeScreen(
    folders: List<FolderConfig>,
    filterMode: FilterMode,
    onFilterChange: (FilterMode) -> Unit,
    onFolderClick: (FolderConfig) -> Unit,
    onAddFolder: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteRequest: (FolderConfig) -> Unit,
    onOpenHistory: () -> Unit
) {
    val displayFolders = when (filterMode) {
        FilterMode.ALL -> folders
        FilterMode.FAVORITES_ONLY -> folders.filter { it.isFavorite }
    }

    val favorites = folders.filter { it.isFavorite }
    val hasFavorites = favorites.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📚 Carpetas configuradas",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "📖",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenHistory() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            "Toca para entrar · ⭐ favorita · 🗑 eliminar · 📖 historial",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (hasFavorites) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    text = "Todas (${folders.size})",
                    active = filterMode == FilterMode.ALL,
                    onClick = { onFilterChange(FilterMode.ALL) }
                )
                FilterChip(
                    text = "⭐ Favoritas (${favorites.size})",
                    active = filterMode == FilterMode.FAVORITES_ONLY,
                    onClick = { onFilterChange(FilterMode.FAVORITES_ONLY) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (displayFolders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay carpetas que mostrar",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        } else {
            displayFolders.forEach { folder ->
                FolderRowCompact(
                    folder = folder,
                    onClick = { onFolderClick(folder) },
                    onToggleFavorite = { onToggleFavorite(folder.path) },
                    onDelete = { onDeleteRequest(folder) }
                )
                Spacer(modifier = Modifier.height(3.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onAddFolder,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EAA))
        ) {
            Text("➕ Añadir otra carpeta", fontSize = 13.sp)
        }
    }
}

@Composable
private fun FilterChip(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Color(0xFFFF6B00) else Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun FolderRowCompact(
    folder: FolderConfig,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "📁 ${folder.name}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = folder.path,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = if (folder.isFavorite) "⭐" else "☆",
            fontSize = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onToggleFavorite)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Text(
            text = "🗑",
            fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onDelete)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

fun checkStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else true
}

fun openAllFilesSettings(packageName: String, context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            context.startActivity(intent)
        }
    }
}

@Composable
fun PermissionScreen(onOpenSettings: () -> Unit, onCheckAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Acceso a archivos", color = Color.White, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Para escanear tus cómics, la app necesita acceso a todos los archivos.",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EAA))
        ) {
            Text("Abrir ajustes", fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCheckAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
        ) {
            Text("Ya di el permiso", fontSize = 13.sp)
        }
    }
}
