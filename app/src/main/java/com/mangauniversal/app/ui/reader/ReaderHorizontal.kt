@file:OptIn(ExperimentalFoundationApi::class)

package com.mangauniversal.app.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun ReaderHorizontal(
    pages: List<File>,
    fitMode: ImageFitMode,
    reverse: Boolean,
    initialPage: Int,
    pendingPage: Int,
    isLastComic: Boolean,
    onPageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    onNextComic: () -> Unit,
    onPendingPageConsumed: () -> Unit
) {
    if (pages.isEmpty()) return

    val safeInitial = if (initialPage < 0) 0 else initialPage.coerceIn(0, pages.size - 1)

    // El pager siempre va de 0..N-1.
    // Si reverse=true, el pager muestra el orden invertido, pero el índice
    // que notificamos (onPageChanged) y que recibimos (pendingPage) es el
    // índice REAL de la página (0..N-1), no el índice del pager.
    val pagerInitial = if (reverse) pages.size - 1 - safeInitial else safeInitial

    val pagerState = rememberPagerState(
        initialPage = pagerInitial,
        pageCount = { pages.size }
    )

    /**
     * Convierte índice del pager → índice real de página.
     */
    fun pagerToReal(pagerIndex: Int): Int {
        return if (reverse) pages.size - 1 - pagerIndex else pagerIndex
    }

    /**
     * Convierte índice real de página → índice del pager.
     */
    fun realToPager(realIndex: Int): Int {
        return if (reverse) pages.size - 1 - realIndex else realIndex
    }

    // Notificar cambio de página con el índice REAL
    LaunchedEffect(pagerState.settledPage) {
        onPageChanged(pagerToReal(pagerState.settledPage))
    }

    // Saltar a pendingPage (desde el slider)
    LaunchedEffect(pendingPage) {
        if (pendingPage >= 0 && pendingPage < pages.size) {
            val target = realToPager(pendingPage)
            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
            onPendingPageConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = false, // ← NO invertir el layout, invertimos el índice manualmente
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val realIdx = pagerToReal(page)
            ZoomablePage(
                file = pages[realIdx],
                pageKey = realIdx,
                contentScale = fitMode.contentScale,
                onTap = { zone ->
                    if (zone == TapZone.Center) onCenterTap()
                }
            )
        }

        // Botón "siguiente cómic" cuando estamos en la última página
        val currentRealPage = pagerToReal(pagerState.settledPage)
        val isLastPage = currentRealPage == pages.size - 1
        if (isLastPage && !isLastComic) {
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
