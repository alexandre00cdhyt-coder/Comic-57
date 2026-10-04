package com.mangauniversal.app.data.scanner

import android.content.Context
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.model.detectComicType
import com.mangauniversal.app.data.model.isComicFile
import com.mangauniversal.app.data.model.isImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class ComicScanner(private val context: Context) {

    private val ignoredFolders = setOf(
        "Android", ".thumbnails", ".cache", "cache", "temp", "tmp",
        ".nomedia", "LOST.DIR", "System Volume Information"
    )

    private val ignoredExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v",
        "mp3", "wav", "ogg", "flac", "aac", "m4a",
        "txt", "log", "ini", "cfg", "json", "xml", "html", "htm",
        "apk", "exe", "dll", "so", "db", "sqlite", "bak"
    )

    suspend fun scanFolder(folderPath: String): List<Comic> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Comic>()
        try {
            val root = File(folderPath)
            if (!root.exists() || !root.canRead()) return@withContext emptyList()
            walk(root, results, depth = 0)
        } catch (e: Exception) {
            // Devolvemos lo que tengamos
        }
        results.sortedWith(compareBy(NaturalOrderComparator()) { it.title })
    }

    private suspend fun walk(dir: File, results: MutableList<Comic>, depth: Int) {
        coroutineContext.ensureActive()
        if (depth > 6) return
        if (dir.name in ignoredFolders) return

        val children = try {
            dir.listFiles() ?: return
        } catch (e: Exception) {
            return
        }

        // Filtrar solo imágenes sueltas
        val imageFiles = children.filter { child ->
            child.isFile && child.name.isImageFile()
        }

        // Si hay 3+ imágenes → tratarla como cómic y NO seguir bajando
        if (imageFiles.size >= 3) {
            results.add(folderToComic(dir, imageFiles))
            return
        }

        // Recorrer subcarpetas y archivos cómic
        for (child in children) {
            coroutineContext.ensureActive()

            val ext = child.extension.lowercase()
            if (ext in ignoredExtensions) continue

            try {
                if (child.isDirectory && child.canRead()) {
                    walk(child, results, depth + 1)
                } else if (child.isFile && child.name.isComicFile()) {
                    results.add(fileToComic(child))
                }
            } catch (e: Exception) {
                continue
            }
        }
    }

    private fun fileToComic(file: File): Comic {
        val type = detectComicType(file.name)
        return Comic(
            id = file.absolutePath,
            title = file.nameWithoutExtension,
            path = file.absolutePath,
            type = type,
            folder = file.parentFile?.name ?: "",
            lastModified = file.lastModified(),
            sizeBytes = file.length()
        )
    }

    private fun folderToComic(folder: File, images: List<File>): Comic {
        val sorted = try {
            images.sortedWith(compareBy(NaturalOrderComparator()) { it.name })
        } catch (e: Exception) {
            images
        }
        val totalSize = try {
            sorted.fold(0L) { acc, img -> acc + img.length() }
        } catch (e: Exception) {
            0L
        }
        return Comic(
            id = folder.absolutePath,
            title = folder.name,
            path = folder.absolutePath,
            type = ComicType.FOLDER,
            coverPath = sorted.firstOrNull()?.absolutePath,
            pageCount = sorted.size,
            folder = folder.parentFile?.name ?: "",
            lastModified = folder.lastModified(),
            sizeBytes = totalSize
        )
    }
}

class NaturalOrderComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                var numA = 0L
                while (i < a.length && a[i].isDigit()) {
                    numA = numA * 10 + (a[i] - '0'); i++
                }
                var numB = 0L
                while (j < b.length && b[j].isDigit()) {
                    numB = numB * 10 + (b[j] - '0'); j++
                }
                if (numA != numB) return if (numA < numB) -1 else 1
            } else {
                val la = ca.lowercaseChar()
                val lb = cb.lowercaseChar()
                if (la != lb) return la - lb
                i++; j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}
