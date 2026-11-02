package com.example.moto_gallery

import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun FullScreenViewer(
    mediaItems: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaItems.size }
    )

    var imageIsZoomed by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        imageIsZoomed = false
    }

    BackHandler {
        onBack()
    }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !imageIsZoomed,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val mediaItem = mediaItems[page]

        MediaViewerItem(
            mediaItem = mediaItem,
            onZoomChanged = { zoomed ->
                if (page == pagerState.currentPage) {
                    imageIsZoomed = zoomed
                }
            }
        )
    }
}
@Composable
fun MediaViewerItem(
    mediaItem: MediaItem,
    onZoomChanged: (Boolean) -> Unit
) {
    if (mediaItem.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {

        VideoPlayer(
            mediaItem = mediaItem
        )

    } else {

        ImageViewer(
            mediaItem = mediaItem,
            onZoomChanged = onZoomChanged
        )
    }
}

@Composable
fun ImageViewer(
    mediaItem: MediaItem,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember(mediaItem.uri) {
        mutableFloatStateOf(1f)
    }

    var offsetX by remember(mediaItem.uri) {
        mutableFloatStateOf(0f)
    }

    var offsetY by remember(mediaItem.uri) {
        mutableFloatStateOf(0f)
    }

    val transformState = rememberTransformableState {
            zoomChange, panChange, _ ->

        val newScale =
            (scale * zoomChange).coerceIn(1f, 5f)

        scale = newScale

        if (scale > 1f) {

            offsetX += panChange.x
            offsetY += panChange.y

        } else {
            offsetX = 0f
            offsetY = 0f
        }
        onZoomChanged(scale > 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)

            .transformable(
                state = transformState,
                enabled = scale > 1f
            )
            .pointerInput(mediaItem.uri) {

                detectTapGestures(
                    onDoubleTap = {

                        if (scale == 1f) {

                            // Zoom into the image
                            scale = 2.5f

                            offsetX = 0f
                            offsetY = 0f

                            onZoomChanged(true)

                        } else {

                            // Return to normal size
                            scale = 1f

                            offsetX = 0f
                            offsetY = 0f

                            onZoomChanged(false)
                        }
                    }
                )
            }
    ) {
        coil3.compose.AsyncImage(
            model = mediaItem.uri,

            contentDescription = null,

            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {

                    // Apply the current zoom
                    scaleX = scale
                    scaleY = scale

                    // Panning
                    translationX = offsetX
                    translationY = offsetY
                },

            contentScale = ContentScale.Fit
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    mediaItem: MediaItem
) {
    val context = LocalContext.current
    val player = remember(mediaItem.uri) {

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
                controllerAutoShow = true
                controllerShowTimeoutMs = 2000
                controllerHideOnTouch = true

                setTimeBarScrubbingEnabled(true)

                setBackgroundColor(
                    android.graphics.Color.BLACK
                )

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