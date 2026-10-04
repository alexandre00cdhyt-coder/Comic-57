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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangauniversal.app.data.folder.FolderConfig
import com.mangauniversal.app.data.folder.FolderManager
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
        // Ya tiene permiso → mostrar flujo de carpetas
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

    // Escuchar cambios en las carpetas guardadas
    LaunchedEffect(Unit) {
        folderManager.folders.collectLatest { list ->
            folders = list
        }
    }

    when {
        folders == null -> {
            // Cargando
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cargando...",
                    color = Color.White
                )
            }
        }
        showPicker -> {
            // Pantalla de elegir carpeta
            FolderPickerScreen(
                onFolderSelected = { path, name ->
                    scope.launch {
                        folderManager.addFolder(path, name)
                        showPicker = false
                    }
                },
                onCancel = {
                    showPicker = false
                }
            )
        }
        folders!!.isEmpty() -> {
            // No hay carpetas → botón para añadir
            NoFoldersScreen(
                onAddFolder = { showPicker = true }
            )
        }
        else -> {
            // Hay carpetas → placeholder (la biblioteca se hará en 1.7)
            FoldersListScreen(
                folders = folders!!,
                onAddFolder = { showPicker = true },
                onRemoveFolder = { path ->
                    scope.launch {
                        folderManager.removeFolder(path)
                    }
                }
            )
        }
    }
}

@Composable
fun NoFoldersScreen(onAddFolder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No tienes carpetas configuradas",
            color = Color.White,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Elige una carpeta donde tengas tus cómics.\nLa app la escaneará y encontrará automáticamente CBZ, ZIP, RAR, PDF y carpetas con imágenes.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Añadir carpeta de cómics")
        }
    }
}

@Composable
fun FoldersListScreen(
    folders: List<FolderConfig>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "📚 Carpetas configuradas",
            color = Color.White,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        folders.forEach { folder ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "📁 ${folder.name}",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = folder.path,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onRemoveFolder(folder.path) }
                ) {
                    Text("Eliminar")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Añadir otra carpeta")
        }
    }
}

// =====================================================
// Pantalla de permisos (ya existente)
// =====================================================

fun checkStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
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
fun PermissionScreen(
    onOpenSettings: () -> Unit,
    onCheckAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Acceso a archivos",
            color = Color.White,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Para escanear tus cómics, la app necesita acceso a todos los archivos.\n\n" +
                    "1. Toca \"Abrir ajustes\"\n" +
                    "2. Activa \"Permitir acceso a todos los archivos\"\n" +
                    "3. Vuelve a la app y toca \"Ya di el permiso\"",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Abrir ajustes")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCheckAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ya di el permiso")
        }
    }
}
