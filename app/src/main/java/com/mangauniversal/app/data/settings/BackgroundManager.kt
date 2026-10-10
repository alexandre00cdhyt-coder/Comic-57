package com.mangauniversal.app.data.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.backgroundDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "background_prefs"
)

data class BackgroundConfig(
    val imageUri: String? = null,
    val opacity: Float = 0.7f,
    val useBackground: Boolean = false,
)

class BackgroundManager(private val context: Context) {

    companion object {
        private val KEY_IMAGE = stringPreferencesKey("bg_image_uri")
        private val KEY_OPACITY = floatPreferencesKey("bg_opacity")
        private val KEY_ENABLED = stringPreferencesKey("bg_enabled")
    }

    suspend fun load(): BackgroundConfig {
        return try {
            val prefs = context.backgroundDataStore.data.first()
            BackgroundConfig(
                imageUri = prefs[KEY_IMAGE],
                opacity = prefs[KEY_OPACITY] ?: 0.7f,
                useBackground = prefs[KEY_ENABLED]?.toBoolean() ?: false
            )
        } catch (e: Exception) {
            BackgroundConfig()
        }
    }

    suspend fun saveImage(uri: Uri?) {
        try {
            context.backgroundDataStore.edit { prefs ->
                if (uri == null) prefs.remove(KEY_IMAGE) else prefs[KEY_IMAGE] = uri.toString()
            }
        } catch (e: Exception) {
        }
    }

    suspend fun saveOpacity(value: Float) {
        try {
            context.backgroundDataStore.edit { prefs ->
                prefs[KEY_OPACITY] = value.coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun saveEnabled(value: Boolean) {
        try {
            context.backgroundDataStore.edit { prefs ->
                prefs[KEY_ENABLED] = value.toString()
            }
        } catch (e: Exception) {
        }
    }
}
