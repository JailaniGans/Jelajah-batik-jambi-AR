package com.jelajahbatikjambi.camera

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CAMERA"

// ArUco detection cost scales with frame size; 640x480 is small enough to
// process quickly and large enough to keep reading markers at a normal
// scanning distance. CLOSEST_HIGHER_THEN_LOWER picks the nearest resolution
// a device actually supports, still preferring something ≥640x480.
private val ANALYSIS_RESOLUTION = Size(640, 480)

/**
 * Owns the CameraX use-case bindings (Preview + ImageAnalysis).
 * Deliberately free of any Compose dependency so it can be reused by the
 * AR engine layer regardless of how the UI renders the preview surface.
 */
class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var analysisExecutor: ExecutorService? = null

    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        analyzer: ImageAnalysis.Analyzer,
        onReady: ((Camera) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .apply { setSurfaceProvider(surfaceProvider) }

                val executor = Executors.newSingleThreadExecutor()
                analysisExecutor = executor

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            ANALYSIS_RESOLUTION,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply { setAnalyzer(executor, analyzer) }

                provider.unbindAll()
                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                camera = boundCamera
                onReady?.invoke(boundCamera)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to bind camera use cases", t)
                onError?.invoke(t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun hasFlashUnit(): Boolean = camera?.cameraInfo?.hasFlashUnit() == true

    fun setTorchEnabled(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        analysisExecutor?.shutdown()
        analysisExecutor = null
        camera = null
        cameraProvider = null
    }
}
