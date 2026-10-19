package com.mangauniversal.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangauniversal.app.data.folder.FolderConfig
import com.mangauniversal.app.data.folder.FolderManager
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.settings.AppSettings
import com.mangauniversal.app.data.settings.ThemePreferences
import com.mangauniversal.app.ui.history.HistoryScreenContent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFF6B00),
                    onPrimary = Color.White,
                    background = Color(0xFF0E0E0E),
                    onBackground = Color.White,
                    surface = Color(0xFF1A1A1A),
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF2A2A2A),
                    onSurfaceVariant = Color.White.copy(alpha = 0.8f),
                    outline = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0E0E0E)
                ) {
                    MangaUniversalApp()
                }
            }
        }
    }
}

sealed class AppTab {
    object Storage : AppTab()
    object Directory : AppTab()
    object History : AppTab()
    object Bookmark : AppTab()
}

@Composable
fun MangaUniversalApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(checkStoragePermission()) }
    var currentTab by remember { mutableStateOf<AppTab>(AppTab.Storage) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    var folders by remember { mutableStateOf<List<FolderConfig>>(emptyList()) }
    var selectedComic by remember { mutableStateOf<Comic?>(null) }
    var comicsInFolder by remember { mutableStateOf<List<Comic>>(emptyList()) }
    var pendingPage by remember { mutableStateOf(-1) }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingPathAfterPermission by remember { mutableStateOf<String?>(null) }

    // Ajustes visuales (Brillo y Filtro Azul)
    val prefs = remember { ThemePreferences(context) }
    var appSettings by remember { mutableStateOf(AppSettings()) }
    
    LaunchedEffect(Unit) {
        appSettings = prefs.load()
    }
    
    // Recargar ajustes cada vez que volvemos al Home (para aplicar cambios del SettingsScreen)
    LaunchedEffect(currentScreen) {
        if (currentScreen is Screen.Home) {
            appSettings = prefs.load()
        }
    }

    val folderManager = remember { FolderManager(context) }
    LaunchedEffect(Unit) {
        folderManager.folders.collectLatest { list ->
            folders = list
        }
    }

    fun requestPermissionAndOpen(path: String) {
        pendingPathAfterPermission = path
        showPermissionDialog = true
        openAllFilesSettings(context.packageName, context)
    }

    if (showPermissionDialog) {
        PermissionDialog(
            onOpenSettings = {
                openAllFilesSettings(context.packageName, context)
            },
            onConfirm = {
                val nowHas = checkStoragePermission()
                hasPermission = nowHas
                if (nowHas) {
                    showPermissionDialog = false
                    val path = pendingPathAfterPermission
                    pendingPathAfterPermission = null
                    if (path != null) {
                        previousScreen = Screen.Home
                        currentScreen = Screen.Directory(path)
                    }
                }
            },
            onCancel = {
                showPermissionDialog = false
                pendingPathAfterPermission = null
            }
        )
    }

    // ENVOLVEMOS TODA LA APP CON EL FILTRO DE BRILLO Y LUZ AZUL
    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.Home -> MainTabsScreen(
                currentTab = currentTab,
                onTabChange = { currentTab = it },
                folders = folders,
                hasPermission = hasPermission,
                onOpenRecientes = {
                    previousScreen = Screen.Home
                    currentScreen = Screen.History
                },
                onOpenStorage = { path ->
                    if (hasPermission) {
                        previousScreen = Screen.Home
                        currentScreen = Screen.Directory(path)
                    } else {
                        requestPermissionAndOpen(path)
                    }
                },
                onOpenFavoriteFolder = { path ->
                    val folder = folders.find { it.path == path }
                    if (folder != null) {
                        previousScreen = Screen.Home
                        currentScreen = Screen.Library(folder)
                    }
                },
                onAddFolder = {
                    if (hasPermission) {
                        val base = Environment.getExternalStorageDirectory().absolutePath
                        previousScreen = Screen.Home
                        currentScreen = Screen.Directory(base)
                    } else {
                        requestPermissionAndOpen(Environment.getExternalStorageDirectory().absolutePath)
                    }
                },
                onOpenHistory = {
                    previousScreen = Screen.Home
                    currentScreen = Screen.History
                },
                onOpenSettings = {
                    previousScreen = Screen.Home
                    currentScreen = Screen.Settings
                },
                onContinueReading = { comic ->
                    comicsInFolder = listOf(comic)
                    selectedComic = comic
                    pendingPage = -1
                    previousScreen = Screen.Home
                    currentScreen = Screen.Reader(comic)
                },
                onOpenComicFromTab = { comic ->
                    comicsInFolder = listOf(comic)
                    selectedComic = comic
                    pendingPage = -1
                    previousScreen = Screen.Home
                    currentScreen = Screen.Reader(comic)
                },
                onOpenBookmark = { comic, page ->
                    comicsInFolder = listOf(comic)
                    selectedComic = comic
                    pendingPage = page
                    previousScreen = Screen.Home
                    currentScreen = Screen.Reader(comic)
                }
            )

            is Screen.Directory -> {
                BackHandler(enabled = true) { currentScreen = previousScreen }
                FileBrowserScreen(
                    startPath = screen.path,
                    favoriteFolders = folders.map { it.path },
                    onBack = { currentScreen = previousScreen },
                    onOpenComic = { comic ->
                        comicsInFolder = listOf(comic)
                        selectedComic = comic
                        pendingPage = -1
                        previousScreen = Screen.Directory(screen.path)
                        currentScreen = Screen.Reader(comic)
                    },
                    onToggleFavorite = { path ->
                        scope.launch {
                            val name = java.io.File(path).name.ifEmpty { path }
                            folderManager.addFolder(path, name)
                        }
                    }
                )
            }

            is Screen.Library -> {
                BackHandler(enabled = true) { currentScreen = previousScreen }
                LibraryScreen(
                    folderPath = screen.folder.path,
                    folderName = screen.folder.name,
                    onBack = { currentScreen = previousScreen },
                    onComicClick = { comic, allComics ->
                        comicsInFolder = allComics
                        selectedComic = comic
                        pendingPage = -1
                        previousScreen = Screen.Library(screen.folder)
                        currentScreen = Screen.Reader(comic)
                    }
                )
            }

            is Screen.Reader -> com.mangauniversal.app.ui.reader.ReaderScreen(
                comic = screen.comic,
                comicsList = comicsInFolder.ifEmpty { listOf(screen.comic) },
                initialPageOverride = pendingPage,
                onBack = {
                    pendingPage = -1
                    currentScreen = previousScreen
                },
                onOpenComic = { newComic ->
                    selectedComic = newComic
                    comicsInFolder = listOf(newComic)
                    pendingPage = -1
                    currentScreen = Screen.Reader(newComic)
                }
            )

            is Screen.History -> {
                BackHandler(enabled = true) { currentScreen = previousScreen }
                com.mangauniversal.app.ui.history.HistoryScreen(
                    onBack = { currentScreen = previousScreen },
                    onOpenComic = { comic, list ->
                        comicsInFolder = list
                        selectedComic = comic
                        pendingPage = -1
                        previousScreen = Screen.History
                        currentScreen = Screen.Reader(comic)
                    }
                )
            }

            is Screen.Settings -> SettingsScreen(
                onBack = { currentScreen = previousScreen }
            )
        }
        
        // CAPA DE BRILLO
        if (appSettings.brightness < 1f && currentScreen !is Screen.Reader) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - appSettings.brightness) * 0.7f))
            )
        }
        
        // CAPA DE FILTRO LUZ AZUL
        if (appSettings.blueLightFilter > 0f && currentScreen !is Screen.Reader) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF9800).copy(alpha = appSettings.blueLightFilter * 0.3f))
            )
        }
    }
}

