package com.mangauniversal.app.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.domain.util.NaturalOrderComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.ZipFile
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

class ThumbnailExtractor(private val context: Context) {

    private val thumbsDir: File by lazy {
        File(context.cacheDir, "thumbs").apply { mkdirs() }
    }

    private val naturalComparator = NaturalOrderComparator()
    private val thumbnailWidth = 400
    
    // Semáforo para evitar OOM (máximo 2 procesamientos a la vez)
    private val semaphore = Semaphore(2)

    suspend fun getThumbnail(comic: Comic): File? = withContext(Dispatchers.IO) {
        semaphore.withPermit {
            try {
                val outFile = File(thumbsDir, "${hashId(comic.id)}.jpg")

                if (outFile.exists() && outFile.length() > 0) {
                    return@withPermit outFile
                }

                val extracted: Bitmap? = when (comic.type) {
                    ComicType.FOLDER -> extractFromFolder(comic)
                    ComicType.CBZ, ComicType.ZIP -> extractFromZip(comic)
                    ComicType.PDF -> extractFromPdf(comic)
                    ComicType.CBR, ComicType.RAR -> extractFromRar(comic)
                    else -> null
                }

                if (extracted == null) return@withPermit null

                FileOutputStream(outFile).use { out ->
                    extracted.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                extracted.recycle()

                if (outFile.exists() && outFile.length() > 0) outFile else null
            } catch (e: OutOfMemoryError) {
                Log.e("ThumbnailExtractor", "OOM en ${comic.path}")
                null
            } catch (e: Exception) {
                Log.e("ThumbnailExtractor", "Error en ${comic.path}", e)
                null
            }
        }
    }

    private fun decodeSampledBitmap(file: File, reqWidth: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= reqWidth) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(input: java.io.InputStream, reqWidth: Int): Bitmap? {
        return try {
            val bytes = input.readBytes()
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (options.outWidth <= 0) return null

            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= reqWidth) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFromFolder(comic: Comic): Bitmap? {
        val folder = File(comic.path)
        val image = folder.listFiles()
            ?.filter { it.isFile && isImage(it.name) }
            ?.minWithOrNull { a, b -> naturalComparator.compare(a.name, b.name) }
            ?: return null
        return decodeSampledBitmap(image, thumbnailWidth)
    }

    private fun extractFromZip(comic: Comic): Bitmap? {
        val file = File(comic.path)
        ZipFile(file).use { zip ->
            val imageEntry = zip.entries().asSequence()
                .filter { !it.isDirectory && isImage(it.name) }
                .minWithOrNull { a, b -> naturalComparator.compare(a.name, b.name) }
                ?: return null
            return zip.getInputStream(imageEntry).use { input ->
                decodeSampledBitmap(input, thumbnailWidth)
            }
        }
    }

    private fun extractFromRar(comic: Comic): Bitmap? {
        val file = File(comic.path)
        // Si el archivo es mayor a 200MB, no intentar leer la portada para no colapsar la RAM
        if (file.length() > 200L * 1024L * 1024L) {
            Log.w("ThumbnailExtractor", "RAR muy grande, omitiendo portada")
            return null
        }

        var randomAccessFile: RandomAccessFile? = null
        var archive: net.sf.sevenzipjbinding.IInArchive? = null

        try {
            randomAccessFile = RandomAccessFile(file, "r")
            archive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
            val simpleArchive = archive.simpleInterface
            val item = simpleArchive.archiveItems
                ?.filter { !it.isFolder && isImage(it.path) }
                ?.minWithOrNull { a, b -> naturalComparator.compare(a.path, b.path) }
                ?: return null

            // En lugar de escribir a un archivo temporal (lento), guardamos en memoria
            var bytes: ByteArray? = null
            var totalSize = 0
            val result = item.extractSlow(object : ISequentialOutStream {
                override fun write(data: ByteArray): Int {
                    if (bytes == null) bytes = data.clone() else bytes = bytes!! + data
                    totalSize += data.size
                    // Si supera 15MB, abortar para no colapsar memoria
                    if (totalSize > 15 * 1024 * 1024) return 0
                    return data.size
                }
            })

            if (result == ExtractOperationResult.OK && bytes != null) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size, options)
                
                var sampleSize = 1
                while (options.outWidth / (sampleSize * 2) >= thumbnailWidth) {
                    sampleSize *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                return BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size, decodeOptions)
            }
            return null
        } catch (e: OutOfMemoryError) {
            Log.e("ThumbnailExtractor", "OOM leyendo RAR")
            return null
        } catch (e: Exception) {
            return null
        } finally {
            try { archive?.close() } catch (e: Exception) { }
            try { randomAccessFile?.close() } catch (e: Exception) { }
        }
    }

    private fun extractFromPdf(comic: Comic): Bitmap? {
        val file = File(comic.path)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    val width = thumbnailWidth
                    val height = (width * page.height.toFloat() / page.width.toFloat()).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    private fun isImage(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif")
    }

    private fun hashId(id: String): String {
        return try {
            MessageDigest.getInstance("SHA-1")
                .digest(id.toByteArray())
                .joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            id.hashCode().toString()
        }
    }
}
