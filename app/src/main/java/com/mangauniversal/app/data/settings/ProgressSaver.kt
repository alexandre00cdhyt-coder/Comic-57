package com.mangauniversal.app.data.settings

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ProgressSaver {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun saveProgress(context: Context, comicId: String, page: Int) {
        appScope.launch {
            try {
                ReaderPreferences(context.applicationContext).saveProgress(comicId, page)
            } catch (e: Exception) {
            }
        }
    }

    fun addToHistory(context: Context, comicId: String, title: String, path: String) {
        appScope.launch {
            try {
                ReaderPreferences(context.applicationContext)
                    .addToHistory(comicId, title, path)
            } catch (e: Exception) {
            }
        }
    }
}
