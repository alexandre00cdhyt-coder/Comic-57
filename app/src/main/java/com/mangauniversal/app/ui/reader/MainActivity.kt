package com.mangauniversal.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
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

    // Estado de permiso — se re-comprueba cada vez que se vuelve a la app
    var hasPermission by remember { mutableStateOf(checkStoragePermission()) }
    var refreshKey by remember { mutableStateOf(0) }

    // Cada vez que se pulsa "Ya di el permiso", aumentamos refreshKey
    // y volvemos a comprobar. También cuando la app vuelve a primer plano.
    LaunchedEffect(refreshKey) {
        hasPermission = checkStoragePermission()
    }

    if (hasPermission) {
        LibraryPlaceholder()
    } else {
        PermissionScreen(
            onOpenSettings = {
                openAllFilesSettings(context.packageName, context)
            },
            onCheckAgain = {
                refreshKey++
            }
        )
    }
}

/**
 * Comprueba si la app tiene el permiso de acceso a todos los archivos.
 */
fun checkStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true // En versiones antiguas el permiso normal se pide de otra forma
    }
}

/**
 * Abre la pantalla de ajustes para conceder el permiso.
 */
fun openAllFilesSettings(packageName: String, context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: pantalla general de "todos los archivos"
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            context.startActivity(intent)
        }
    }
}

/**
 * Pantalla que explica al usuario cómo dar el permiso.
 */
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

/**
 * Placeholder temporal. Después será la pantalla de biblioteca.
 */
@Composable
fun LibraryPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✅ Permiso concedido",
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Biblioteca (próximamente)",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}
