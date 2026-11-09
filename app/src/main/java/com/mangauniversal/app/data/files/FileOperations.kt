package com.mangauniversal.app.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType
import com.mangauniversal.app.data.reader.PageExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileOperations {

    suspend fun delete(target: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (target.isDirectory) target.deleteRecursively() else target.delete()
        } catch (e: Exception) { false }
    }

    suspend fun rename(target: File, newName: String): File? = withContext(Dispatchers.IO) {
        try {
            val newFile = File(target.parentFile, newName)
            if (target.renameTo(newFile)) newFile else null
        } catch (e: Exception) { null }
    }

    suspend fun copy(source: File, destinationDir: File): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destinationDir, source.name)
            if (source.isDirectory) copyDirectory(source, destFile) else copyFile(source, destFile)
            destFile
        } catch (e: Exception) { null }
    }

    suspend fun move(source: File, destinationDir: File): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destinationDir, source.name)
            if (source.renameTo(destFile)) {
                destFile
            } else {
                val copied = copy(source, destinationDir)
                if (copied != null) { delete(source); copied } else null
            }
        } catch (e: Exception) { null }
    }

    suspend fun extractImages(
        context: Context,
        comic: Comic,
        calidad: Int = 100,
        reducirTamaño: Boolean = false,
        onProgress: (Int, Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val comicFile = File(comic.path)
            val baseName = comicFile.nameWithoutExtension
            val destFolder = File(comicFile.parentFile, "${baseName}_extraido")
            
            if (!destFolder.exists()) destFolder.mkdirs()

            val extractor = PageExtractor(context)
            
            if (comic.type == ComicType.PDF) {
                val pageCount = extractor.getPdfPageCount(comicFile)
                ParcelFileDescriptor.open(comicFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (i in 0 until pageCount) {
                            renderer.openPage(i).use { page ->
                                val width = if (reducirTamaño) 1200 else 2400
                                val height = (width * page.height.toFloat() / page.width.toFloat()).toInt()
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                
                                val destFile = File(destFolder, String.format("%04d.jpg", i + 1))
                                FileOutputStream(destFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, out)
                                }
                                bitmap.recycle()
                            }
                            onProgress(i + 1, pageCount)
                        }
                    }
                }
            } else {
                val pages = extractor.getPages(comic)
                val total = pages.size
                for ((index, pageFile) in pages.withIndex()) {
                    if (pageFile.exists()) {
                        val destFile = File(destFolder, String.format("%04d_%s", index + 1, pageFile.name))
                        
                        if (calidad < 100 || reducirTamaño) {
                            val options = BitmapFactory.Options().apply { if (reducirTamaño) inSampleSize = 2 }
                            val bitmap = BitmapFactory.decodeFile(pageFile.absolutePath, options)
                            if (bitmap != null) {
                                FileOutputStream(destFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, out)
                                }
                                bitmap.recycle()
                            } else {
                                copyFile(pageFile, destFile)
                            }
                        } else {
                            copyFile(pageFile, destFile)
                        }
                    }
                    onProgress(index + 1, total)
                }
            }
            destFolder
        } catch (e: Exception) {
            Log.e("FileOperations", "Error extracting images", e)
            null
        }
    }
        private fun copyDirectory(source: File, dest: File) {
        if (!dest.exists()) dest.mkdirs()
        val files = source.listFiles() ?: return
        for (file in files) {
            val newFile = File(dest, file.name)
            if (file.isDirectory) copyDirectory(file, newFile) else copyFile(file, newFile)
        }
    }

    private fun copyFile(source: File, dest: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }
}
