package com.example.moto_gallery

import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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

    BackHandler { onBack() }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        MediaViewerItem(mediaItem = mediaItems[page])
    }
}

@Composable
fun MediaViewerItem(mediaItem: MediaItem) {
    if (mediaItem.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
        VideoPlayer(mediaItem = mediaItem)
    } else {
        ImageViewer(mediaItem = mediaItem)
    }
}

@Composable
fun ImageViewer(mediaItem: MediaItem) {
    var scale by remember(mediaItem.uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(mediaItem.uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(mediaItem.uri) { mutableFloatStateOf(0f) }

    var boxWidth by remember { mutableFloatStateOf(0f) }
    var boxHeight by remember { mutableFloatStateOf(0f) }

    // Intrinsic size of the loaded image (0 until loaded)
    var imageW by remember(mediaItem.uri) { mutableFloatStateOf(0f) }
    var imageH by remember(mediaItem.uri) { mutableFloatStateOf(0f) }

    // Size of the image as actually drawn with ContentScale.Fit (at scale 1)
    fun fittedSize(): Pair<Float, Float> {
        if (imageW <= 0f || imageH <= 0f || boxWidth <= 0f || boxHeight <= 0f) {
            return Pair(boxWidth, boxHeight)
        }
        val ratio = min(boxWidth / imageW, boxHeight / imageH)
        return Pair(imageW * ratio, imageH * ratio)
    }

    fun maxOffsets(s: Float): Pair<Float, Float> {
        val (fw, fh) = fittedSize()
        val maxX = max(0f, (fw * s - boxWidth) / 2f)
        val maxY = max(0f, (fh * s - boxHeight) / 2f)
        return Pair(maxX, maxY)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged {
                boxWidth = it.width.toFloat()
                boxHeight = it.height.toFloat()
            }

            .pointerInput(mediaItem.uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val alreadyConsumed = event.changes.any { it.isConsumed }
                        if (!alreadyConsumed) {
                            val pointers = event.changes.count { it.pressed }
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (pointers >= 2) {
                                val oldScale = scale
                                val newScale = (oldScale * zoom).coerceIn(1f, 5f)
                                val centroid = event.changes
                                    .filter { it.pressed }
                                    .map { it.position }
                                    .let { pts ->
                                        Offset(
                                            pts.sumOf { it.x.toDouble() }.toFloat() / pts.size,
                                            pts.sumOf { it.y.toDouble() }.toFloat() / pts.size
                                        )
                                    }
                                val cx = centroid.x - boxWidth / 2f
                                val cy = centroid.y - boxHeight / 2f
                                val factor = newScale / oldScale

                                // Keep the point under the fingers fixed, then apply pan
                                val rawX = cx - (cx - offsetX) * factor + pan.x
                                val rawY = cy - (cy - offsetY) * factor + pan.y

                                scale = newScale
                                val (maxX, maxY) = maxOffsets(newScale)
                                offsetX = rawX.coerceIn(-maxX, maxX)
                                offsetY = rawY.coerceIn(-maxY, maxY)

                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (scale > 1f) {
                                // One finger while zoomed: pan, but let the pager have it at edges
                                val (maxX, maxY) = maxOffsets(scale)
                                val newX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                val newY = (offsetY + pan.y).coerceIn(-maxY, maxY)

                                val movedX = newX != offsetX
                                val movedY = newY != offsetY && abs(pan.y) > abs(pan.x)

                                offsetX = newX
                                offsetY = newY

                                if (movedX || movedY) {
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        }
                    } while (!alreadyConsumed && event.changes.any { it.pressed })
                }
            }
            .pointerInput(mediaItem.uri) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale == 1f) {
                            scale = 2.5f
                        } else {
                            scale = 1f
                        }
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
    ) {
        AsyncImage(
            model = mediaItem.uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                if (size.isSpecified) {
                    imageW = size.width
                    imageH = size.height
                }
            }
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