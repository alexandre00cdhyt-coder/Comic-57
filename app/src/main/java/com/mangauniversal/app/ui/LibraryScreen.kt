package com.mangauniversal.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.scanner.ComicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pantalla que muestra los cómics de una carpeta concreta.
 */
@Composable
fun LibraryScreen(
    folderPath: String,
    folderName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var comics by remember { mutableStateOf<List<Comic>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Escanear la carpeta al entrar
    LaunchedEffect(folderPath) {
        isLoading = true
        comics = withContext(Dispatchers.IO) {
            ComicScanner(context).scanFolder(folderPath)
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
            .padding(12.dp)
    ) {
        // Barra superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) {
                Text("←", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folderPath,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF6B00))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Escaneando carpeta...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            comics == null || comics!!.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron cómics en esta carpeta",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                // Contador
                Text(
                    text = "✅ ${comics!!.size} cómics encontrados",
                    color = Color(0xFFFF6B00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lista de cómics con LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(comics!!, key = { it.id }) { comic ->
                        ComicRow(comic = comic)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicRow(comic: Comic) {
    val icon = when (comic.type) {
        ComicType.CBZ -> "📕"
        ComicType.CBR -> "📕"
        ComicType.ZIP -> "📦"
        ComicType.RAR -> "📦"
        ComicType.SEVEN_ZIP -> "📦"
        ComicType.PDF -> "📄"
        ComicType.FOLDER -> "📁"
        else -> "❓"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 26.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comic.title,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${comic.type} · ${formatSize(comic.sizeBytes)}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024L * 1024L -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024L * 1024L * 1024L)} GB"
    }
}
