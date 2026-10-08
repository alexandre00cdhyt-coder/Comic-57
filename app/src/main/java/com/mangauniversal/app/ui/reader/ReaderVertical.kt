package com.mangauniversal.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@Composable
fun ReaderVertical(
    pages: List<File>,
    isLastComic: Boolean,
    pendingPage: Int,
    onPageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    onNextComic: () -> Unit,
    onPendingPageConsumed: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // Zoom local: 0.5x a 1.0x (sin scroll horizontal)
    var zoom by remember { mutableFloatStateOf(1f) }
    var lastNotifiedPage by remember { mutableIntStateOf(-1) }

    val listState = rememberLazyListState()

    // Notificar cambio de página
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val idx = listState.firstVisibleItemIndex
        if (idx != lastNotifiedPage) {
            lastNotifiedPage = idx
            onPageChanged(idx)
        }
    }

    // Saltar a pendingPage
    LaunchedEffect(pendingPage) {
        if (pendingPage >= 0 && pendingPage < pages.size && listState.firstVisibleItemIndex != pendingPage) {
            listState.scrollToItem(pendingPage)
            onPendingPageConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2) {
                                val zoomChange = event.calculateZoom()
                                if (zoomChange != 1f) {
                                    // Zoom solo entre 0.5x y 1.0x (sin salirse de pantalla)
                                    val newZoom = (zoom * zoomChange).coerceIn(0.5f, 1.0f)
                                    zoom = newZoom
                                    event.changes.forEach { if (it.pressed) it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(pages.size) { index ->
                // Ajustar el ANCHO de TODA la tira según el zoom.
                // Cada imagen ocupa `zoom` fracción del ancho total.
                val widthFraction = zoom.coerceIn(0.5f, 1.0f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(pages[index])
                            .memoryCacheKey(pages[index].absolutePath)
                            .diskCacheKey(pages[index].absolutePath)
                            .crossfade(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .wrapContentHeight()
                            .clickable(onClick = onCenterTap)
                    )
                }
            }
        }

        val isAtEnd = listState.firstVisibleItemIndex >= pages.size - 3
        if (isAtEnd && !isLastComic) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 60.dp, end = 20.dp)
                    .size(56.dp)
                    .background(Color(0xCCFF6B00), shape = CircleShape)
                    .clickable { onNextComic() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→",
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
