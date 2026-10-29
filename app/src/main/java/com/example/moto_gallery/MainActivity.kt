package com.example.moto_gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moto_gallery.ui.theme.Moto_GalleryTheme
import androidx.compose.foundation.layout.Column

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import android.provider.MediaStore
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.size

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Icon

import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Moto_GalleryTheme {
                Scaffold(modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    MotoGalleryApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MotoGalleryApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val hasImagePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_IMAGES
    ) == PackageManager.PERMISSION_GRANTED

    val hasVideoPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_VIDEO
    ) == PackageManager.PERMISSION_GRANTED

    val hasPermission = hasImagePermission && hasVideoPermission

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permission ->
        // We'll handle the result here later
    }

    var mediaItems by remember {
        mutableStateOf<List<MediaItem>>(emptyList())
    }

    LaunchedEffect(hasPermission) {
        if(hasPermission) {
            val startTime = System.currentTimeMillis()

            mediaItems = loadMedia(context.contentResolver)

            val loadTime = System.currentTimeMillis() - startTime

            println("MotoGallery: loaded ${mediaItems.size} items in ${loadTime} ms")

        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (hasPermission) {
            Text("Permission granted")
        } else {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                    )
                }
            ) {
                Text("Allow access to photos")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(
                items = mediaItems,
                key = { mediaItems ->
                    mediaItems.uri.toString()
                }
            ) { mediaItem ->
                PhotoItem(mediaItem,
                    onClick = {
                        println("Tapped: ${mediaItem.uri}")
                    }
                )
            }
        }
    }
}

@Composable
fun PhotoItem(
    mediaItem: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Load the thumbnail away from the main UI thread.
    val thumbnail by produceState<Bitmap?>(
        initialValue = null,
        key1 = mediaItem.uri
    ) {
        value = withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // Ask Android for a thumbnail instead of decoding the
                    // original full-resolution media file.
                    context.contentResolver.loadThumbnail(
                        mediaItem.uri,
                        Size(300, 300),
                        null
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable {
                onClick()
            }
    ) {

        if (thumbnail != null) {
            // Display the already-decoded small thumbnail.
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Show a play indicator for videos.
        if (mediaItem.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            Icon(
                painter = painterResource(R.drawable.ic_play_circle),
                contentDescription = "Play",
                tint = Color.LightGray.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
    }
}