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
    isLastComic: Boolean,
    onPageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    onNextComic: () -> Unit
) {
    val safeInitial = if (initialPage < 0) 0 else initialPage.coerceIn(0, pages.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { pages.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

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

        val isLastPage = pagerState.currentPage == pages.size - 1
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
