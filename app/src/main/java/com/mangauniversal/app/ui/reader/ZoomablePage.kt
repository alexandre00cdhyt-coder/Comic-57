package com.mangauniversal.app.ui.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.launch

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

@Composable
fun ZoomablePage(
    file: File?,
    pageKey: Any,
    onTap: (TapZone) -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember(pageKey) { Animatable(MIN_SCALE) }
    val offsetAnim = remember(pageKey) {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val targetWidthPx = with(density) { config.screenWidthDp.dp.roundToPx() }

    fun clampOffset(candidate: Offset, currentScale: Float): Offset {
        if (containerSize == IntSize.Zero || imageSize == Size.Zero) return candidate
        val scaledW = imageSize.width * currentScale
        val scaledH = imageSize.height * currentScale
        val maxX = max(0f, (scaledW - containerSize.width) / 2f)
        val maxY = max(0f, (scaledH - containerSize.height) / 2f)
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(pageKey) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pressed = event.changes.filter { it.pressed }
                        val pointerCount = pressed.size

                        if (pointerCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)

                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val currentScale = scaleAnim.value
                                val newScale = (currentScale * zoomChange)
                                    .coerceIn(MIN_SCALE, MAX_SCALE)
                                val actualZoom = newScale / currentScale
                                val adjusted = (offsetAnim.value - centroid) * actualZoom +
                                        centroid + panChange

                                scope.launch {
                                    scaleAnim.snapTo(newScale)
                                    offsetAnim.snapTo(
                                        if (newScale <= MIN_SCALE) Offset.Zero
                                        else clampOffset(adjusted, newScale)
                                    )
                                }

                                event.changes.forEach { if (it.pressed) it.consume() }
                            }
                        } else if (pointerCount == 1 && scaleAnim.value > MIN_SCALE) {
                            val change = pressed.first()
                            val delta = change.positionChange()
                            if (abs(delta.x) > 0f || abs(delta.y) > 0f) {
                                scope.launch {
                                    offsetAnim.snapTo(
                                        clampOffset(offsetAnim.value + delta, scaleAnim.value)
                                    )
                                }
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(pageKey) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scaleAnim.value > MIN_SCALE + 0.01f) {
                            scope.launch {
                                launch {
                                    scaleAnim.animateTo(
                                        MIN_SCALE,
                                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                                    )
                                }
                                offsetAnim.animateTo(
                                    Offset.Zero,
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                )
                            }
                        } else {
                            val newScale = DOUBLE_TAP_SCALE
                            val adjusted = (offsetAnim.value - tapOffset) *
                                    (newScale / scaleAnim.value) + tapOffset
                            val clamped = clampOffset(adjusted, newScale)

                            scope.launch {
                                launch {
                                    scaleAnim.animateTo(
                                        newScale,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                                offsetAnim.animateTo(
                                    clamped,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    },
                    onTap = { tapOffset ->
                        if (scaleAnim.value <= MIN_SCALE + 0.01f) {
                            val width = size.width
                            val zone = when {
                                tapOffset.x < width * 0.3f -> TapZone.Left
                                tapOffset.x > width * 0.7f -> TapZone.Right
                                else -> TapZone.Center
                            }
                            onTap(zone)
                        }
                    }
                )
            }
    ) {
        if (file == null) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file)
                    .size(targetWidthPx * 2)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    imageSize = Size(
                        state.result.drawable.intrinsicWidth.toFloat(),
                        state.result.drawable.intrinsicHeight.toFloat()
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scaleAnim.value,
                        scaleY = scaleAnim.value,
                        translationX = offsetAnim.value.x,
                        translationY = offsetAnim.value.y
                    )
            )
        }
    }
}

enum class TapZone { Left, Center, Right }
