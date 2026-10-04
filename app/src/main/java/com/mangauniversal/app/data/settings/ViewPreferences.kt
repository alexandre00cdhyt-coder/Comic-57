package com.mangauniversal.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mangauniversal.app.ui.ViewMode
import kotlinx.coroutines.flow.first

private val Context.viewDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "view_prefs"
)

/**
 * Gestiona la vista seleccionada por el usuario.
 * La persiste en DataStore para que se recuerde entre reinicios.
 */
class ViewPreferences(private val context: Context) {

    companion object {
        private val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
    }

    /**
     * Devuelve la vista guardada, o null si no hay ninguna.
     */
    suspend fun getViewMode(): ViewMode? {
        return try {
            val prefs = context.viewDataStore.data.first()
            val value = prefs[KEY_VIEW_MODE] ?: return null
            ViewMode.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Guarda la vista actual.
     */
    suspend fun saveViewMode(mode: ViewMode) {
        try {
            context.viewDataStore.edit { prefs ->
                prefs[KEY_VIEW_MODE] = mode.name
            }
        } catch (e: Exception) {
            // Ignorar
        }
    }
}
