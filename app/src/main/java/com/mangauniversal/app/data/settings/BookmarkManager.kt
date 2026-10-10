package com.mangauniversal.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.bookmarkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bookmark_prefs"
)

data class Bookmark(
    val comicId: String,
    val comicTitle: String,
    val comicPath: String,
    val page: Int,
    val note: String,
    val addedAt: Long,
)

class BookmarkManager(private val context: Context) {

    companion object {
        private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks_all")
        private const val ENTRY_SEP = "@@@"
        private const val FIELD_SEP = "###"
    }

    suspend fun getAll(): List<Bookmark> {
        return try {
            val prefs = context.bookmarkDataStore.data.first()
            val raw = prefs[KEY_BOOKMARKS] ?: ""
            decode(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun add(bookmark: Bookmark) {
        try {
            context.bookmarkDataStore.edit { prefs ->
                val current = decode(prefs[KEY_BOOKMARKS] ?: "")
                val filtered = current.filterNot {
                    it.comicId == bookmark.comicId && it.page == bookmark.page
                }
                val updated = (listOf(bookmark) + filtered).take(500)
                prefs[KEY_BOOKMARKS] = encode(updated)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun remove(comicId: String, page: Int) {
        try {
            context.bookmarkDataStore.edit { prefs ->
                val current = decode(prefs[KEY_BOOKMARKS] ?: "")
                val updated = current.filterNot {
                    it.comicId == comicId && it.page == page
                }
                prefs[KEY_BOOKMARKS] = encode(updated)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun clearAll() {
        try {
            context.bookmarkDataStore.edit { prefs ->
                prefs[KEY_BOOKMARKS] = ""
            }
        } catch (e: Exception) {
        }
    }

    private fun encode(list: List<Bookmark>): String {
        return list.joinToString(ENTRY_SEP) { b ->
            listOf(
                b.comicId.replace(FIELD_SEP, "").replace(ENTRY_SEP, ""),
                b.comicTitle.replace(FIELD_SEP, "").replace(ENTRY_SEP, ""),
                b.comicPath.replace(FIELD_SEP, "").replace(ENTRY_SEP, ""),
                b.page.toString(),
                b.note.replace(FIELD_SEP, "").replace(ENTRY_SEP, ""),
                b.addedAt.toString()
            ).joinToString(FIELD_SEP)
        }
    }

    private fun decode(raw: String): List<Bookmark> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ENTRY_SEP).mapNotNull { entry ->
            val parts = entry.split(FIELD_SEP)
            if (parts.size < 6) return@mapNotNull null
            try {
                Bookmark(
                    comicId = parts[0],
                    comicTitle = parts[1],
                    comicPath = parts[2],
                    page = parts[3].toIntOrNull() ?: 0,
                    note = parts[4],
                    addedAt = parts[5].toLongOrNull() ?: 0L
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
