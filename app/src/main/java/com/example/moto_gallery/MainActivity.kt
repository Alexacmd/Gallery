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
import androidx.compose.ui.Alignment
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

import androidx.compose.material3.Button

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_IMAGES
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // We'll handle the result here later
    }

    var photos by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }

    LaunchedEffect(hasPermission) {
        if(hasPermission) {
            photos = loadPhotos(context.contentResolver)
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
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                }
            ) {
                Text("Allow access to photos")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(photos) { photo ->
                PhotoItem(photo)
            }
        }
    }
}

@Composable
fun PhotoItem(photo: Uri) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(2.dp),
        Alignment.Center
    ) {
        AsyncImage(
            model = photo,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}