package com.mangauniversal.app.ui

enum class ViewMode {
    /** Lista con miniatura mediana y detalles (tamaño, fecha) */
    LIST_THUMB,

    /** Lista compacta: solo icono + nombre */
    LIST_COMPACT,

    /** Cuadrícula de 2 columnas (portadas grandes) */
    GRID_2,

    /** Cuadrícula de 3 columnas (portadas medianas) */
    GRID_3,

    /** Cuadrícula de 4 columnas (portadas pequeñas) */
    GRID_4,

    /** Cuadrícula de 6 columnas (portadas mini) */
    GRID_6,
}
