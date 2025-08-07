package com.swapna.fishidentificationapp

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import android.widget.Toast


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FishIdentificationApp()
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishIdentificationApp() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val classifier = remember { FishClassifier(context) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { capturedBitmap: Bitmap? ->
        capturedBitmap?.let {
            bitmap = it
            loading = true
            result = try {
                classifier.classify(it)
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            loading = false
        }
    }

    // Runtime camera permission
    val cameraPermission = android.Manifest.permission.CAMERA
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            android.widget.Toast.makeText(context, "Camera permission is required", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // When image is selected from gallery
    LaunchedEffect(imageUri) {
        imageUri?.let { uri ->
            loading = true
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()

            val resultDrawable = (loader.execute(request) as? SuccessResult)?.drawable
            resultDrawable?.let { drawable ->
                val bmp = drawable.toBitmap()
                bitmap = bmp
                try {
                    result = classifier.classify(bmp)
                } catch (e: Exception) {
                    result = "Error: ${e.message}"
                }
            }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Fish Identification") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Select Image")
                }
                Button(onClick = {
                    permissionLauncher.launch(cameraPermission)
                }) {
                    Text("Take Photo")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                loading -> CircularProgressIndicator()
                result.isNotEmpty() -> Text("Prediction: $result", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