@Composable
fun PermissionDialog(
    onOpenSettings: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(
                text = "Acceso a archivos",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Para explorar tus cómics, la app necesita acceso a todos los archivos.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "1. Abre ajustes y activa el permiso.\n2. Vuelve y toca \"Ya di el permiso\".",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
            ) {
                Text("Ya di el permiso", color = Color.White, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Abrir ajustes otra vez", color = Color.White, fontSize = 13.sp)
            }
        }
    )
}

sealed class Screen {
    object Home : Screen()
    object History : Screen()
    object Settings : Screen()
    data class Directory(val path: String) : Screen()
    data class Library(val folder: FolderConfig) : Screen()
    data class Reader(val comic: Comic) : Screen()
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

@Composable
fun MainTabsScreen(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    folders: List<FolderConfig>,
    hasPermission: Boolean,
    onOpenRecientes: () -> Unit,
    onOpenStorage: (String) -> Unit,
    onOpenFavoriteFolder: (String) -> Unit,
    onAddFolder: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinueReading: (Comic) -> Unit,
    onOpenComicFromTab: (Comic) -> Unit,
    onOpenBookmark: (Comic, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
        TopBar(
            currentTab = currentTab,
            onTabChange = onTabChange,
            onOpenSettings = onOpenSettings
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentTab) {
                is AppTab.Storage -> StorageExplorerScreen(
                    favoriteFolders = folders.filter { it.isFavorite }.map { it.path },
                    onOpenRecientes = onOpenRecientes,
                    onOpenStorage = onOpenStorage,
                    onOpenFavoriteFolder = onOpenFavoriteFolder,
                    onAddFolder = onAddFolder,
                    onOpenHistory = onOpenHistory,
                    onContinueReading = onContinueReading
                )
                is AppTab.Directory -> DirectoryTab(
                    hasPermission = hasPermission,
                    onOpenStorage = onOpenStorage
                )
                is AppTab.History -> HistoryScreenContent(
                    onOpenComic = { comic, _ -> onOpenComicFromTab(comic) }
                )
                is AppTab.Bookmark -> BookmarkScreen(
                    onOpenBookmark = onOpenBookmark
                )
            }
        }

        BottomBar(
            currentTab = currentTab,
            onTabChange = onTabChange,
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
fun TopBar(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F3A4D))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
            }
            Text(
                text = "0",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { }) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Más", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16293A))
        ) {
            TabItem("Storage", currentTab is AppTab.Storage) { onTabChange(AppTab.Storage) }
            TabItem("Directory", currentTab is AppTab.Directory) { onTabChange(AppTab.Directory) }
            TabItem("History", currentTab is AppTab.History) { onTabChange(AppTab.History) }
            TabItem("Bookmark", currentTab is AppTab.Bookmark) { onTabChange(AppTab.Bookmark) }
        }
    }
}

@Composable
private fun RowScope.TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
        if (selected) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(Color(0xFF4A90E2))
            )
        }
    }
}

@Composable
fun BottomBar(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16293A))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomBarItem(Icons.Default.Edit, "Edit") { }
        BottomBarItem(Icons.Default.History, "Recent") { onTabChange(AppTab.History) }
        BottomBarItem(Icons.Default.GridView, "View") { }
        BottomBarItem(Icons.Default.Settings, "Settings") { onOpenSettings() }
    }
}

@Composable
private fun BottomBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun DirectoryTab(
    hasPermission: Boolean,
    onOpenStorage: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Directorio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Explora tus carpetas de cómics",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                onOpenStorage(Environment.getExternalStorageDirectory().absolutePath)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
        ) {
            Text("Abrir almacenamiento", color = Color.White)
        }
    }
}
