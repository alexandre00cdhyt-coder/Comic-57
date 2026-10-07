package com.mangauniversal.app.ui.reader

import androidx.compose.ui.layout.ContentScale

enum class ImageFitMode(val label: String, val contentScale: ContentScale) {
    FIT("Ajustar", ContentScale.Fit),
    FILL("Llenar", ContentScale.Crop),
    FIT_WIDTH("Ancho", ContentScale.FillWidth),
    FIT_HEIGHT("Alto", ContentScale.FillHeight),
}
