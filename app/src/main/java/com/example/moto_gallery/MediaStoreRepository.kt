package com.example.moto_gallery

import android.content.ContentResolver
import android.provider.MediaStore
import android.net.Uri

fun loadPhotos(contentResolver: ContentResolver): List<Uri> {
    val photos = mutableListOf<Uri>()

    val collection =
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    val projection = arrayOf(
        MediaStore.Images.Media._ID
    )

    val sortOrder =
        "${MediaStore.Images.Media.DATE_ADDED} DESC"

    contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->

        val idColumn = cursor.getColumnIndexOrThrow(
            MediaStore.Images.Media._ID
        )

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)

            val contentUri = Uri.withAppendedPath(
                collection,
                id.toString()
            )

            photos.add(contentUri)
        }
    }

    return photos
}