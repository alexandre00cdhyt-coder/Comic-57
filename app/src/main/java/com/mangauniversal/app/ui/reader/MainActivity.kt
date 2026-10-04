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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.scanner.ComicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        LibraryScreen()
    } else {
        PermissionScreen(
            onOpenSettings = { openAllFilesSettings(context.packageName, context) },
            onCheckAgain = { refreshKey++ }
        )
    }
}

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

@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    var comics by remember { mutableStateOf<List<Comic>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        comics = withContext(Dispatchers.IO) {
            ComicScanner(context).scanAll()
        }
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                isLoading -> {
                    Text(
                        text = "Escaneando almacenamiento...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                comics == null -> {
                    Text(
                        text = "Error al escanear",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                }
                comics!!.isEmpty() -> {
                    Text(
                        text = "No se encontraron cómics",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prueba a copiar archivos CBZ, ZIP, RAR, 7Z, PDF o carpetas con imágenes al teléfono",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        text = "✅ Encontrados ${comics!!.size} cómics",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    comics!!.take(30).forEach { comic ->
                        Text(
                            text = "• ${comic.title}  [${comic.type}]",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }

                    if (comics!!.size > 30) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "... y ${comics!!.size - 30} más",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
