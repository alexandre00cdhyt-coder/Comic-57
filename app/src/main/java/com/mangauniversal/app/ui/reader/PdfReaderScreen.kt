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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mangauniversal.app.data.reader.PageExtractor
import com.mangauniversal.app.data.settings.ReaderMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfReaderScreen(
    pdfFile: File,
    pageCount: Int,
    extractor: PageExtractor,
    fitMode: ImageFitMode,
    readerMode: ReaderMode,
    initialPage: Int,
    pendingPage: Int,
    isLastComic: Boolean,
    onPageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    onNextComic: () -> Unit,
    onPendingPageConsumed: () -> Unit
) {
    if (pageCount <= 0) return

    val safeInitial = if (initialPage < 0) 0 else initialPage.coerceIn(0, pageCount - 1)
    val reverse = readerMode == ReaderMode.LEFT
    val pagerInitial = if (reverse) pageCount - 1 - safeInitial else safeInitial

    val pagerState = rememberPagerState(
        initialPage = pagerInitial,
        pageCount = { pageCount }
    )

    fun pagerToReal(pagerIndex: Int): Int {
        return if (reverse) pageCount - 1 - pagerIndex else pagerIndex
    }

    fun realToPager(realIndex: Int): Int {
        return if (reverse) pageCount - 1 - realIndex else realIndex
    }

    LaunchedEffect(pagerState.settledPage) {
        onPageChanged(pagerToReal(pagerState.settledPage))
    }

    LaunchedEffect(pendingPage) {
        if (pendingPage >= 0 && pendingPage < pageCount) {
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
            reverseLayout = false,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val realIdx = pagerToReal(page)
            PdfPage(
                pdfFile = pdfFile,
                pageIndex = realIdx,
                extractor = extractor,
                contentScale = fitMode.contentScale,
                onTap = onCenterTap
            )
        }

        val currentRealPage = pagerToReal(pagerState.settledPage)
        val isLastPage = currentRealPage == pageCount - 1
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

@Composable
private fun PdfPage(
    pdfFile: File,
    pageIndex: Int,
    extractor: PageExtractor,
    contentScale: ContentScale,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    var renderedFile by remember(pageIndex) { mutableStateOf<File?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }

    LaunchedEffect(pageIndex) {
        isLoading = true
        val cacheDir = File(context.cacheDir, "pdf_rendered/${pdfFile.name.hashCode()}")
        cacheDir.mkdirs()
        
        val file = withContext(Dispatchers.IO) {
            extractor.renderPdfPage(pdfFile, pageIndex, cacheDir)
        }
        renderedFile = file
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFFFF6B00))
        } else if (renderedFile != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(renderedFile)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "Error al cargar página ${pageIndex + 1}",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}
