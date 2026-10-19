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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.reader.PageExtractor
import com.mangauniversal.app.data.settings.Bookmark
import com.mangauniversal.app.data.settings.BookmarkManager
import com.mangauniversal.app.data.settings.ProgressSaver
import com.mangauniversal.app.data.settings.ReaderMode
import com.mangauniversal.app.data.settings.ReaderPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ReaderScreen(
    comic: Comic,
    comicsList: List<Comic>,
    initialPageOverride: Int = -1,
    onBack: () -> Unit,
    onOpenComic: (Comic) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ReaderPreferences(context) }
    val bookmarkManager = remember { BookmarkManager(context) }
    val pageExtractor = remember { PageExtractor(context) }

    var pages by remember { mutableStateOf<List<File>?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) } // Para PDFs
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fitMode by remember { mutableStateOf(ImageFitMode.FIT_WIDTH) }
    var readerMode by remember { mutableStateOf(ReaderMode.RIGHT) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showFitDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNote by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    var pendingPage by remember { mutableIntStateOf(-1) }
    var initialPage by remember { mutableIntStateOf(-1) }
    var pagesLoaded by remember { mutableStateOf(false) }

    var isSliding by remember { mutableStateOf(false) }
    var sliderPage by remember { mutableIntStateOf(0) }

    val currentIndex = comicsList.indexOfFirst { it.id == comic.id }
    val nextComic = if (currentIndex >= 0 && currentIndex < comicsList.size - 1) {
        comicsList[currentIndex + 1]
    } else null

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        readerMode = prefs.getMode()
        if (initialPageOverride >= 0) {
            initialPage = initialPageOverride
            pendingPage = initialPageOverride
            currentPage = initialPageOverride
        } else {
            val saved = prefs.getProgress(comic.id)
            if (saved > 0) {
                initialPage = saved
                pendingPage = saved
                currentPage = saved
            }
        }
    }

    LaunchedEffect(comic.id) {
        ProgressSaver.addToHistory(context, comic.id, comic.title, comic.path)
    }

    DisposableEffect(comic.id) {
        val activity = view.context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            ProgressSaver.saveProgress(context, comic.id, currentPage)
        }
    }

    BackHandler(enabled = true) {
        if (controlsVisible) {
            controlsVisible = false
        } else {
            ProgressSaver.saveProgress(context, comic.id, currentPage)
            onBack()
        }
    }

    LaunchedEffect(comic.id) {
        isLoading = true
        errorMessage = null
        try {
            if (comic.type == ComicType.PDF) {
                val file = File(comic.path)
                pdfFile = file
                val count = pageExtractor.getPdfPageCount(file)
                if (count > 0) {
                    // Generamos una lista ficticia con la cantidad de páginas
                    pages = List(count) { File(comic.path) }
                    pagesLoaded = true
                } else {
                    errorMessage = "PDF vacío o corrupto"
                }
            } else {
                val result = withContext(Dispatchers.IO) {
                    pageExtractor.getPages(comic)
                }
                if (result.isEmpty()) {
                    errorMessage = "No se pudieron cargar las páginas"
                } else {
                    pages = result
                    pagesLoaded = true
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        }
        isLoading = false
    }

    LaunchedEffect(currentPage) {
        if (pagesLoaded && pages != null && pages!!.isNotEmpty()) {
            ProgressSaver.saveProgress(context, comic.id, currentPage)
        }
    }

    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = {
                Text("Añadir marcador", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Página ${currentPage + 1}",
                        color = Color(0xFFFF6B00),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(10.dp))
                    TextField(
                        value = bookmarkNote,
                        onValueChange = { bookmarkNote = it },
                        placeholder = {
                            Text("Nota (opcional)", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFFFF6B00),
                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFFFF6B00)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val note = bookmarkNote
                    bookmarkNote = ""
                    showBookmarkDialog = false
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            bookmarkManager.add(
                                Bookmark(
                                    comicId = comic.id,
                                    comicTitle = comic.title,
                                    comicPath = comic.path,
                                    page = currentPage,
                                    note = note,
                                    addedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }) {
                    Text("Guardar", color = Color(0xFFFF6B00), fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    bookmarkNote = ""
                    showBookmarkDialog = false
                }) {
                    Text("Cancelar", color = Color.White, fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
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
                    TextButton(onClick = onBack) {
                        Text("Volver", color = Color.White)
                    }
                }
            }
            pages != null -> {
                if (comic.type == ComicType.PDF && pdfFile != null) {
                    // LECTOR DE PDF NATIVO (Al vuelo)
                    PdfReaderScreen(
                        pdfFile = pdfFile!!,
                        pageCount = pages!!.size,
                        extractor = pageExtractor,
                        fitMode = fitMode,
                        readerMode = readerMode,
                        initialPage = initialPage,
                        pendingPage = pendingPage,
                        isLastComic = nextComic == null,
                        onPageChanged = { currentPage = it },
                        onCenterTap = { controlsVisible = !controlsVisible },
                        onNextComic = {
                            ProgressSaver.saveProgress(context, comic.id, currentPage)
                            nextComic?.let { onOpenComic(it) }
                        },
                        onPendingPageConsumed = { pendingPage = -1 }
                    )
                } else {
                    when (readerMode) {
                        ReaderMode.RIGHT, ReaderMode.LEFT -> ReaderHorizontal(
                            pages = pages!!,
                            fitMode = fitMode,
                            reverse = readerMode == ReaderMode.LEFT,
                            initialPage = initialPage,
                            pendingPage = pendingPage,
                            isLastComic = nextComic == null,
                            onPageChanged = { currentPage = it },
                            onCenterTap = { controlsVisible = !controlsVisible },
                            onNextComic = {
                                ProgressSaver.saveProgress(context, comic.id, currentPage)
                                nextComic?.let { onOpenComic(it) }
                            },
                            onPendingPageConsumed = { pendingPage = -1 }
                        )
                        ReaderMode.VERTICAL -> ReaderVertical(
                            pages = pages!!,
                            isLastComic = nextComic == null,
                            pendingPage = pendingPage,
                            onPageChanged = { currentPage = it },
                            onCenterTap = { controlsVisible = !controlsVisible },
                            onNextComic = {
                                ProgressSaver.saveProgress(context, comic.id, currentPage)
                                nextComic?.let { onOpenComic(it) }
                            },
                            onPendingPageConsumed = { pendingPage = -1 }
                        )
                    }
                }
            }
        }

        if (isSliding && pages != null && sliderPage in pages!!.indices) {
            // La previsualización del slider en PDFs puede ser costosa, así que la mostramos simple.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(220.dp, 320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE000000))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (comic.type == ComicType.PDF && pdfFile != null) {
                    Text("Página ${sliderPage + 1}", color = Color.White, fontSize = 20.sp)
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(pages!![sliderPage])
                            .memoryCacheKey(pages!![sliderPage].absolutePath)
                            .diskCacheKey(pages!![sliderPage].absolutePath)
                            .crossfade(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
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
                        .clickable {
                            ProgressSaver.saveProgress(context, comic.id, currentPage)
                            onBack()
                        }
                        .padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comic.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Añadir marcador",
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { showBookmarkDialog = true }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "<#",
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            pendingPage = 0
                            currentPage = 0
                        }
                        .padding(horizontal = 6.dp)
                )
                Text(
                    text = "#>",
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            pages?.let {
                                pendingPage = it.size - 1
                                currentPage = it.size - 1
                            }
                        }
                        .padding(horizontal = 6.dp)
                )
                Text(
                    text = "📖",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable { showModeDialog = true }
                        .padding(horizontal = 6.dp)
                )
                Text(
                    text = "⚙",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable { showFitDialog = true }
                        .padding(horizontal = 6.dp)
                )
            }
        }

        if (pages != null && controlsVisible) {
            val totalPages = pages!!.size
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0x80000000))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = if (isSliding) sliderPage.toFloat() else currentPage.toFloat(),
                    onValueChange = { newValue ->
                        isSliding = true
                        sliderPage = newValue.toInt()
                                            isSliding = false
                    },
                    valueRange = 0f..(totalPages - 1).toFloat().coerceAtLeast(1f),
                    modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF6B00),
                        activeTrackColor = Color(0xFFFF6B00),
                        inactiveTrackColor = Color(0x55FFFFFF)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSliding) "${sliderPage + 1} / $totalPages" else "${currentPage + 1} / $totalPages",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
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
