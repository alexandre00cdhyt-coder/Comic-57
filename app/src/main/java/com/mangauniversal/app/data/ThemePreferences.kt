package com.mangauniversal.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_prefs"
)

class ThemePreferences(private val context: Context) {

    companion object {
        private val KEY_BG_IMAGE = stringPreferencesKey("background_image")
        private val KEY_BG_OPACITY = floatPreferencesKey("background_opacity")
        private val KEY_CARD_OPACITY = floatPreferencesKey("card_opacity")

        const val DEFAULT_BG_OPACITY = 0.7f
        const val DEFAULT_CARD_OPACITY = 0.2f
    }

    suspend fun getBackgroundImage(): String? {
        return try {
            val prefs = context.themeDataStore.data.first()
            prefs[KEY_BG_IMAGE]
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveBackgroundImage(path: String?) {
        try {
            context.themeDataStore.edit { prefs ->
                if (path == null) {
                    prefs.remove(KEY_BG_IMAGE)
                } else {
                    prefs[KEY_BG_IMAGE] = path
                }
            }
        } catch (e: Exception) {
        }
    }

    suspend fun getBackgroundOpacity(): Float {
        return try {
            val prefs = context.themeDataStore.data.first()
            prefs[KEY_BG_OPACITY] ?: DEFAULT_BG_OPACITY
        } catch (e: Exception) {
            DEFAULT_BG_OPACITY
        }
    }

    suspend fun saveBackgroundOpacity(value: Float) {
        try {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_BG_OPACITY] = value.coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun getCardOpacity(): Float {
        return try {
            val prefs = context.themeDataStore.data.first()
            prefs[KEY_CARD_OPACITY] ?: DEFAULT_CARD_OPACITY
        } catch (e: Exception) {
            DEFAULT_CARD_OPACITY
        }
    }

    suspend fun saveCardOpacity(value: Float) {
        try {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_CARD_OPACITY] = value.coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
        }
    }
}
