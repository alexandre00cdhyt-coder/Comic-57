package com.mangauniversal.app.ui

/**
 * Modos de vista de la biblioteca.
 */
enum class ViewMode {
    /** Lista con miniatura a la izquierda (60x80) */
    LIST_THUMB,

    /** Lista compacta: solo icono + nombre, sin miniatura */
    LIST_COMPACT,

    /** Cuadrícula de 2 columnas (portadas grandes) */
    GRID_2,

    /** Cuadrícula de 4 columnas (portadas medianas) */
    GRID_4,

    /** Cuadrícula de 6 columnas (portadas pequeñas) */
    GRID_6,
}
