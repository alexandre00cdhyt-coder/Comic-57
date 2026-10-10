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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun MangaUniversalApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(checkStoragePermission()) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    var folders by remember { mutableStateOf<List<FolderConfig>>(emptyList()) }
    var selectedComic by remember { mutableStateOf<Comic?>(null) }
    var comicsInFolder by remember { mutableStateOf<List<Comic>>(emptyList()) }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingPathAfterPermission by remember { mutableStateOf<String?>(null) }

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

    // Gesto de atrás global: en el home cierra la app
    BackHandler(enabled = currentScreen is Screen.Home) {
        // Comportamiento por defecto: cerrar la app
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
                        currentScreen = Screen.FileBrowser(path)
                    }
                }
            },
            onCancel = {
                showPermissionDialog = false
                pendingPathAfterPermission = null
            }
        )
    }

    when (val screen = currentScreen) {
        is Screen.Home -> StorageExplorerScreen(
            favoriteFolders = folders.filter { it.isFavorite }.map { it.path },
            onOpenRecientes = {
                previousScreen = Screen.Home
                currentScreen = Screen.History
            },
            onOpenStorage = { path ->
                if (hasPermission) {
                    previousScreen = Screen.Home
                    currentScreen = Screen.FileBrowser(path)
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
                    currentScreen = Screen.FileBrowser(base)
                } else {
                    requestPermissionAndOpen(Environment.getExternalStorageDirectory().absolutePath)
                }
            },
            onOpenHistory = {
                previousScreen = Screen.Home
                currentScreen = Screen.History
            }
        )

        is Screen.FileBrowser -> FileBrowserScreen(
            startPath = screen.path,
            favoriteFolders = folders.map { it.path },
            onBack = { currentScreen = previousScreen },
            onOpenComic = { comic ->
                comicsInFolder = listOf(comic)
                selectedComic = comic
                previousScreen = Screen.FileBrowser(screen.path)
                currentScreen = Screen.Reader(comic)
            },
            onToggleFavorite = { path ->
                scope.launch {
                    val name = java.io.File(path).name.ifEmpty { path }
                    folderManager.addFolder(path, name)
                }
            }
        )

        is Screen.Library -> LibraryScreen(
            folderPath = screen.folder.path,
            folderName = screen.folder.name,
            onBack = { currentScreen = previousScreen },
            onComicClick = { comic, allComics ->
                comicsInFolder = allComics
                selectedComic = comic
                previousScreen = Screen.Library(screen.folder)
                currentScreen = Screen.Reader(comic)
            }
        )

        is Screen.Reader -> com.mangauniversal.app.ui.reader.ReaderScreen(
            comic = screen.comic,
            comicsList = comicsInFolder.ifEmpty { listOf(screen.comic) },
            onBack = {
                currentScreen = previousScreen
            },
            onOpenComic = { newComic ->
                selectedComic = newComic
                comicsInFolder = listOf(newComic)
                currentScreen = Screen.Reader(newComic)
            }
        )

        is Screen.History -> com.mangauniversal.app.ui.history.HistoryScreen(
            onBack = { currentScreen = previousScreen },
            onOpenComic = { comic, list ->
                comicsInFolder = list
                selectedComic = comic
                previousScreen = Screen.History
                currentScreen = Screen.Reader(comic)
            }
        )
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
    data class FileBrowser(val path: String) : Screen()
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
