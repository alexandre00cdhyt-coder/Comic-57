package com.mangauniversal.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import net.sf.sevenzipjbinding.SevenZip

class MangaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Inicializar sevenzipjbinding para poder leer RAR 4/5 y 7Z
        try {
            SevenZip.initSevenZipFromPlatformJAR()
            Log.d("MangaUniversal", "sevenzipjbinding inicializado correctamente")
        } catch (e: Throwable) {
            Log.e("MangaUniversal", "Error inicializando sevenzipjbinding: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun newImageLoader(): ImageLoader {
        val context = this

        // Calcular el tamaño de caché basado en la RAM del dispositivo
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClass = activityManager.memoryClass  // en MB
        val cacheSizeMb = (memoryClass * 0.15).toInt().coerceIn(100, 400)
        val cacheSizeBytes = cacheSizeMb.toLong() * 1024L * 1024L

        return ImageLoader.Builder(context)
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizeBytes(cacheSizeBytes.toInt())
                    .build()
            }
            .crossfade(false)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}
