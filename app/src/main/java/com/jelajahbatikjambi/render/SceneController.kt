package com.jelajahbatikjambi.render

import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.Scene
import com.google.android.filament.gltfio.FilamentAsset

/**
 * Adds/removes a loaded [FilamentAsset]'s entities to the [Scene] and sets up
 * basic lighting so a model is visibly shaded without needing an
 * image-based-lighting environment (no .ktx assets exist yet — see §25).
 */
class SceneController(private val engine: Engine, private val scene: Scene) {

    private var currentAssetEntities: IntArray? = null
    private val lightEntities = mutableListOf<Int>()

    fun setupDefaultLighting() {
        // Key light.
        addDirectionalLight(
            directionX = -0.5f, directionY = -1f, directionZ = -0.3f,
            colorR = 1f, colorG = 0.98f, colorB = 0.92f,
            intensityLux = 110_000f
        )
        // Fill light, dimmer and from the opposite side, so shaded faces aren't pure black.
        addDirectionalLight(
            directionX = 0.6f, directionY = 0.3f, directionZ = 0.8f,
            colorR = 0.6f, colorG = 0.65f, colorB = 0.75f,
            intensityLux = 30_000f
        )
    }

    private fun addDirectionalLight(
        directionX: Float, directionY: Float, directionZ: Float,
        colorR: Float, colorG: Float, colorB: Float,
        intensityLux: Float
    ) {
        val entity = engine.entityManager.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .direction(directionX, directionY, directionZ)
            .color(colorR, colorG, colorB)
            .intensity(intensityLux)
            .castShadows(false)
            .build(engine, entity)
        scene.addEntity(entity)
        lightEntities.add(entity)
    }

    fun showAsset(asset: FilamentAsset) {
        hideCurrentAsset()
        val entities = asset.entities
        scene.addEntities(entities)
        currentAssetEntities = entities
    }

    fun hideCurrentAsset() {
        currentAssetEntities?.let { scene.removeEntities(it) }
        currentAssetEntities = null
    }

    fun destroy() {
        hideCurrentAsset()
        lightEntities.forEach { entity ->
            engine.lightManager.destroy(entity)
            engine.entityManager.destroy(entity)
        }
        lightEntities.clear()
    }
}
