package com.example.explorelanka.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Only trust our application-specific deep-link format, never arbitrary URLs. */
private fun validatedDestinationId(value: String): String? {
    val uri = runCatching { Uri.parse(value.trim()) }.getOrNull() ?: return null
    if (uri.scheme != "explorelanka" || uri.host != "place") return null
    if (uri.pathSegments.size != 1 || uri.query != null || uri.fragment != null) return null
    val id = uri.pathSegments.first()
    return id.takeIf { DestinationData.findById(it) != null }
}

@Composable
fun QrScannerScreen(
    userId: String,
    onBackClick: () -> Unit,
    onDestinationClick: (String) -> Unit
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
    var status by remember { mutableStateOf("Point your camera at an Explore Lanka QR code.") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Scan destination QR", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(8.dp))
        if (permissionGranted) {
            QrCameraPreview(
                onDecoded = { text ->
                    val id = validatedDestinationId(text)
                    if (id == null) {
                        status = "Invalid or unknown destination QR code."
                        false
                    } else {
                        status = "Opening ${DestinationData.findById(id)?.name ?: "destination"}"
                        vm.recordScan(userId, id) { onDestinationClick(id) }
                        true
                    }
                },
                onError = { status = it },
                modifier = Modifier.fillMaxWidth().height(400.dp)
            )
        } else {
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null,
                        modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Camera permission is needed to scan QR codes.")
                    OutlinedButton(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Text("Grant camera permission") }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("Example: explorelanka://place/sigiriya",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        vm.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private class QrAnalyzer(
    private val scanner: BarcodeScanner,
    private val mainExecutor: java.util.concurrent.Executor,
    private val onDecoded: (String) -> Boolean
) : ImageAnalysis.Analyzer {
    private val working = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (finished.get() || !working.compareAndSet(false, true)) {
            image.close()
            return
        }
        val mediaImage = image.image
        if (mediaImage == null) {
            working.set(false)
            image.close()
            return
        }
        try {
            val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            scanner.process(input)
            .addOnSuccessListener(mainExecutor) { barcodes ->
                if (!finished.get()) {
                    barcodes.firstNotNullOfOrNull { it.rawValue }?.let { text ->
                        if (onDecoded(text)) finished.set(true)
                    }
                }
            }
            .addOnFailureListener(mainExecutor) { /* keep scanning next frame */ }
            .addOnCompleteListener(mainExecutor) {
                working.set(false)
                image.close()
            }
        } catch (exception: Exception) {
            working.set(false)
            image.close()
        }
    }
}

@Composable
private fun QrCameraPreview(
    onDecoded: (String) -> Boolean,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentDecode by rememberUpdatedState(onDecoded)
    val currentError by rememberUpdatedState(onError)
    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    DisposableEffect(lifecycleOwner, previewView) {
        val executor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor, QrAnalyzer(scanner, mainExecutor) { currentDecode(it) })
        var provider: ProcessCameraProvider? = null
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!disposed) {
                try {
                    provider = future.get().also { cameraProvider ->
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    }
                } catch (exception: Exception) {
                    currentError(exception.message ?: "Unable to start QR camera.")
                }
            }
        }, mainExecutor)
        onDispose {
            disposed = true
            analysis.clearAnalyzer()
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
