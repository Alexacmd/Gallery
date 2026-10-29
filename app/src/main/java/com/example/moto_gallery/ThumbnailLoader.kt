package com.example.moto_gallery

import kotlinx.coroutines.Dispatchers

object ThumbnailLoader {
    val dispatcher = Dispatchers.IO.limitedParallelism(4)
}