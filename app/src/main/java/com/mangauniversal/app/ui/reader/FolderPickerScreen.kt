package com.mangauniversal.app.ui

import android.os.Environment
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Explorador de carpetas completo.
 * - Muestra las raíces de almacenamiento disponibles.
 * - Permite navegar por cualquier carpeta.
 * - Botón para marcar carpeta actual como favorita.
 */
@Composable
fun FolderPickerScreen(
    onFolderSelected: (path: String, name: String) -> Unit,
    onCancel: () -> Unit
) {
    // Detectar todas las raíces de almacenamiento disponibles
    val storageRoots = remember { getStorageRoots() }

    var currentPath by remember { mutableStateOf<String?>(null) }

    // Si currentPath es null → mostramos pantalla de "raíces"
    // Si no → mostramos el explorador de esa carpeta
    if (currentPath == null) {
        StorageRootsScreen(
            roots = storageRoots,
            onRootSelected = { currentPath = it },
            onCancel = onCancel
        )
    } else {
        FileBrowserScreen(
            currentPath = currentPath!!,
            storageRoots = storageRoots,
            onNavigate = { currentPath = it },
            onSelectCurrent = {
                val dir = File(currentPath!!)
                onFolderSelected(currentPath!!, dir.name)
            },
            onBack = {
                val dir = File(currentPath!!)
                val parent = dir.parentFile
                // Si estamos en una raíz, volvemos a la pantalla de raíces
                if (parent == null || storageRoots.any { it == dir.absolutePath }) {
                    currentPath = null
                } else {
                    currentPath = parent.absolutePath
                }
            }
        )
    }
}

// -----------------------------------------------------------------
// PANTALLA 1: Lista de raíces de almacenamiento
// -----------------------------------------------------------------

@Composable
private fun StorageRootsScreen(
    roots: List<String>,
    onRootSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
            .padding(16.dp)
    ) {
        Text(
            text = "📂 Añadir carpeta de cómics",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Elige la ubicación de almacenamiento donde están tus cómics:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(roots, key = { it }) { root ->
                StorageRootRow(
                    path = root,
                    onClick = { onRootSelected(root) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✖ Cancelar")
        }
    }
}

@Composable
private fun StorageRootRow(
    path: String,
    onClick: () -> Unit
) {
    val dir = remember(path) { File(path) }
    val name = remember(path) {
        when {
            path.contains("emulated/0") -> "📱 Almacenamiento interno"
            path.contains("emulated") -> "📱 Almacenamiento emulado"
            else -> "💾 ${dir.name} (memoria externa / SD)"
        }
    }
    val isReadable = remember(path) { dir.canRead() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isReadable, onClick = onClick)
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = if (isReadable) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = path,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!isReadable) {
                Text(
                    text = "⛔ No accesible",
                    color = Color(0xFFFF5555),
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = "›",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 24.sp
        )
    }
}

// -----------------------------------------------------------------
// PANTALLA 2: Explorador de archivos
// -----------------------------------------------------------------

@Composable
private fun FileBrowserScreen(
    currentPath: String,
    storageRoots: List<String>,
    onNavigate: (String) -> Unit,
    onSelectCurrent: () -> Unit,
    onBack: () -> Unit
) {
    val currentDir = remember(currentPath) { File(currentPath) }

    val subDirs = remember(currentPath) {
        try {
            val files = currentDir.listFiles()
            if (files == null) emptyList()
            else files
                .filter { it.isDirectory && !it.name.startsWith(".") && it.canRead() }
                .sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Contar cuántas imágenes/cómics hay aquí
    val comicCountHere = remember(currentPath) {
        try {
            val files = currentDir.listFiles() ?: emptyArray()
            files.count { file ->
                if (file.isDirectory) false
                else {
                    val ext = file.name.substringAfterLast('.', "").lowercase()
                    ext in setOf("cbz", "cbr", "zip", "rar", "7z", "pdf")
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
            .padding(16.dp)
    ) {
        // Ruta actual
        Text(
            text = currentPath,
            color = Color(0xFFFF6B00),
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Info: cuántos cómics hay aquí
        Text(
            text = "Esta carpeta contiene $comicCountHere cómics",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Botonera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("← Atrás", fontSize = 11.sp)
            }
            Button(
                onClick = onSelectCurrent,
                modifier = Modifier.weight(1.5f)
            ) {
                Text("⭐ Marcar como favorita", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de subcarpetas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (subDirs.isEmpty()) {
                Text(
                    text = "No hay subcarpetas aquí.\n\n" +
                            "Si esta carpeta tiene cómics, márcala con \"⭐ Marcar como favorita\".",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(subDirs, key = { it.absolutePath }) { dir ->
                        FolderRow(
                            dir = dir,
                            onClick = { onNavigate(dir.absolutePath) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    dir: File,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "📁", fontSize = 22.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dir.name,
                color = Color.White,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val childCount = remember(dir.absolutePath) {
                try {
                    dir.listFiles()?.size ?: 0
                } catch (e: Exception) {
                    0
                }
            }
            if (childCount > 0) {
                Text(
                    text = "$childCount elementos",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = "›",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 22.sp
        )
    }
}

// -----------------------------------------------------------------
// Detectar raíces de almacenamiento
// -----------------------------------------------------------------

/**
 * Devuelve las rutas de almacenamiento disponibles:
 *  - Interna principal
 *  - MicroSD / memoria externa (si está montada)
 */
private fun getStorageRoots(): List<String> {
    val roots = mutableListOf<String>()

    // Almacenamiento interno principal
    val primary = Environment.getExternalStorageDirectory()
    if (primary != null && primary.exists() && primary.canRead()) {
        roots.add(primary.absolutePath)
    }

    // MicroSD / almacenamiento externo
    val storageDir = File("/storage")
    if (storageDir.exists() && storageDir.isDirectory) {
        storageDir.listFiles()?.forEach { file ->
            if (file.isDirectory &&
                file.canRead() &&
                file.name != "emulated" &&
                file.name != "self"
            ) {
                if (!roots.contains(file.absolutePath)) {
                    roots.add(file.absolutePath)
                }
            }
        }
    }

    return roots
}
