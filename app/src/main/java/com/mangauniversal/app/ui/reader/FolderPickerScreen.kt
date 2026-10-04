package com.mangauniversal.app.ui

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Pantalla que permite al usuario navegar por el almacenamiento
 * y elegir una carpeta que contenga cómics.
 *
 * @param onFolderSelected Callback con la ruta absoluta y el nombre elegido.
 */
@Composable
fun FolderPickerScreen(
    onFolderSelected: (path: String, name: String) -> Unit,
    onCancel: () -> Unit
) {
    // Empezamos desde la raíz del almacenamiento interno
    val rootPath = Environment.getExternalStorageDirectory().absolutePath
    var currentPath by remember { mutableStateOf(rootPath) }

    val currentDir = remember(currentPath) { File(currentPath) }
    val subDirs = remember(currentPath) {
        (currentDir.listFiles() ?: emptyArray())
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título
        Text(
            text = "Elegir carpeta de cómics",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Ruta actual
        Text(
            text = currentPath,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Botones de arriba
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentPath != rootPath) {
                Button(
                    onClick = {
                        currentDir.parentFile?.let { currentPath = it.absolutePath }
                    }
                ) {
                    Text("← Atrás")
                }
            }
            Button(
                onClick = {
                    onFolderSelected(currentPath, currentDir.name)
                }
            ) {
                Text("✅ Elegir esta carpeta")
            }
            Button(
                onClick = onCancel
            ) {
                Text("✖ Cancelar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lista de subcarpetas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (subDirs.isEmpty()) {
                Text(
                    text = "No hay subcarpetas aquí.\nPuedes elegir esta carpeta con el botón de arriba.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(subDirs) { dir ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentPath = dir.absolutePath }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📁",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.padding(8.dp))
                            Text(
                                text = dir.name,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
