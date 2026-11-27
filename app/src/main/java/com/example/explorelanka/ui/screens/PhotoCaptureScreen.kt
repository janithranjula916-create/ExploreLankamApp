package com.example.explorelanka.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.explorelanka.data.DestinationData
import com.example.explorelanka.viewmodel.JournalViewModel

@Composable
fun PhotoCaptureScreen(
    userId: String,
    initialDestinationId: String,
    onBackClick: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val vm: JournalViewModel = viewModel()
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted = it }
    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    val capture = remember { ImageCapture.Builder().build() }
    var caption by rememberSaveable { mutableStateOf("") }
    var selectedPlace by rememberSaveable(initialDestinationId) {
        mutableStateOf(initialDestinationId.ifBlank {
            DestinationData.places.firstOrNull()?.id.orEmpty()
        })
    }
    var expanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Capture travel photo", style = MaterialTheme.typography.titleLarge)
        }
        if (permissionGranted) {
            CameraCapturePreview(
                imageCapture = capture,
                onError = { cameraError = it },
                modifier = Modifier.fillMaxWidth().height(320.dp)
            )
        } else {
            Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission is required to take photographs.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Text("Grant camera permission") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Destination: " + (DestinationData.findById(selectedPlace)?.name
                    ?: "Select destination"))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DestinationData.places.forEach { place ->
                    DropdownMenuItem(text = { Text(place.name) }, onClick = {
                        selectedPlace = place.id; expanded = false
                    })
                }
            }
        }
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it.take(240) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Caption (optional)") },
            maxLines = 3
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (selectedPlace.isBlank()) { cameraError = "Select a destination."; return@Button }
                busy = true
                val file = runCatching { vm.newCaptureFile(userId) }.getOrElse {
                    cameraError = it.message ?: "Cannot create photo file."; busy = false
                    return@Button
                }
                capture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(file).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            vm.saveCapture(userId, selectedPlace, caption, file, onSaved)
                        }
                        override fun onError(exception: ImageCaptureException) {
                            file.delete(); busy = false
                            cameraError = exception.message ?: "Unable to capture photo."
                        }
                    }
                )
            },
            enabled = permissionGranted && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (busy) "Saving..." else "Capture and save")
        }
        vm.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun CameraCapturePreview(
    imageCapture: ImageCapture,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestError by rememberUpdatedState(onError)
    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    DisposableEffect(lifecycleOwner, imageCapture, previewView) {
        var disposed = false
        var cameraProvider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!disposed) {
                try {
                    cameraProvider = future.get().also { provider ->
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                            preview, imageCapture
                        )
                    }
                } catch (exception: Exception) {
                    latestError(exception.message ?: "Unable to open the camera.")
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            cameraProvider?.unbindAll()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
