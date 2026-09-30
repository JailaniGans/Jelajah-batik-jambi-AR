package com.jelajahbatikjambi.ui.ar

import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jelajahbatikjambi.camera.CameraController

/**
 * Full-screen CameraX preview. Binding happens once per lifecycle owner via
 * [DisposableEffect] rather than in the AndroidView `update` block, so the
 * camera is not re-bound on every recomposition.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer,
    onCameraReady: (Camera) -> Unit = {},
    onControllerReady: (CameraController) -> Unit = {},
    onError: (Throwable) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraController = remember { CameraController(context) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { previewView }
    )

    LaunchedEffect(cameraController) {
        onControllerReady(cameraController)
    }

    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(
            lifecycleOwner = lifecycleOwner,
            surfaceProvider = previewView.surfaceProvider,
            analyzer = analyzer,
            onReady = onCameraReady,
            onError = onError
        )
        onDispose {
            cameraController.unbind()
        }
    }
}
