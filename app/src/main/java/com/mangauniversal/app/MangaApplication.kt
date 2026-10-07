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
import coil.disk.DiskCache
import net.sf.sevenzipjbinding.SevenZip

class MangaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
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

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClass = activityManager.memoryClass
        val memoryCacheMb = (memoryClass * 0.25).toInt().coerceIn(100, 500)
        val memoryCacheBytes = (memoryCacheMb.toLong() * 1024L * 1024L).toInt()

        val diskCacheBytes = 512L * 1024L * 1024L

        return ImageLoader.Builder(context)
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizeBytes(memoryCacheBytes)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskCacheBytes)
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
