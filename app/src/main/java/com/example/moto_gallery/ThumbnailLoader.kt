package com.example.moto_gallery

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

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

            ThumbnailCache.get(mediaItem.uri)?.let { return@withContext it }

            val signal = CancellationSignal()
            val handle = coroutineContext[Job]!!.invokeOnCompletion { cause ->
                if (cause is CancellationException) signal.cancel()
            }
            try {
                contentResolver.loadThumbnail(mediaItem.uri, Size(320,320), signal)
                    .also { ThumbnailCache.put(mediaItem.uri, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            } finally {
                handle.dispose()
            }
        }
    }
}