package com.mangauniversal.app.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * Extrae la primera imagen de un cómic y la guarda como miniatura.
 * Se cachea en: /data/data/com.mangauniversal.app/cache/thumbs/
 */
class ThumbnailExtractor(private val context: Context) {

    private val thumbsDir: File by lazy {
        File(context.cacheDir, "thumbs").apply { mkdirs() }
    }

    suspend fun getThumbnail(comic: Comic): File? = withContext(Dispatchers.IO) {
        try {
            val outFile = File(thumbsDir, "${comic.id.hashCode()}.jpg")

            if (outFile.exists() && outFile.length() > 0) {
                return@withContext outFile
            }

            val bitmap: Bitmap? = when (comic.type) {
                ComicType.FOLDER -> extractFromFolder(comic)
                ComicType.CBZ, ComicType.ZIP -> extractFromZip(comic)
                ComicType.PDF -> extractFromPdf(comic)
                else -> null
            } ?: return@withContext null

            val scaled = scaleBitmap(bitmap, 400)

            FileOutputStream(outFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            if (scaled != bitmap) scaled.recycle()
            bitmap.recycle()

            if (outFile.exists() && outFile.length() > 0) outFile else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFromFolder(comic: Comic): Bitmap? {
        val folder = File(comic.path)
        val image = folder.listFiles()
            ?.filter { it.isFile && isImage(it.name) }
            ?.minByOrNull { it.name.lowercase() }
            ?: return null
        return BitmapFactory.decodeFile(image.absolutePath)
    }

    private fun extractFromZip(comic: Comic): Bitmap? {
        val file = File(comic.path)
        ZipFile(file).use { zip ->
            val imageEntry = zip.entries().asSequence()
                .filter { !it.isDirectory && isImage(it.name) }
                .minByOrNull { it.name.lowercase() }
                ?: return null
            zip.getInputStream(imageEntry).use { input ->
                return BitmapFactory.decodeStream(input)
            }
        }
    }

    private fun extractFromPdf(comic: Comic): Bitmap? {
        val file = File(comic.path)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    val width = 400
                    val height = (width * page.height.toFloat() / page.width.toFloat()).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    private fun scaleBitmap(src: Bitmap, maxWidth: Int): Bitmap {
        if (src.width <= maxWidth) return src
        val ratio = maxWidth.toFloat() / src.width
        val newHeight = (src.height * ratio).toInt()
        return Bitmap.createScaledBitmap(src, maxWidth, newHeight, true)
    }

    private fun isImage(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif")
    }
}
