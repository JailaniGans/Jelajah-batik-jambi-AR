package com.jelajahbatikjambi.render

import android.content.res.AssetManager
import com.google.android.filament.Engine
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Loads a GLB model into a [FilamentAsset] ready to add to a Filament
 * [com.google.android.filament.Scene]. [path] is either a bundled
 * `assets/models/` path, or — for a user-uploaded custom motif — an absolute
 * file path in internal storage; a path starting with "/" is treated as the
 * latter (see [com.jelajahbatikjambi.ui.common.AssetImage] for the same
 * convention applied to motif photos). One instance owns the AssetLoader +
 * ResourceLoader + MaterialProvider for the render surface's lifetime — see
 * [RenderLifecycle].
 */
class ModelLoader(private val engine: Engine) {

    private val materialProvider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materialProvider, engine.entityManager)
    private val resourceLoader = ResourceLoader(engine)

    fun loadFromAssets(assetManager: AssetManager, path: String): FilamentAsset {
        val bytes = if (path.startsWith("/")) {
            File(path).readBytes()
        } else {
            assetManager.open(path).use { it.readBytes() }
        }
        val buffer = ByteBuffer.allocateDirect(bytes.size)
            .order(ByteOrder.nativeOrder())
            .put(bytes)
            .apply { rewind() }

        val asset = assetLoader.createAsset(buffer)
            ?: error("Failed to parse GLB at $path")
        resourceLoader.loadResources(asset)
        asset.releaseSourceData()
        return asset
    }

    fun destroyAsset(asset: FilamentAsset) {
        assetLoader.destroyAsset(asset)
    }

    fun destroy() {
        resourceLoader.destroy()
        assetLoader.destroy()
        materialProvider.destroy()
    }

    companion object {
        /**
         * Must be called once before [ModelLoader] is used (e.g. app start).
         *
         * Throws [UnsatisfiedLinkError] if `libgltfio-jni.so` can't be loaded — call it
         * through [NativeSupport.initialize], never directly, so the failure is contained
         * instead of taking down the caller.
         */
        fun ensureNativeLibraryLoaded() {
            Gltfio.init()
        }
    }
}
