package com.example.moto_gallery

import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun FullScreenViewer(
    mediaItems: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialIndex,
        pageCount = { mediaItems.size }
    )

    BackHandler {
        onBack()
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->

        val mediaItem = mediaItems[page]

        MediaViewerItem(
            mediaItem = mediaItem
        )
    }
}


@Composable
fun MediaViewerItem(
    mediaItem: MediaItem
) {
    if (mediaItem.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {

        VideoPlayer(mediaItem = mediaItem)

    } else {

        ImageViewer(mediaItem = mediaItem)
    }
}

@Composable
fun ImageViewer(
    mediaItem: com.example.moto_gallery.MediaItem
) {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = mediaItem.uri
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            value = withContext(Dispatchers.IO) {
                try {
                    // Load a larger image for the viewer.
                    context.contentResolver.loadThumbnail(
                        mediaItem.uri,
                        Size(1080, 1080),
                        null
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "Loading...",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun VideoPlayer(
    mediaItem: com.example.moto_gallery.MediaItem
) {

    val context = LocalContext.current

    val player = androidx.compose.runtime.remember(mediaItem.uri) {
        ExoPlayer.Builder(context).build().apply {

            setMediaItem(
                androidx.media3.common.MediaItem.Builder()
                    .setUri(mediaItem.uri)
                    .build()
            )

            prepare()

            playWhenReady = true
        }
    }

    AndroidView(
        factory = { context ->

            PlayerView(context).apply {

                this.player = player

                useController = true

                setBackgroundColor(android.graphics.Color.BLACK)

                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },

        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(player) {

        onDispose {
            player.release()
        }
    }
}