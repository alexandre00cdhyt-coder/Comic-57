package com.mangauniversal.app.data.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.domain.util.NaturalOrderComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.ZipFile
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

class PageExtractor(private val context: Context) {

    private val pagesRoot: File by lazy {
        File(context.cacheDir, "pages").apply { mkdirs() }
    }

    /**
     * Devuelve la lista de archivos de imagen.
     * Para PDF, devolverá un archivo vacío (marcador) y la renderización se hará bajo demanda.
     */
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
                ComicType.PDF -> listOf(File(comic.path)) // Para PDF, devolvemos el archivo original
                ComicType.CBR, ComicType.RAR -> extractRar(comic, targetDir)
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

    private fun extractRar(comic: Comic, targetDir: File): List<File> {
        val file = File(comic.path)
        val outFiles = mutableListOf<File>()
        var archive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        try {
            randomAccessFile = RandomAccessFile(file, "r")
            archive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))

            val simpleArchive = archive.simpleInterface
            val items = simpleArchive.archiveItems
                ?.filter { !it.isFolder && isImage(it.path) }
                ?.sortedWith(compareBy(NaturalOrderComparator()) { it.path })
                ?: emptyList()

            var index = 0
            for (item in items) {
                index++
                val ext = item.path.substringAfterLast('.', "jpg")
                val outFile = File(targetDir, String.format("%05d.%s", index, ext))

                try {
                    val result = item.extractSlow(object : ISequentialOutStream {
                        override fun write(data: ByteArray): Int {
                            try {
                                FileOutputStream(outFile, true).use { out ->
                                    out.write(data)
                                }
                                return data.size
                            } catch (e: Exception) {
                                return 0
                            }
                        }
                    })

                    if (result == ExtractOperationResult.OK && outFile.exists() && outFile.length() > 0) {
                        outFiles.add(outFile)
                    } else {
                        outFile.delete()
                    }
                } catch (e: Exception) {
                    outFile.delete()
                }
            }
        } catch (e: Exception) {
            return emptyList()
        } finally {
            try { archive?.close() } catch (e: Exception) { }
            try { randomAccessFile?.close() } catch (e: Exception) { }
        }

        return outFiles
    }

    /**
     * Renderiza una página específica de un PDF a un archivo temporal.
     * Esto es lo que hace que los PDFs abran al instante.
     */
    suspend fun renderPdfPage(pdfFile: File, pageIndex: Int, cacheDir: File): File? = withContext(Dispatchers.IO) {
        try {
            val outFile = File(cacheDir, String.format("pdf_page_%05d.jpg", pageIndex))
            if (outFile.exists() && outFile.length() > 0) return@withContext outFile

            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    
                    renderer.openPage(pageIndex).use { page ->
                        // Resolución para el lector
                        val width = 1200
                        val height = (width * page.height.toFloat() / page.width.toFloat()).toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        FileOutputStream(outFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        bitmap.recycle()
                    }
                }
            }
            if (outFile.exists() && outFile.length() > 0) outFile else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene la cantidad de páginas de un PDF.
     */
    suspend fun getPdfPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun isImage(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif")
    }
}
