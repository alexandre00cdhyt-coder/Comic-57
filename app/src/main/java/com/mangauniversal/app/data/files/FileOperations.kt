package com.mangauniversal.app.data.files

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileOperations {

    suspend fun delete(target: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (target.isDirectory) {
                target.deleteRecursively()
            } else {
                target.delete()
            }
        } catch (e: Exception) {
            Log.e("FileOperations", "Error deleting ${target.path}", e)
            false
        }
    }

    suspend fun rename(target: File, newName: String): File? = withContext(Dispatchers.IO) {
        try {
            val newFile = File(target.parentFile, newName)
            if (target.renameTo(newFile)) newFile else null
        } catch (e: Exception) {
            Log.e("FileOperations", "Error renaming ${target.path}", e)
            null
        }
    }

    suspend fun copy(source: File, destinationDir: File): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destinationDir, source.name)
            if (source.isDirectory) {
                copyDirectory(source, destFile)
            } else {
                copyFile(source, destFile)
            }
            destFile
        } catch (e: Exception) {
            Log.e("FileOperations", "Error copying ${source.path}", e)
            null
        }
    }

    suspend fun move(source: File, destinationDir: File): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destinationDir, source.name)
            if (source.renameTo(destFile)) {
                destFile
            } else {
                // Si falla el rename (diferente partición), copiamos y borramos
                val copied = copy(source, destinationDir)
                if (copied != null) {
                    delete(source)
                    copied
                } else null
            }
        } catch (e: Exception) {
            Log.e("FileOperations", "Error moving ${source.path}", e)
            null
        }
    }

    private fun copyDirectory(source: File, dest: File) {
        if (!dest.exists()) dest.mkdirs()
        val files = source.listFiles() ?: return
        for (file in files) {
            val newFile = File(dest, file.name)
            if (file.isDirectory) {
                copyDirectory(file, newFile)
            } else {
                copyFile(file, newFile)
            }
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
