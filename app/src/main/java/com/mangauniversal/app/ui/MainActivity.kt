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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mangauniversal.app.data.folder.FolderConfig
import com.mangauniversal.app.data.folder.FolderManager
import com.mangauniversal.app.data.model.Comic
import kotlinx.coroutines.flow.collectLatest

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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    var folders by remember { mutableStateOf<List<FolderConfig>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<FolderConfig?>(null) }
    var selectedComic by remember { mutableStateOf<Comic?>(null) }
    var comicsInFolder by remember { mutableStateOf<List<Comic>>(emptyList()) }

    // Cargar carpetas
    val folderManager = remember { FolderManager(context) }
    LaunchedEffect(Unit) {
        folderManager.folders.collectLatest { list ->
            folders = list
        }
    }

    // Comprobar permiso al volver de ajustes
    LaunchedEffect(currentScreen) {
        hasPermission = checkStoragePermission()
    }

    // Navegación
    when (val screen = currentScreen) {
        is Screen.Home -> {
            StorageExplorerScreen(
                favoriteFolders = folders.filter { it.isFavorite }.map { it.path },
                onOpenRecientes = {
                    if (selectedComic != null) {
                        currentScreen = Screen.Reader(selectedComic!!)
                    } else {
                        currentScreen = Screen.History
                    }
                },
                onOpenStorage = { path ->
                    if (!hasPermission) {
                        openAllFilesSettings(context.packageName, context)
                    } else {
                        currentScreen = Screen.FileBrowser(path)
                    }
                },
                onOpenFavoriteFolder = { path ->
                    val folder = folders.find { it.path == path }
                    if (folder != null) {
                        selectedFolder = folder
                        currentScreen = Screen.Library(folder)
                    } else {
                        // La carpeta ya no existe
                    }
                },
                onAddFolder = {
                    if (!hasPermission) {
                        openAllFilesSettings(context.packageName, context)
                    } else {
                        val basePath = Environment.getExternalStorageDirectory().absolutePath
                        currentScreen = Screen.FileBrowser(basePath)
                    }
                },
                onOpenHistory = {
                    currentScreen = Screen.History
                }
            )
        }

        is Screen.FileBrowser -> {
            FileBrowserScreen(
                startPath = screen.path,
                favoriteFolders = folders.map { it.path },
                onBack = {
                    currentScreen = Screen.Home
                },
                onToggleFavorite = { path ->
                    val existing = folders.find { it.path == path }
                    if (existing != null) {
                        // Ya existe: quitar de favoritos (mantener en carpetas)
                        // Por ahora, simplemente desmarcamos el favorito
                        // (no eliminamos del FolderManager)
                    } else {
                        // Añadir nueva carpeta como favorita
                        val name = java.io.File(path).name.ifEmpty { path }
                        // Necesitamos usar el scope para llamar a suspend
                        // Lo haremos con un LaunchedEffect controlado
                    }
                }
            )
        }

        is Screen.Library -> {
            LibraryScreen(
                folderPath = screen.folder.path,
                folderName = screen.folder.name,
                onBack = { currentScreen = Screen.Home },
                onComicClick = { comic, allComics ->
                    comicsInFolder = allComics
                    selectedComic = comic
                    currentScreen = Screen.Reader(comic)
                }
            )
        }

        is Screen.Reader -> {
            ReaderScreenWrapper(
                comic = screen.comic,
                comicsList = comicsInFolder.ifEmpty { listOf(screen.comic) },
                onBack = { currentScreen = Screen.Home },
                onOpenComic = { newComic ->
                    selectedComic = newComic
                    currentScreen = Screen.Reader(newComic)
                }
            )
        }

        is Screen.History -> {
            com.mangauniversal.app.ui.history.HistoryScreen(
                onBack = { currentScreen = Screen.Home },
                onOpenComic = { comic, list ->
                    comicsInFolder = list
                    selectedComic = comic
                    currentScreen = Screen.Reader(comic)
                }
            )
        }
    }
}

/**
 * Wrapper para cargar el ReaderScreen real.
 * Como ReaderScreen recibe `comic` y `comicsList`, se llama directo.
 */
@Composable
private fun ReaderScreenWrapper(
    comic: Comic,
    comicsList: List<Comic>,
    onBack: () -> Unit,
    onOpenComic: (Comic) -> Unit
) {
    com.mangauniversal.app.ui.reader.ReaderScreen(
        comic = comic,
        comicsList = comicsList,
        onBack = onBack,
        onOpenComic = onOpenComic
    )
}

/**
 * Pantallas de navegación.
 */
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
