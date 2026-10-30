package com.example.moto_gallery

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache

object ThumbnailCache{

    // Max amount of memory this cache is allowed to use
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()

    private val cacheSizeKb = maxMemoryKb / 8

    // Cache entries are now measured by their memory instead of the count which was set as 100 before
    private val cache = object: LruCache<String, Bitmap>(cacheSizeKb) {

        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.allocationByteCount / 1024
        }
    }

    fun get(uri: Uri): Bitmap? {
        val bitmap = cache.get(uri.toString())

        if (bitmap != null){
            println("Thumbnail:HIT")
        } else{
            println("Thumbnail:MISS")
        }

        return bitmap
    }

    fun put(uri: Uri, bitmap: Bitmap){
        cache.put(uri.toString(), bitmap)
    }

}
