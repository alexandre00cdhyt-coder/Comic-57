package com.mangauniversal.app.data.folder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Representa una carpeta configurada por el usuario.
 */
data class FolderConfig(
    val path: String,
    val name: String,
    val isFavorite: Boolean,
    val addedAt: Long,
    val comicCount: Int = 0,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "folder_prefs"
)

class FolderManager(private val context: Context) {

    companion object {
        private val KEY_FOLDERS = stringPreferencesKey("folders_list")
        private const val SEPARATOR = "|||"
        private const val FIELD_SEP = "|"
    }

    val folders: Flow<List<FolderConfig>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_FOLDERS] ?: ""
        decodeFolders(raw)
    }

    suspend fun addFolder(path: String, name: String) {
        context.dataStore.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS] ?: "")
            if (current.none { it.path == path }) {
                val newFolder = FolderConfig(
                    path = path,
                    name = name,
                    isFavorite = true,
                    addedAt = System.currentTimeMillis()
                )
                val updated = current + newFolder
                prefs[KEY_FOLDERS] = encodeFolders(updated)
            }
        }
    }

    suspend fun removeFolder(path: String) {
        context.dataStore.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS] ?: "")
            val updated = current.filterNot { it.path == path }
            prefs[KEY_FOLDERS] = encodeFolders(updated)
        }
    }

    suspend fun toggleFavorite(path: String) {
        context.dataStore.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS] ?: "")
            val updated = current.map { folder ->
                if (folder.path == path) folder.copy(isFavorite = !folder.isFavorite)
                else folder
            }
            prefs[KEY_FOLDERS] = encodeFolders(updated)
        }
    }

    suspend fun updateComicCount(path: String, count: Int) {
        context.dataStore.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS] ?: "")
            val updated = current.map { folder ->
                if (folder.path == path) folder.copy(comicCount = count)
                else folder
            }
            prefs[KEY_FOLDERS] = encodeFolders(updated)
        }
    }

    private fun encodeFolders(folders: List<FolderConfig>): String {
        return folders.joinToString(SEPARATOR) { folder ->
            listOf(
                folder.path.replace(FIELD_SEP, ""),
                folder.name.replace(FIELD_SEP, ""),
                folder.isFavorite.toString(),
                folder.addedAt.toString(),
                folder.comicCount.toString()
            ).joinToString(FIELD_SEP)
        }
    }

    private fun decodeFolders(raw: String): List<FolderConfig> {
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).mapNotNull { entry ->
            val parts = entry.split(FIELD_SEP)
            if (parts.size < 5) return@mapNotNull null
            runCatching {
                FolderConfig(
                    path = parts[0],
                    name = parts[1],
                    isFavorite = parts[2].toBoolean(),
                    addedAt = parts[3].toLong(),
                    comicCount = parts[4].toInt()
                )
            }.getOrNull()
        }
    }
}
