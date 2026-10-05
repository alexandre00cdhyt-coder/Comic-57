@file:OptIn(ExperimentalFoundationApi::class)

package com.mangauniversal.app.ui.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Modos de ajuste de imagen.
 */
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

    var pages by remember { mutableStateOf<List<File>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fitMode by remember { mutableStateOf(ImageFitMode.FIT) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showFitDialog by remember { mutableStateOf(false) }

    // ─── Auto-ocultar controles tras 3 segundos ───
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    // ─── Modo inmersivo: ocultar barras del sistema ───
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

    // ─── Botón atrás físico ───
    BackHandler(enabled = true) {
        if (controlsVisible) {
            controlsVisible = false
        } else {
            onBack()
        }
    }

    // ─── Cargar páginas ───
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

        // ─── CONTENIDO ───
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
            pages != null -> ReaderPager(
                pages = pages!!,
                fitMode = fitMode,
                onCenterTap = { controlsVisible = !controlsVisible }
            )
        }

        // ─── BARRA SUPERIOR (auto-ocultar) ───
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
                Spacer(modifier = Modifier.width(10.dp))
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
                        text = comic.type.name,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
                Button(
                    onClick = { showFitDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier = Modifier.width(50.dp)
                ) {
                    Text("⚙", fontSize = 16.sp, color = Color.White)
                }
            }
        }

        // ─── DIÁLOGO DE AJUSTE ───
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

@Composable
private fun ReaderPager(
    pages: List<File>,
    fitMode: ImageFitMode,
    onCenterTap: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomablePage(
                file = pages[page],
                pageKey = page,
                contentScale = fitMode.contentScale,
                onTap = { zone ->
                    if (zone == TapZone.Center) {
                        onCenterTap()
                    }
                }
            )
        }
    }
}
