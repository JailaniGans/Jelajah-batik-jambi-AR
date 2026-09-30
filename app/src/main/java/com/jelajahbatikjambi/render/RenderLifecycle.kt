package com.jelajahbatikjambi.render

import android.content.res.AssetManager
import android.util.Log
import android.view.SurfaceHolder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.gltfio.FilamentAsset
import com.jelajahbatikjambi.ar.Pose
import com.jelajahbatikjambi.ar.Quaternion
import com.jelajahbatikjambi.ar.Vector3
import com.jelajahbatikjambi.ar.plus
import com.jelajahbatikjambi.ar.times

private const val TAG = "FILAMENT"
private const val SCALE_IN_DURATION_NANOS = 250_000_000L
private const val SCALE_IN_START = 0.8f
private const val SCALE_IN_END = 1f

/**
 * Wires [FilamentRenderer] to a [SurfaceHolder] and the Android Lifecycle:
 * the render surface attaches/detaches with the SurfaceView's own surface,
 * and the frame loop pauses while the app isn't in the foreground (§53).
 *
 * Also owns model/scene setup. Each Batik motif has its own textured cube
 * GLB ([com.jelajahbatikjambi.data.model.BatikData.modelPath]) rather than a
 * single shared placeholder, so [updateTarget] loads models lazily as each
 * motif is first detected and keeps them cached by path — switching back to
 * a previously-seen motif doesn't re-decode its GLB. [modelPath] or [pose]
 * being null means nothing is tracked: the current model is removed from the
 * scene entirely rather than left sitting at a stale transform (§26).
 * Newly-shown models ease in from 0.8x to 1.0x scale over ~250ms (§49)
 * rather than snapping to full size.
 *
 * Failures here are logged and swallowed rather than crashing the app
 * (§33) — losing the 3D overlay still leaves marker detection and the info
 * panel working.
 */
class RenderLifecycle(private val assetManager: AssetManager) : SurfaceHolder.Callback, DefaultLifecycleObserver {

    private val renderer = FilamentRenderer()
    private val modelLoader = ModelLoader(renderer.engine)
    private val sceneController = SceneController(renderer.engine, renderer.scene)
    private val transformController = TransformController(renderer.engine)

    private val assetCache = mutableMapOf<String, FilamentAsset>()
    private var currentModelPath: String? = null
    private var modelVisible = false
    private var visibleSinceNanos = 0L
    private var surfaceReady = false
    private var destroyed = false
    private var updateErrorLogged = false

    init {
        sceneController.setupDefaultLighting()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            val frame = holder.surfaceFrame
            renderer.attachSurface(holder.surface, frame.width(), frame.height())
            surfaceReady = true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start Filament rendering", t)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer.setViewportSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        renderer.detachSurface()
    }

    override fun onStart(owner: LifecycleOwner) {
        renderer.resumeRendering()
    }

    override fun onStop(owner: LifecycleOwner) {
        renderer.pauseRendering()
    }

    /**
     * [modelPath] identifies which motif's GLB to show (from
     * [com.jelajahbatikjambi.data.model.BatikData.modelPath]); [pose] is its
     * live tracked transform. Either being null hides whatever is currently
     * shown. [userRotation] (§27 — drag-to-rotate) is composed on top of the
     * tracked rotation in world space (`userRotation * pose.rotation`) rather
     * than replacing it: the object still follows the marker, but the user
     * can additionally spin it to see other sides. [userOffset] (§ user
     * request — drag-and-drop) is likewise added on top of the tracked
     * position, and [userScale] (§ user request — pinch to zoom) multiplies
     * the intro scale-in factor. All three default to their identity values
     * (no manual adjustment applied).
     *
     * Called on every emission from [com.jelajahbatikjambi.ar.ArController.pose],
     * so the common case (same model, new pose) must stay cheap.
     */
    fun updateTarget(
        modelPath: String?,
        pose: Pose?,
        userRotation: Quaternion = Quaternion.IDENTITY,
        userOffset: Vector3 = Vector3.ZERO,
        userScale: Float = 1f
    ) {
        try {
            applyTarget(modelPath, pose, userRotation, userOffset, userScale)
        } catch (t: Throwable) {
            if (!updateErrorLogged) {
                Log.e(TAG, "Failed to update tracked model", t)
                updateErrorLogged = true
            }
        }
    }

    private fun applyTarget(modelPath: String?, pose: Pose?, userRotation: Quaternion, userOffset: Vector3, userScale: Float) {
        if (!surfaceReady || modelPath == null || pose == null) {
            if (modelVisible) {
                sceneController.hideCurrentAsset()
                modelVisible = false
            }
            return
        }

        if (modelPath != currentModelPath) {
            if (modelVisible) sceneController.hideCurrentAsset()
            val asset = assetCache.getOrPut(modelPath) { modelLoader.loadFromAssets(assetManager, modelPath) }
            sceneController.showAsset(asset)
            currentModelPath = modelPath
            modelVisible = true
            visibleSinceNanos = System.nanoTime()
        }

        val asset = assetCache.getValue(modelPath)
        val combinedRotation = userRotation * pose.rotation
        val combinedPosition = pose.position + userOffset
        transformController.setTransform(
            entity = asset.root,
            positionX = combinedPosition.x, positionY = combinedPosition.y, positionZ = combinedPosition.z,
            rotationX = combinedRotation.x, rotationY = combinedRotation.y, rotationZ = combinedRotation.z,
            rotationW = combinedRotation.w,
            scale = currentScaleInFactor() * userScale
        )
    }

    private fun currentScaleInFactor(): Float {
        val elapsedNanos = System.nanoTime() - visibleSinceNanos
        val progress = (elapsedNanos.toFloat() / SCALE_IN_DURATION_NANOS).coerceIn(0f, 1f)
        val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress) // ease-out cubic
        return SCALE_IN_START + (SCALE_IN_END - SCALE_IN_START) * eased
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (destroyed) return
        destroyed = true
        try {
            assetCache.values.forEach { modelLoader.destroyAsset(it) }
            assetCache.clear()
            sceneController.destroy()
            modelLoader.destroy()
            renderer.destroy()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to release Filament resources", t)
        }
    }
}
