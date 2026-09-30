package com.jelajahbatikjambi.render

import android.view.Choreographer
import android.view.Surface
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.Filament
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.SwapChainFlags
import com.google.android.filament.View
import com.google.android.filament.Viewport

/**
 * Owns the core Filament objects (Engine, Renderer, View, Scene, Camera) and
 * drives the per-frame render loop via Choreographer. Free of any
 * Compose/Activity dependency — [RenderLifecycle] wires this to the Android
 * lifecycle and a rendering [Surface].
 *
 * The view is transparent (translucent blend mode + a fully transparent
 * clear color) so it composites over the CameraX preview behind it, and the
 * Filament camera stays at the world origin looking down -Z — this matches
 * [com.jelajahbatikjambi.ar.CoordinateConverter]'s render-space convention,
 * where a marker's position is already expressed relative to the phone
 * camera. No extra transform is needed to align the two.
 */
class FilamentRenderer {

    val engine: Engine = Engine.create()
    val scene: Scene = engine.createScene()
    val view: View = engine.createView()
    val renderer: Renderer = engine.createRenderer()

    private val cameraEntity = engine.entityManager.create()
    val camera: Camera = engine.createCamera(cameraEntity)

    private var swapChain: SwapChain? = null
    private var choreographer: Choreographer? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer?.postFrameCallback(this)
            renderFrame(frameTimeNanos)
        }
    }

    init {
        view.scene = scene
        view.camera = camera
        view.blendMode = View.BlendMode.TRANSLUCENT
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        }
        camera.lookAt(
            0.0, 0.0, 0.0,
            0.0, 0.0, -1.0,
            0.0, 1.0, 0.0
        )
    }

    fun attachSurface(surface: Surface, width: Int, height: Int) {
        swapChain = engine.createSwapChain(surface, SwapChainFlags.CONFIG_TRANSPARENT)
        setViewportSize(width, height)
        resumeRendering()
    }

    fun setViewportSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        view.viewport = Viewport(0, 0, width, height)
        val aspect = width.toDouble() / height.toDouble()
        camera.setProjection(60.0, aspect, 0.01, 30.0, Camera.Fov.VERTICAL)
    }

    fun resumeRendering() {
        if (choreographer != null) return
        choreographer = Choreographer.getInstance().also { it.postFrameCallback(frameCallback) }
    }

    fun pauseRendering() {
        choreographer?.removeFrameCallback(frameCallback)
        choreographer = null
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val chain = swapChain ?: return
        if (renderer.beginFrame(chain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun detachSurface() {
        pauseRendering()
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
    }

    fun destroy() {
        detachSurface()
        engine.destroyCameraComponent(cameraEntity)
        engine.entityManager.destroy(cameraEntity)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyRenderer(renderer)
        engine.destroy()
    }

    companion object {
        /** Must be called once before any Filament object is created (e.g. app start). */
        fun ensureNativeLibraryLoaded() {
            Filament.init()
        }
    }
}
