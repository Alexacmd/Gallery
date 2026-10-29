package com.example.moto_gallery

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache

object ThumbnailCache{

    private val cache = LruCache<String, Bitmap>(100)

    fun get(uri: Uri): Bitmap?{
        val bitmap = cache.get(uri.toString())

        if (bitmap != null){
            println("Thumbnail:HIT")
        } else{
            println("Thumbnail:MISS")
        }

        return bitmap
//      return cache.get(uri.toString())
    }

    fun put(uri: Uri, bitmap: Bitmap){
        cache.put(uri.toString(), bitmap)
    }

}
