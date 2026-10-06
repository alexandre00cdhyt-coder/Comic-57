@file:OptIn(ExperimentalFoundationApi::class)

package com.mangauniversal.app.ui.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    var fitMode by remember { mutableStateOf(ImageFitMode.FIT_WIDTH) }
    var readerMode by remember { mutableStateOf(ReaderMode.RIGHT) }
    var verticalZoom by remember { mutableFloatStateOf(1f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showFitDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        readerMode = prefs.getMode()
        verticalZoom = prefs.getVerticalZoom()
    }

    DisposableEffect(Unit) {
        val activity = view.context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            // Guardar zoom al salir
            scope.launch { prefs.saveVerticalZoom(verticalZoom) }
        }
    }

    BackHandler(enabled = true) {
        if (controlsVisible) {
            controlsVisible = false
        } else {
            onBack()
        }
    }

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
                    TextButton(onClick = onBack) { Text("Volver", color = Color.White) }
                }
            }
            pages != null -> {
                when (readerMode) {
                    ReaderMode.RIGHT -> ReaderHorizontal(
                        pages = pages!!,
                        fitMode = fitMode,
                        reverse = false,
                        onCenterTap = { controlsVisible = !controlsVisible }
                    )
                    ReaderMode.LEFT -> ReaderHorizontal(
                        pages = pages!!,
                        fitMode = fitMode,
                        reverse = true,
                        onCenterTap = { controlsVisible = !controlsVisible }
                    )
                    ReaderMode.VERTICAL -> ReaderVertical(
                        pages = pages!!,
                        zoom = verticalZoom,
                        onZoomChange = { newZoom ->
                            verticalZoom = newZoom
                        },
                        onCenterTap = { controlsVisible = !controlsVisible }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x80000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    fontSize = 22.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = comic.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "📖",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable { showModeDialog = true }
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = "⚙",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable { showFitDialog = true }
                        .padding(horizontal = 8.dp)
                )
            }
        }

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
                                        ReaderMode.RIGHT -> (if (readerMode == mode) "✓ " else "   ") + "→ Derecha"
                                        ReaderMode.LEFT -> (if (readerMode == mode) "✓ " else "   ") + "← Izquierda"
                                        ReaderMode.VERTICAL -> (if (readerMode == mode) "✓ " else "   ") + "↓ Vertical"
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
private fun ReaderHorizontal(
    pages: List<File>,
    fitMode: ImageFitMode,
    reverse: Boolean,
    onCenterTap: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

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
    }
}

/**
 * Modo Vertical continuo tipo CDisplayEx.
 * - Imágenes apiladas sin huecos.
 * - Pinch con 2 dedos para cambiar el tamaño (50% - 150%).
 * - Cuando el zoom > 100% aparece scroll horizontal.
 * - El zoom se guarda al salir.
 */
@Composable
private fun ReaderVertical(
    pages: List<File>,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onCenterTap: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) {
        configuration.screenWidthDp.dp.roundToPx()
    }
    val screenWidthDp = configuration.screenWidthDp.dp

    // scroll horizontal compartido (para cuando el zoom > 1)
    val horizontalScroll = rememberScrollState()

    // Zoom con pinch
    var internalZoom by remember { mutableFloatStateOf(zoom) }

    // Sincronizar si viene de fuera
    LaunchedEffect(zoom) {
        internalZoom = zoom
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pressed = event.changes.filter { it.pressed }
                        val pointerCount = pressed.size

                        if (pointerCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                val newZoom = (internalZoom * zoomChange)
                                    .coerceIn(
                                        ReaderPreferences.MIN_VERTICAL_ZOOM,
                                        ReaderPreferences.MAX_VERTICAL_ZOOM
                                    )
                                internalZoom = newZoom
                                onZoomChange(newZoom)
                                event.changes.forEach { if (it.pressed) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(pages.size) { index ->
                // Cada imagen se ajusta al ancho × zoom
                val imageWidthDp = screenWidthDp * internalZoom

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (internalZoom > 1f) {
                        // Zoom > 100%: scroll horizontal disponible
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScroll)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(pages[index])
                                    .size((screenWidthPx * internalZoom).toInt())
                                    .crossfade(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .width(imageWidthDp)
                                    .wrapContentHeight()
                            )
                        }
                    } else {
                        // Zoom <= 100%: imagen centrada en pantalla
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(pages[index])
                                .size(screenWidthPx)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .width(imageWidthDp)
                                .wrapContentHeight()
                                .clickable(onClick = onCenterTap)
                        )
                    }
                }
            }
        }
    }
}
