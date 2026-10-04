package com.mangauniversal.app.data.scanner

import android.content.Context
import android.os.Environment
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

    /**
     * Escanea una carpeta concreta (y subcarpetas) buscando cómics.
     */
    suspend fun scanFolder(folderPath: String): List<Comic> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Comic>()
        val root = File(folderPath)
        if (!root.exists() || !root.canRead()) return@withContext emptyList()
        walk(root, results)
        // Ordenar con orden natural (v1, v2, v10 en vez de v1, v10, v2)
        results.sortedWith(compareBy(NaturalOrderComparator()) { it.title })
    }

    private suspend fun walk(dir: File, results: MutableList<Comic>) {
        coroutineContext.ensureActive()
        if (dir.name in ignoredFolders) return

        val children = dir.listFiles() ?: return

        // Si la carpeta tiene imágenes sueltas → tratarla como cómic
        val imageFiles = children.filter { it.isFile && it.name.isImageFile() }
        if (imageFiles.size >= 2) {
            results.add(folderToComic(dir, imageFiles))
            return
        }

        for (child in children) {
            coroutineContext.ensureActive()
            if (child.isDirectory && child.canRead()) {
                walk(child, results)
            } else if (child.isFile && child.name.isComicFile()) {
                results.add(fileToComic(child))
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
        val sorted = images.sortedWith(compareBy(NaturalOrderComparator()) { it.name })
        return Comic(
            id = folder.absolutePath,
            title = folder.name,
            path = folder.absolutePath,
            type = ComicType.FOLDER,
            coverPath = sorted.firstOrNull()?.absolutePath,
            pageCount = sorted.size,
            folder = folder.parentFile?.name ?: "",
            lastModified = folder.lastModified(),
            sizeBytes = images.sumOf { it.length() }
        )
    }
}

/**
 * Comparador que ordena strings con números de forma natural.
 * Ejemplo: "v1", "v2", "v10" (en vez de "v1", "v10", "v2")
 */
class NaturalOrderComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]

            if (ca.isDigit() && cb.isDigit()) {
                // Leer el número completo de cada lado
                var numA = 0
                while (i < a.length && a[i].isDigit()) {
                    numA = numA * 10 + (a[i] - '0')
                    i++
                }
                var numB = 0
                while (j < b.length && b[j].isDigit()) {
                    numB = numB * 10 + (b[j] - '0')
                    j++
                }
                if (numA != numB) return numA - numB
            } else {
                val la = ca.lowercaseChar()
                val lb = cb.lowercaseChar()
                if (la != lb) return la - lb
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}
