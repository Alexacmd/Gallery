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

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import coil3.compose.AsyncImage
import org.jetbrains.annotations.Async

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
            mediaItems = loadMedia(context.contentResolver)
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
            modifier = modifier.fillMaxSize()
        ) {
            items(mediaItems) { mediaItem ->
                PhotoItem(mediaItem,
                    onClick = {
                        println("Tapped: ${mediaItem.uri}")
                    })
            }
        }
    }
}

@Composable
fun PhotoItem(mediaItem: MediaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable {
                onClick()
            }
    ) {
        AsyncImage(
            model = mediaItem.uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (mediaItem.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = CircleShape
                    )
                    .padding(12.dp)

            ) {
                Text(
                    text = "▶",
                    color = Color.White
                )
            }
        }
    }
}

//@Composable
//fun PhotoItem(
//    mediaItem: MediaItem,
//    onClick: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .aspectRatio(1f)
//            .padding(2.dp)
//            .clickable {
//                // Report that this item was tapped.
//                onClick()
//            }
//    ) {
//        AsyncImage(
//            model = mediaItem.uri,
//            contentDescription = null,
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )
//
//        // Show the actual MediaStore type value on every item.
//        Text(
//            text = mediaItem.type.toString(),
//            color = Color.White,
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .background(Color.Black.copy(alpha = 0.8f))
//                .padding(4.dp)
//        )
//    }
//}