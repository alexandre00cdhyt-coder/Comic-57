package com.mangauniversal.app.data.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.scanner.NaturalOrderComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class PageExtractor(private val context: Context) {

    private val pagesRoot: File by lazy {
        File(context.cacheDir, "pages").apply { mkdirs() }
    }

    suspend fun getPages(comic: Comic): List<File> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(pagesRoot, comic.id.hashCode().toString())

            if (targetDir.exists()) {
                val existing = targetDir.listFiles()
                    ?.filter { it.isFile }
                    ?.sortedWith(compareBy(NaturalOrderComparator()) { it.name })
                    ?: emptyList()
                if (existing.isNotEmpty()) return@withContext existing
            }

            targetDir.mkdirs()

            when (comic.type) {
                ComicType.FOLDER -> extractFolder(comic, targetDir)
                ComicType.CBZ, ComicType.ZIP -> extractZip(comic, targetDir)
                ComicType.PDF -> extractPdf(comic, targetDir)
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractFolder(comic: Comic, targetDir: File): List<File> {
        val folder = File(comic.path)
        return folder.listFiles()
            ?.filter { it.isFile && isImage(it.name) }
            ?.sortedWith(compareBy(NaturalOrderComparator()) { it.name })
            ?: emptyList()
    }

    private fun extractZip(comic: Comic, targetDir: File): List<File> {
        val file = File(comic.path)
        val outFiles = mutableListOf<File>()
        ZipFile(file).use { zip ->
            val imageEntries = zip.entries().asSequence()
                .filter { !it.isDirectory && isImage(it.name) }
                .sortedWith(compareBy(NaturalOrderComparator()) { it.name })
                .toList()
            var index = 0
            for (entry in imageEntries) {
                index++
                val ext = entry.name.substringAfterLast('.', "jpg")
                val outFile = File(targetDir, String.format("%05d.%s", index, ext))
                try {
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { out -> input.copyTo(out) }
                    }
                    if (outFile.exists() && outFile.length() > 0) outFiles.add(outFile)
                } catch (e: Exception) { }
            }
        }
        return outFiles
    }

    private fun extractPdf(comic: Comic, targetDir: File): List<File> {
        val file = File(comic.path)
        val outFiles = mutableListOf<File>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    try {
                        renderer.openPage(i).use { page ->
                            val width = 1200
                            val height = (width * page.height.toFloat() / page.width.toFloat()).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val outFile = File(targetDir, String.format("%05d.jpg", i + 1))
                            FileOutputStream(outFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            bitmap.recycle()
                            if (outFile.exists() && outFile.length() > 0) outFiles.add(outFile)
                        }
                    } catch (e: Exception) { }
                }
            }
        }
        return outFiles
    }

    private fun isImage(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif")
    }
}
