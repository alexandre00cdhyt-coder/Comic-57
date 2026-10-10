package com.mangauniversal.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_prefs"
)

data class AppSettings(
    val backgroundImage: String? = null,
    val backgroundOpacity: Float = 0.7f,
    val cardOpacity: Float = 0.2f,
    val showStatusBar: Boolean = true,
    val fullScreenImage: Boolean = true,
    val showFolderPath: Boolean = true,
    val leftScrollbar: Boolean = false,
    val resumePage: Boolean = true,
    val readerTheme: String = "DARK",
    val blueLightFilter: Float = 0f,
    val brightness: Float = 1f,
)

class ThemePreferences(private val context: Context) {

    companion object {
        private val KEY_BG_IMAGE = stringPreferencesKey("background_image")
        private val KEY_BG_OPACITY = floatPreferencesKey("background_opacity")
        private val KEY_CARD_OPACITY = floatPreferencesKey("card_opacity")
        private val KEY_SHOW_STATUS = booleanPreferencesKey("show_status_bar")
        private val KEY_FULLSCREEN = booleanPreferencesKey("fullscreen_image")
        private val KEY_SHOW_PATH = booleanPreferencesKey("show_folder_path")
        private val KEY_LEFT_SCROLL = booleanPreferencesKey("left_scrollbar")
        private val KEY_RESUME = booleanPreferencesKey("resume_page")
        private val KEY_READER_THEME = stringPreferencesKey("reader_theme")
        private val KEY_BLUE_FILTER = floatPreferencesKey("blue_light_filter")
        private val KEY_BRIGHTNESS = floatPreferencesKey("app_brightness")

        const val DEFAULT_BG_OPACITY = 0.7f
        const val DEFAULT_CARD_OPACITY = 0.2f
    }

    suspend fun load(): AppSettings {
        return try {
            val prefs = context.themeDataStore.data.first()
            AppSettings(
                backgroundImage = prefs[KEY_BG_IMAGE],
                backgroundOpacity = prefs[KEY_BG_OPACITY] ?: DEFAULT_BG_OPACITY,
                cardOpacity = prefs[KEY_CARD_OPACITY] ?: DEFAULT_CARD_OPACITY,
                showStatusBar = prefs[KEY_SHOW_STATUS] ?: true,
                fullScreenImage = prefs[KEY_FULLSCREEN] ?: true,
                showFolderPath = prefs[KEY_SHOW_PATH] ?: true,
                leftScrollbar = prefs[KEY_LEFT_SCROLL] ?: false,
                resumePage = prefs[KEY_RESUME] ?: true,
                readerTheme = prefs[KEY_READER_THEME] ?: "DARK",
                blueLightFilter = prefs[KEY_BLUE_FILTER] ?: 0f,
                brightness = prefs[KEY_BRIGHTNESS] ?: 1f
            )
        } catch (e: Exception) {
            AppSettings()
        }
    }

    suspend fun saveBackgroundImage(path: String?) {
        try {
            context.themeDataStore.edit { prefs ->
                if (path == null) prefs.remove(KEY_BG_IMAGE) else prefs[KEY_BG_IMAGE] = path
            }
        } catch (e: Exception) {
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

    suspend fun saveCardOpacity(value: Float) {
        try {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_CARD_OPACITY] = value.coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun saveShowStatusBar(value: Boolean) {
        try {
            context.themeDataStore.edit { prefs -> prefs[KEY_SHOW_STATUS] = value }
        } catch (e: Exception) {
        }
    }

    suspend fun saveFullScreen(value: Boolean) {
        try {
            context.themeDataStore.edit { prefs -> prefs[KEY_FULLSCREEN] = value }
        } catch (e: Exception) {
        }
    }

    suspend fun saveShowFolderPath(value: Boolean) {
        try {
            context.themeDataStore.edit { prefs -> prefs[KEY_SHOW_PATH] = value }
        } catch (e: Exception) {
        }
    }

    suspend fun saveLeftScrollbar(value: Boolean) {
        try {
            context.themeDataStore.edit { prefs -> prefs[KEY_LEFT_SCROLL] = value }
        } catch (e: Exception) {
        }
    }

    suspend fun saveResumePage(value: Boolean) {
        try {
            context.themeDataStore.edit { prefs -> prefs[KEY_RESUME] = value }
        } catch (e: Exception) {
        }
    }

    suspend fun saveReaderTheme(value: String) {
        try {
            context.themeDataStore.edit { prefs -> prefs[KEY_READER_THEME] = value }
        } catch (e: Exception) {
        }
    }

    suspend fun saveBlueLightFilter(value: Float) {
        try {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_BLUE_FILTER] = value.coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun saveBrightness(value: Float) {
        try {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_BRIGHTNESS] = value.coerceIn(0.3f, 1f)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun resetAll() {
        try {
            context.themeDataStore.edit { prefs -> prefs.clear() }
        } catch (e: Exception) {
        }
    }
}
