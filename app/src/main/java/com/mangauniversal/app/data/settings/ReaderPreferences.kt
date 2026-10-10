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
    name = "reader_prefs_v5"
)

class ReaderPreferences(private val context: Context) {

    companion object {
        private val KEY_MODE = stringPreferencesKey("reader_mode")
        private val KEY_VERTICAL_ZOOM = floatPreferencesKey("vertical_zoom")
        private val KEY_PROGRESS_ALL = stringPreferencesKey("progress_all")
        private val KEY_HISTORY_ALL = stringPreferencesKey("history_all")

        const val MIN_VERTICAL_ZOOM = 0.5f
        const val MAX_VERTICAL_ZOOM = 1.5f
        const val DEFAULT_VERTICAL_ZOOM = 1.0f

        private const val ENTRY_SEP = "@@@"
        private const val FIELD_SEP = "###"
    }

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

    suspend fun saveProgress(comicId: String, page: Int) {
        try {
            context.readerDataStore.edit { prefs ->
                val raw = prefs[KEY_PROGRESS_ALL] ?: ""
                val map = decodeProgress(raw).toMutableMap()
                map[comicId] = page
                prefs[KEY_PROGRESS_ALL] = encodeProgress(map)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun getProgress(comicId: String): Int {
        return try {
            val prefs = context.readerDataStore.data.first()
            val raw = prefs[KEY_PROGRESS_ALL] ?: return 0
            decodeProgress(raw)[comicId] ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getAllProgress(): Map<String, Int> {
        return try {
            val prefs = context.readerDataStore.data.first()
            val raw = prefs[KEY_PROGRESS_ALL] ?: return emptyMap()
            decodeProgress(raw)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun encodeProgress(map: Map<String, Int>): String {
        return map.entries.joinToString(ENTRY_SEP) { (k, v) ->
            "$k$FIELD_SEP$v"
        }
    }

    private fun decodeProgress(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(ENTRY_SEP).mapNotNull { entry ->
            val parts = entry.split(FIELD_SEP, limit = 2)
            if (parts.size < 2) return@mapNotNull null
            val key = parts[0]
            val value = parts[1].toIntOrNull() ?: return@mapNotNull null
            key to value
        }.toMap()
    }

    suspend fun addToHistory(comicId: String, comicTitle: String, comicPath: String) {
        try {
            context.readerDataStore.edit { prefs ->
                val raw = prefs[KEY_HISTORY_ALL] ?: ""
                val entries = decodeHistory(raw).toMutableList()

                entries.removeAll { it.comicId == comicId }

                entries.add(
                    0,
                    HistoryEntry(
                        comicId = comicId,
                        timestamp = System.currentTimeMillis(),
                        title = comicTitle.replace(FIELD_SEP, "").replace(ENTRY_SEP, ""),
                        path = comicPath.replace(FIELD_SEP, "").replace(ENTRY_SEP, "")
                    )
                )

                val limited = entries.take(200)
                prefs[KEY_HISTORY_ALL] = encodeHistory(limited)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun getHistory(): List<HistoryEntry> {
        return try {
            val prefs = context.readerDataStore.data.first()
            val raw = prefs[KEY_HISTORY_ALL] ?: ""
            decodeHistory(raw).sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun removeFromHistory(comicId: String) {
        try {
            context.readerDataStore.edit { prefs ->
                val raw = prefs[KEY_HISTORY_ALL] ?: ""
                val entries = decodeHistory(raw).filterNot { it.comicId == comicId }
                prefs[KEY_HISTORY_ALL] = encodeHistory(entries)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun clearHistory() {
        try {
            context.readerDataStore.edit { prefs ->
                prefs[KEY_HISTORY_ALL] = ""
            }
        } catch (e: Exception) {
        }
    }

    private fun encodeHistory(entries: List<HistoryEntry>): String {
        return entries.joinToString(ENTRY_SEP) { e ->
            listOf(
                e.comicId,
                e.timestamp.toString(),
                e.title,
                e.path
            ).joinToString(FIELD_SEP)
        }
    }

    private fun decodeHistory(raw: String): List<HistoryEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ENTRY_SEP).mapNotNull { entry ->
            val parts = entry.split(FIELD_SEP)
            if (parts.size < 4) return@mapNotNull null
            try {
                HistoryEntry(
                    comicId = parts[0],
                    timestamp = parts[1].toLongOrNull() ?: 0L,
                    title = parts[2],
                    path = parts[3]
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class HistoryEntry(
    val comicId: String,
    val timestamp: Long,
    val title: String,
    val path: String,
)
