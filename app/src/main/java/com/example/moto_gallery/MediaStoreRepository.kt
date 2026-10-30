package com.example.moto_gallery

import android.content.ContentResolver
import android.provider.MediaStore
import android.net.Uri
data class MediaItem(
    val uri: Uri,
    val type: Int
)
fun loadMedia(contentResolver: ContentResolver): List<MediaItem> {
    val mediaList = mutableListOf<MediaItem>()

    val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE
    )

    val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " + "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"

    val selectionArgs = arrayOf(
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    )

    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

    contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

        while(cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val mediaType = cursor.getInt(typeColumn)
            val contentUri = Uri.withAppendedPath(collection, id.toString())
            //println("Media: uri=$contentUri type=$mediaType")
            mediaList.add(
                MediaItem(
                    uri = contentUri,
                    type = mediaType
                )
            )
        }
    }

    return mediaList

}