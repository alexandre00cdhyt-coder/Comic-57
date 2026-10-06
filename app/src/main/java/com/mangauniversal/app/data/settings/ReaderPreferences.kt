package com.mangauniversal.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

enum class ReaderMode {
    RIGHT,
    LEFT,
    VERTICAL,
}

private val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reader_prefs"
)

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "progress_prefs"
)

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "history_prefs"
)

class ReaderPreferences(private val context: Context) {

    companion object {
        private val KEY_MODE = stringPreferencesKey("reader_mode")
        private val KEY_VERTICAL_ZOOM = floatPreferencesKey("vertical_zoom")
        const val MIN_VERTICAL_ZOOM = 0.5f
        const val MAX_VERTICAL_ZOOM = 1.5f
        const val DEFAULT_VERTICAL_ZOOM = 1.0f

        // Prefijos
        private const val PROGRESS_PREFIX = "progress_"
        private const val TIMESTAMP_PREFIX = "timestamp_"
        private const val HISTORY_LIST_KEY = "history_list"
        private const val HISTORY_SEP = "|||"
    }

    // ─────────── MODO DE LECTURA ───────────

    suspend fun getMode(): ReaderMode {
        return try {
            val prefs = context.readerDataStore.data.first()
            val value = prefs[KEY_MODE] ?: return ReaderMode.RIGHT
            ReaderMode.valueOf(value)
        } catch (e: Exception) {
            ReaderMode.RIGHT
        }
    }

    suspend fun saveMode(mode: ReaderMode) {
        try {
            context.readerDataStore.edit { prefs ->
                prefs[KEY_MODE] = mode.name
            }
        } catch (e: Exception) {
        }
    }

    // ─────────── ZOOM VERTICAL ───────────

    suspend fun getVerticalZoom(): Float {
        return try {
            val prefs = context.readerDataStore.data.first()
            val value = prefs[KEY_VERTICAL_ZOOM] ?: return DEFAULT_VERTICAL_ZOOM
            value.coerceIn(MIN_VERTICAL_ZOOM, MAX_VERTICAL_ZOOM)
        } catch (e: Exception) {
            DEFAULT_VERTICAL_ZOOM
        }
    }

    suspend fun saveVerticalZoom(zoom: Float) {
        try {
            context.readerDataStore.edit { prefs ->
                prefs[KEY_VERTICAL_ZOOM] = zoom.coerceIn(MIN_VERTICAL_ZOOM, MAX_VERTICAL_ZOOM)
            }
        } catch (e: Exception) {
        }
    }

    // ─────────── PROGRESO ───────────

    suspend fun saveProgress(comicId: String, page: Int) {
        try {
            context.progressDataStore.edit { prefs ->
                val key = stringPreferencesKey(PROGRESS_PREFIX + comicId.hashCode().toString())
                prefs[key] = page.toString()
            }
        } catch (e: Exception) {
        }
    }

    suspend fun getProgress(comicId: String): Int {
        return try {
            val prefs = context.progressDataStore.data.first()
            val key = stringPreferencesKey(PROGRESS_PREFIX + comicId.hashCode().toString())
            val value = prefs[key] ?: return 0
            value.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // ─────────── HISTORIAL ───────────

    /**
     * Añade un cómic al historial con su timestamp.
     * Se guarda como lista separada por "|||".
     * Formato: "comicId:timestamp|||comicId:timestamp|||..."
     */
    suspend fun addToHistory(comicId: String, comicTitle: String, comicPath: String) {
        try {
            context.historyDataStore.edit { prefs ->
                val raw = prefs[HISTORY_LIST_KEY] ?: ""
                val entries = if (raw.isBlank()) mutableListOf() else raw.split(HISTORY_SEP).toMutableList()

                // Quitar el cómic si ya está
                entries.removeAll { it.startsWith("$comicId:") }

                // Añadir con timestamp actual al principio
                val timestamp = System.currentTimeMillis()
                val entry = "$comicId:$timestamp:$comicTitle:$comicPath"
                entries.add(0, entry)

                // Guardar solo los últimos 100
                val limited = entries.take(100)

                prefs[HISTORY_LIST_KEY] = limited.joinToString(HISTORY_SEP)
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Devuelve la lista de cómics leídos.
     * Formato: Lista de (comicId, timestamp, title, path) ordenada por más reciente.
     */
    suspend fun getHistory(): List<HistoryEntry> {
        return try {
            val prefs = context.historyDataStore.data.first()
            val raw = prefs[HISTORY_LIST_KEY] ?: ""
            if (raw.isBlank()) return emptyList()

            raw.split(HISTORY_SEP).mapNotNull { entry ->
                val parts = entry.split(":", limit = 4)
                if (parts.size < 4) return@mapNotNull null
                HistoryEntry(
                    comicId = parts[0],
                    timestamp = parts[1].toLongOrNull() ?: 0L,
                    title = parts[2],
                    path = parts[3]
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun clearHistory() {
        try {
            context.historyDataStore.edit { prefs ->
                prefs[HISTORY_LIST_KEY] = ""
            }
        } catch (e: Exception) {
        }
    }
}

/**
 * Entrada del historial.
 */
data class HistoryEntry(
    val comicId: String,
    val timestamp: Long,
    val title: String,
    val path: String,
)
