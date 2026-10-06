package com.mangauniversal.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Modos de lectura del lector.
 */
enum class ReaderMode {
    /** Página a página, izquierda a derecha (occidental) */
    RIGHT,

    /** Página a página, derecha a izquierda (manga japonés) */
    LEFT,

    /** Tira larga: scroll vertical continuo */
    VERTICAL,
}

private val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reader_prefs"
)

class ReaderPreferences(private val context: Context) {

    companion object {
        private val KEY_MODE = stringPreferencesKey("reader_mode")
        private val KEY_VERTICAL_ZOOM = floatPreferencesKey("vertical_zoom")
        const val MIN_VERTICAL_ZOOM = 0.5f
        const val MAX_VERTICAL_ZOOM = 1.5f
        const val DEFAULT_VERTICAL_ZOOM = 1.0f
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

    /**
     * Devuelve el zoom del modo vertical (50% - 150%).
     */
    suspend fun getVerticalZoom(): Float {
        return try {
            val prefs = context.readerDataStore.data.first()
            val value = prefs[KEY_VERTICAL_ZOOM] ?: return DEFAULT_VERTICAL_ZOOM
            value.coerceIn(MIN_VERTICAL_ZOOM, MAX_VERTICAL_ZOOM)
        } catch (e: Exception) {
            DEFAULT_VERTICAL_ZOOM
        }
    }

    /**
     * Guarda el zoom del modo vertical.
     */
    suspend fun saveVerticalZoom(zoom: Float) {
        try {
            context.readerDataStore.edit { prefs ->
                prefs[KEY_VERTICAL_ZOOM] = zoom.coerceIn(MIN_VERTICAL_ZOOM, MAX_VERTICAL_ZOOM)
            }
        } catch (e: Exception) {
        }
    }
}
