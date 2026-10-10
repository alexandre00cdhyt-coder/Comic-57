package com.mangauniversal.app.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Ajustes de imagen aplicables al lector.
 * Valores por defecto = sin cambios.
 */
data class ImageAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val temperature: Float = 0f,
    val warmth: Float = 0f,
    val blueLightFilter: Float = 0f,
)

/**
 * Convierte los ajustes a una ColorMatrix para usar en AsyncImage o graphicsLayer.
 */
fun ImageAdjustments.toColorMatrix(): ColorMatrix {
    val matrix = ColorMatrix()
    matrix.setToSaturation(saturation.coerceIn(0f, 2f))

    // Contraste y brillo
    val c = contrast.coerceIn(0.5f, 1.5f)
    val t = (1f - c) * 0.5f + brightness.coerceIn(-0.5f, 0.5f)

    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, t * 255f,
            0f, c, 0f, 0f, t * 255f,
            0f, 0f, c, 0f, t * 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    matrix.timesAssign(contrastMatrix)

    // Temperatura (frío/cálido) — rojo vs azul
    val temp = temperature.coerceIn(-1f, 1f)
    val warm = warmth.coerceIn(0f, 1f)
    val redBoost = (temp + warm * 0.5f) * 30f
    val blueBoost = (-temp + blueLightFilter * -80f) * 1f

    val tempMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, redBoost,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, blueBoost,
            0f, 0f, 0f, 1f, 0f
        )
    )
    matrix.timesAssign(tempMatrix)

    return matrix
}

/**
 * Overlay para atenuar la imagen según el brillo.
 */
fun brightnessOverlay(brightness: Float): Color {
    return if (brightness < 0f) {
        Color.Black.copy(alpha = (-brightness).coerceIn(0f, 0.6f))
    } else {
        Color.White.copy(alpha = brightness.coerceIn(0f, 0.4f))
    }
}
