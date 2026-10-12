package com.mangauniversal.app.data.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

private val Context.backgroundDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "background_prefs"
)

data class BackgroundConfig(
    val imagePath: String? = null,
    val opacity: Float = 0.7f,
    val useBackground: Boolean = false,
)

class BackgroundManager(private val context: Context) {

    companion object {
        private val KEY_IMAGE = stringPreferencesKey("bg_image_path")
        private val KEY_OPACITY = floatPreferencesKey("bg_opacity")
        private val KEY_ENABLED = stringPreferencesKey("bg_enabled")
        private const val BG_FILENAME = "background_image.jpg"
    }

    private val bgFile: File
        get() = File(context.filesDir, BG_FILENAME)

    suspend fun load(): BackgroundConfig {
        return try {
            val prefs = context.backgroundDataStore.data.first()
            val path = prefs[KEY_IMAGE]
            val exists = path != null && File(path).exists()
            BackgroundConfig(
                imagePath = if (exists) path else null,
                opacity = prefs[KEY_OPACITY] ?: 0.7f,
                useBackground = (prefs[KEY_ENABLED]?.toBoolean() ?: false) && exists
            )
        } catch (e: Exception) {
            BackgroundConfig()
        }
    }

    suspend fun saveImageFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                bgFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            val ok = bgFile.exists() && bgFile.length() > 0
            if (ok) {
                context.backgroundDataStore.edit { prefs ->
                    prefs[KEY_IMAGE] = bgFile.absolutePath
                }
            }
            ok
        } catch (e: Exception) {
            false
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
