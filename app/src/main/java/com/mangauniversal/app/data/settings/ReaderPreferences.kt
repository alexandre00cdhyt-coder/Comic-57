@file:OptIn(ExperimentalFoundationApi::class)

package com.mangauniversal.app.ui.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.reader.PageExtractor
import com.mangauniversal.app.data.settings.ReaderMode
import com.mangauniversal.app.data.settings.ReaderPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ImageFitMode(val label: String, val contentScale: ContentScale) {
    FIT("Ajustar", ContentScale.Fit),
    FILL("Llenar", ContentScale.Crop),
    FIT_WIDTH("Ancho", ContentScale.FillWidth),
    FIT_HEIGHT("Alto", ContentScale.FillHeight),
}

@Composable
fun ReaderScreen(
    comic: Comic,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ReaderPreferences(context) }

    var pages by remember { mutableStateOf<List<File>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fitMode by remember { mutableStateOf(ImageFitMode.FIT) }
    var readerMode by remember { mutableStateOf(ReaderMode.RIGHT) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showFitDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }

    // Auto-ocultar controles
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Cargar modo guardado
    LaunchedEffect(Unit) {
        readerMode = prefs.getMode()
    }

    // Modo inmersivo
    DisposableEffect(Unit) {
        val activity = view.context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Botón atrás físico
    BackHandler(enabled = true) {
        if (controlsVisible) {
            controlsVisible = false
        } else {
            onBack()
        }
    }

    // Cargar páginas
    LaunchedEffect(comic.id) {
        isLoading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                PageExtractor(context).getPages(comic)
            }
            if (result.isEmpty()) {
                errorMessage = "No se pudieron cargar las páginas"
            } else {
                pages = result
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFF6B00))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando páginas...", color = Color.White, fontSize = 13.sp)
                }
            }
            errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage!!, color = Color(0xFFFF5555), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Volver", fontSize = 12.sp) }
                }
            }
            pages != null -> {
                when (readerMode) {
                    ReaderMode.RIGHT -> ReaderHorizontal(pages = pages!!, fitMode = fitMode, reverse = false, onCenterTap = { controlsVisible = !controlsVisible })
                    ReaderMode.LEFT -> ReaderHorizontal(pages = pages!!, fitMode = fitMode, reverse = true, onCenterTap = { controlsVisible = !controlsVisible })
                    ReaderMode.VERTICAL -> ReaderVertical(pages = pages!!, fitMode = fitMode, onCenterTap = { controlsVisible = !controlsVisible })
                }
            }
        }

        // Barra superior
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EAA)),
                    modifier = Modifier.width(50.dp)
                ) {
                    Text("←", fontSize = 16.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comic.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (readerMode) {
                            ReaderMode.RIGHT -> "→ Derecha"
                            ReaderMode.LEFT -> "← Izquierda"
                            ReaderMode.VERTICAL -> "↓ Vertical"
                        },
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
                Button(
                    onClick = { showModeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier = Modifier.width(50.dp)
                ) {
                    Text("📖", fontSize = 16.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = { showFitDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier = Modifier.width(50.dp)
                ) {
                    Text("⚙", fontSize = 16.sp, color = Color.White)
                }
            }
        }

        // Diálogo: modo de lectura
        if (showModeDialog) {
            AlertDialog(
                onDismissRequest = { showModeDialog = false },
                title = { Text("Modo de lectura", color = Color.White, fontSize = 15.sp) },
                text = {
                    Column {
                        ReaderMode.values().forEach { mode ->
                            TextButton(
                                onClick = {
                                    readerMode = mode
                                    scope.launch { prefs.saveMode(mode) }
                                    showModeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when (mode) {
                                        ReaderMode.RIGHT -> (if (readerMode == mode) "✓ " else "   ") + "→ Derecha (occidental)"
                                        ReaderMode.LEFT -> (if (readerMode == mode) "✓ " else "   ") + "← Izquierda (manga)"
                                        ReaderMode.VERTICAL -> (if (readerMode == mode) "✓ " else "   ") + "↓ Vertical (tira larga)"
                                    },
                                    color = if (readerMode == mode) Color(0xFFFF6B00) else Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModeDialog = false }) {
                        Text("Cerrar", color = Color.White, fontSize = 13.sp)
                    }
                },
                containerColor = Color(0xFF1A1A1A)
            )
        }

        // Diálogo: ajuste de imagen
        if (showFitDialog) {
            AlertDialog(
                onDismissRequest = { showFitDialog = false },
                title = { Text("Ajuste de imagen", color = Color.White, fontSize = 15.sp) },
                text = {
                    Column {
                        ImageFitMode.values().forEach { mode ->
                            TextButton(
                                onClick = {
                                    fitMode = mode
                                    showFitDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = (if (fitMode == mode) "✓ " else "   ") + mode.label,
                                    color = if (fitMode == mode) Color(0xFFFF6B00) else Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFitDialog = false }) {
                        Text("Cerrar", color = Color.White, fontSize = 13.sp)
                    }
                },
                containerColor = Color(0xFF1A1A1A)
            )
        }
    }
}

/**
 * Lector horizontal (página a página).
 * reverse = false → derecha (occidental)
 * reverse = true → izquierda (manga)
 */
@Composable
private fun ReaderHorizontal(
    pages: List<File>,
    fitMode: ImageFitMode,
    reverse: Boolean,
    onCenterTap: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val actualPage = if (reverse) pages.size - 1 - pagerState.currentPage else pagerState.currentPage

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = reverse,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val idx = if (reverse) pages.size - 1 - page else page
            ZoomablePage(
                file = pages[idx],
                pageKey = idx,
                contentScale = fitMode.contentScale,
                onTap = { zone ->
                    if (zone == TapZone.Center) onCenterTap()
                }
            )
        }

        // Indicador
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${actualPage + 1} / ${pages.size}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Lector vertical (tira larga continua).
 */
@Composable
private fun ReaderVertical(
    pages: List<File>,
    fitMode: ImageFitMode,
    onCenterTap: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(pages.size) { index ->
                ZoomablePage(
                    file = pages[index],
                    pageKey = index,
                    contentScale = fitMode.contentScale,
                    onTap = { zone ->
                        if (zone == TapZone.Center) onCenterTap()
                    }
                )
            }
        }
    }
}
