package com.jelajahbatikjambi.ui.ar

import android.graphics.PixelFormat
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jelajahbatikjambi.ar.Pose
import com.jelajahbatikjambi.ar.Quaternion
import com.jelajahbatikjambi.ar.Vector3
import com.jelajahbatikjambi.render.NativeSupport
import com.jelajahbatikjambi.render.RenderLifecycle

private const val TAG = "FILAMENT"

/**
 * Transparent Filament rendering surface, layered above [CameraPreview] in
 * [ArScreen] so the 3D content composites over the live camera feed.
 * `setZOrderOnTop` is required here: a lone SurfaceView otherwise renders
 * behind the rest of the window, including the CameraX preview beneath it.
 *
 * [modelPath] (which motif's GLB — see [com.jelajahbatikjambi.data.model.BatikData.modelPath])
 * and [pose] are forwarded to [RenderLifecycle.updateTarget] on every
 * recomposition via [SideEffect] rather than [androidx.compose.runtime.LaunchedEffect]:
 * this runs at camera-analysis frame rate, and SideEffect is the plain
 * "publish this value to non-Compose code" hook — no coroutine relaunch overhead.
 *
 * If Filament itself fails to initialize (e.g. no usable GPU driver — §33),
 * this renders nothing rather than crashing: marker detection and the info
 * panel keep working without the 3D overlay. Two independent guards cover
 * that: [NativeSupport.isAvailable] short-circuits when the native libraries
 * themselves wouldn't load (checked at process start, so no [android.util.Log]
 * noise per composition), and [runCatching] below catches a driver/engine
 * failure at construction time. [ArScreen] pairs this with a visible notice
 * so the missing overlay is explained rather than just silent.
 *
 * [userRotation] (§27 3D interaction — drag to rotate), [userOffset]
 * (§ user request — drag-and-drop) and [userScale] (§ user request — pinch
 * to zoom) are all composed on top of the tracked pose rather than replacing
 * it; see [RenderLifecycle.updateTarget].
 */
@Composable
fun FilamentView(
    modelPath: String?,
    pose: Pose?,
    userRotation: Quaternion = Quaternion.IDENTITY,
    userOffset: Vector3 = Vector3.ZERO,
    userScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    // Read before any Filament type is touched: constructing RenderLifecycle
    // calls Filament.init() transitively and throws if the .so files are absent.
    if (!NativeSupport.isAvailable) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val renderLifecycle = remember {
        runCatching { RenderLifecycle(context.assets) }
            .onFailure { Log.e(TAG, "Failed to initialize Filament renderer", it) }
            .getOrNull()
    } ?: return

    val surfaceView = remember {
        SurfaceView(context).apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSLUCENT)
            holder.addCallback(renderLifecycle)
        }
    }

    AndroidView(modifier = modifier.fillMaxSize(), factory = { surfaceView })

    SideEffect {
        renderLifecycle.updateTarget(modelPath, pose, userRotation, userOffset, userScale)
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(renderLifecycle)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(renderLifecycle)
            surfaceView.holder.removeCallback(renderLifecycle)
            renderLifecycle.onDestroy(lifecycleOwner)
        }
    }
}
