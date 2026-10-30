package com.example.moto_gallery

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

object ThumbnailLoader {

    //At most concurrency limit = 4
    val dispatcher = Dispatchers.IO.limitedParallelism(4)

    //Load the thumbnail if it isn't cached
    suspend fun load(
        contentResolver: ContentResolver,
        mediaItem: MediaItem
    ): Bitmap? {

        val cachedBitmap = ThumbnailCache.get(mediaItem.uri)

        if (cachedBitmap != null) {
            return cachedBitmap
        }

        //loadThumbnail() only exists on Android 10 and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }

        // Generate thumbnail away from the main thread
        return withContext(dispatcher) {

            try {

                val startTime = System.currentTimeMillis()

                //Ask Android for a thumbnail instead of decoding the original full-resolution media file.
                val bitmap = contentResolver.loadThumbnail(
                    mediaItem.uri,
                    Size(200,200),
                    null
                )

                val loadTime = System.currentTimeMillis() - startTime
                println("Thumbnail $loadTime ms")

                //Save thumbnail in ram for future use
                ThumbnailCache.put(
                    mediaItem.uri,
                    bitmap
                )
                bitmap

            } catch (e: Exception) {
                null
            }
        }
    }
}