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
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Escáner de cómics en el almacenamiento del dispositivo.
 *
 * Recorre recursivamente:
 *  - Memoria interna: /storage/emulated/0/
 *  - Memoria externa / microSD: /storage/XXXX-XXXX/
 *
 * Detecta:
 *  - Archivos CBZ, CBR, ZIP, RAR, 7Z, PDF
 *  - Carpetas que contengan imágenes (se tratan como un cómic)
 */
class ComicScanner(private val context: Context) {

    // Carpetas que NO queremos escanear (basura del sistema)
    private val ignoredFolders = setOf(
        "Android",
        ".thumbnails",
        ".cache",
        "cache",
        "temp",
        "tmp",
        ".nomedia",
        "LOST.DIR",
        "System Volume Information"
    )

    /**
     * Escanea todo el almacenamiento y devuelve la lista de cómics encontrados.
     */
    suspend fun scanAll(): List<Comic> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Comic>()
        val roots = getStorageRoots()

        for (root in roots) {
            if (!root.exists() || !root.canRead()) continue
            walk(root, results)
        }

        results.sortedBy { it.title.lowercase() }
    }

    /**
     * Devuelve las raíces de almacenamiento disponibles.
     * Interna + microSD (si está montada).
     */
    private fun getStorageRoots(): List<File> {
        val roots = mutableListOf<File>()

        // Almacenamiento interno principal
        val primary = Environment.getExternalStorageDirectory()
        if (primary != null && primary.exists()) {
            roots.add(primary)
        }

        // Almacenamiento externo / microSD
        val storageDir = File("/storage")
        if (storageDir.exists() && storageDir.isDirectory) {
            storageDir.listFiles()?.forEach { file ->
                if (file.isDirectory &&
                    file.canRead() &&
                    file.name != "emulated" &&
                    file.name != "self"
                ) {
                    roots.add(file)
                }
            }
        }

        return roots
    }

    /**
     * Recorre recursivamente una carpeta buscando cómics.
     */
    private suspend fun walk(dir: File, results: MutableList<Comic>) {
        coroutineContext.ensureActive()

        // Ignorar carpetas basura
        if (dir.name in ignoredFolders) return

        val children = dir.listFiles() ?: return

        // Comprobar si la carpeta actual es un "cómic" (contiene imágenes)
        val imageFiles = children.filter { it.isFile && it.name.isImageFile() }
        if (imageFiles.size >= 2) {
            // Es una carpeta de imágenes → la tratamos como cómic
            results.add(folderToComic(dir, imageFiles))
            return // No seguimos bajando dentro
        }

        // Recorrer los hijos
        for (child in children) {
            coroutineContext.ensureActive()

            if (child.isDirectory) {
                walk(child, results)
            } else if (child.isFile && child.name.isComicFile()) {
                results.add(fileToComic(child))
            }
        }
    }

    /**
     * Convierte un archivo (CBZ, PDF, etc.) en un Comic.
     */
    private fun fileToComic(file: File): Comic {
        val type = detectComicType(file.name)
        val title = file.nameWithoutExtension
        val folder = file.parentFile?.name ?: ""

        return Comic(
            id = hashId(file.absolutePath),
            title = title,
            path = file.absolutePath,
            type = type,
            coverPath = null,
            pageCount = 0, // Se calculará al abrir el cómic
            lastModified = file.lastModified(),
            sizeBytes = file.length(),
            folder = folder,
            isFavorite = false,
            lastPageRead = 0,
            lastReadAt = 0L,
        )
    }

    /**
     * Convierte una carpeta con imágenes en un Comic.
     */
    private fun folderToComic(folder: File, images: List<File>): Comic {
        val sorted = images.sortedBy { it.name.lowercase() }
        return Comic(
            id = hashId(folder.absolutePath),
            title = folder.name,
            path = folder.absolutePath,
            type = ComicType.FOLDER,
            coverPath = sorted.firstOrNull()?.absolutePath,
            pageCount = sorted.size,
            lastModified = folder.lastModified(),
            sizeBytes = images.sumOf { it.length() },
            folder = folder.parentFile?.name ?: "",
            isFavorite = false,
            lastPageRead = 0,
            lastReadAt = 0L,
        )
    }

    /**
     * Genera un ID único a partir de la ruta.
     */
    private fun hashId(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
