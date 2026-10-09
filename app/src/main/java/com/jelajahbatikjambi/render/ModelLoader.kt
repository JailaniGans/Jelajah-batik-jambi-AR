package com.jelajahbatikjambi.render

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "FILAMENT"
private const val SOURCE_TEXTURE_SIZE = 512

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

    /**
     * Source-photo textures uploaded at runtime, keyed by model path. The
     * [Texture] objects need to stay referenced for as long as the asset is
     * rendered (the material only holds a native handle), so they outlive
     * [loadFromAssets] here and are released together with the engine.
     */
    private val textureCache = mutableMapOf<String, Texture>()

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
        bindSourceTexture(asset, path)
        asset.releaseSourceData()
        return asset
    }

    /**
     * Binds the motif's stored photo directly onto the asset's materials,
     * bypassing gltfio's embedded-image decode entirely.
     *
     * Why: a custom cube's GLB is generated on-device with its texture
     * embedded as a JPEG buffer, and structurally that GLB is sound — yet on
     * some devices gltfio's embedded-image path (stb_image) fails to produce a
     * usable texture and the cube renders fully black even though the same
     * geometry/material layout renders fine otherwise. Uploading the texture
     * ourselves from the same source photo (`custom_images/{uuid}.jpg`, which
     * always exists next to `custom_models/{uuid}.glb`) makes rendering
     * independent of that brittle path — deterministically textured cubes for
     * freshly-added motifs AND for already-stored GLBs that still embed a
     * bad/black texture. The gltfio ubershader's base-color sampler is
     * `baseColorMap`, gated by `baseColorIndex` (> -1).
     *
     * Only applies to custom models (absolute paths); bundled assets keep
     * their embedded texture. Never throws — a failure here falls back to the
     * embedded texture (the pre-fix behaviour) instead of breaking AR.
     */
    private fun bindSourceTexture(asset: FilamentAsset, modelPath: String) {
        if (!modelPath.startsWith("/")) return
        val source = sourcePhotoFor(modelPath) ?: return
        val decoded = decodeForGpu(source) ?: return
        val cropped = TexturedCubeGlbGenerator.squareCenterCrop(decoded, SOURCE_TEXTURE_SIZE)
        try {
            val texture = uploadToGpu(cropped) ?: return
            textureCache[modelPath] = texture

            val sampler = TextureSampler(
                TextureSampler.MinFilter.LINEAR,
                TextureSampler.MagFilter.LINEAR,
                TextureSampler.WrapMode.CLAMP_TO_EDGE
            )
            val instances = runCatching { asset.getInstance().getMaterialInstances() }.getOrNull() ?: return
            var bound = 0
            for (instance in instances) {
                runCatching {
                    instance.setParameter("baseColorIndex", 0)
                    instance.setParameter("baseColorMap", texture, sampler)
                    bound++
                }.onFailure { Log.w(TAG, "Failed to bind source texture on a material instance", it) }
            }
            Log.i(TAG, "Bound source texture (${cropped.width}x${cropped.height}) on $bound material instance(s) for $modelPath")
        } finally {
            if (cropped !== decoded) cropped.recycle()
            decoded.recycle()
        }
    }

    /** `custom_models/{uuid}.glb` -> `custom_images/{uuid}.jpg` (same files dir). */
    private fun sourcePhotoFor(modelPath: String): File? {
        val modelFile = File(modelPath)
        val modelsDir = modelFile.parentFile ?: return null
        val imagesDir = File(modelsDir.parentFile ?: return null, "custom_images")
        val stem = modelFile.name.removeSuffix(".glb")
        val photo = File(imagesDir, "$stem.jpg")
        return if (photo.isFile) photo else {
            Log.w(TAG, "No source photo for $modelPath (expected $photo); keeping embedded texture")
            null
        }
    }

    /** Decodes the stored photo, downscaled, guaranteed ARGB_8888. */
    private fun decodeForGpu(photo: File): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.absolutePath, bounds)
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sample = 1
            while (maxDim / (sample * 2) >= SOURCE_TEXTURE_SIZE * 2) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(photo.absolutePath, opts)
                ?.copy(Bitmap.Config.ARGB_8888, false)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to decode source photo $photo", t)
            null
        }
    }

    /** Uploads the bitmap as a single-level RGBA8 texture. */
    private fun uploadToGpu(bitmap: Bitmap): Texture? {
        return try {
            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
            for (px in pixels) {
                buffer.put(((px shr 16) and 0xFF).toByte()) // R
                buffer.put(((px shr 8) and 0xFF).toByte())  // G
                buffer.put((px and 0xFF).toByte())          // B
                buffer.put(((px shr 24) and 0xFF).toByte()) // A
            }
            buffer.rewind()
            val texture = Texture.Builder()
                .width(w).height(h).levels(1)
                .sampler(Texture.Sampler.SAMPLER_2D)
                .format(Texture.InternalFormat.RGBA8)
                .build(engine)
            texture.setImage(engine, 0, Texture.PixelBufferDescriptor(buffer, Texture.Format.RGBA, Texture.Type.UBYTE))
            texture
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to upload source texture", t)
            null
        }
    }

    fun destroyAsset(asset: FilamentAsset) {
        assetLoader.destroyAsset(asset)
    }

    fun destroy() {
        textureCache.clear()
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
