package com.jelajahbatikjambi.render

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Read-only inspection of the texture sampler inside a stored GLB, used by
 * [com.jelajahbatikjambi.data.repository.MotifRepository.repairLegacyCustomGlbs]
 * to spot custom cubes that must be regenerated.
 *
 * Root cause of the black-cube bug (§ user request — "objek 3d nya berwarna
 * hitam"): the first on-device generator wrote `minFilter 9987`
 * (LINEAR_MIPMAP_LINEAR). gltfio's embedded-texture path does not generate
 * mipmaps, so a mip-requiring sampler samples a missing level and Filament
 * renders the cube *black* — confirmed on-device (an A/B test proved the
 * exact same GLB renders the motif as soon as minFilter is flattened to a
 * single level, 9729). The generator was fixed to emit
 * [TexturedCubeGlbGenerator]'s single-level sampler; this object finds the
 * leftover files so they can be rewritten from the stored source photo.
 */
object GlbSampler {

    /** "glTF" magic as little-endian uint32, same constant style as the generator. */
    private const val MAGIC_GLTF = 0x46546C67
    private const val CHUNK_TYPE_JSON = 0x4E4F534A

    /** glTF minFilter values that demand mipmaps (NEAREST_MIPMAP_NEAREST .. LINEAR_MIPMAP_LINEAR). */
    private const val FIRST_MIPMAP_FILTER = 9984

    /**
     * Reads the first texture sampler's `minFilter` from the GLB's JSON
     * chunk. Returns null when the file can't be parsed (too short, bad
     * magic, no JSON chunk, malformed JSON) or declares no sampler at all.
     */
    fun minFilterOrNull(glb: ByteArray): Int? {
        val json = jsonChunkOrNull(glb) ?: return null
        val root = runCatching { JSONObject(String(json)) }.getOrNull() ?: return null
        val samplers = root.optJSONArray("samplers") ?: return null
        if (samplers.length() == 0) return null
        val sampler = samplers.optJSONObject(0) ?: return null
        return if (sampler.has("minFilter")) sampler.getInt("minFilter") else null
    }

    /**
     * Whether a stored custom GLB should be regenerated: unreadable/missing
     * file, unparseable GLB, no sampler, or a mipmapped minFilter (the old
     * generator's signature; see class KDoc). Single-level filters
     * (9728 NEAREST / 9729 LINEAR) are the fixed generator's output and are
     * kept untouched.
     */
    fun requiresRegeneration(glb: ByteArray?): Boolean {
        val filter = glb?.let { minFilterOrNull(it) } ?: return true
        return filter >= FIRST_MIPMAP_FILTER
    }

    private fun jsonChunkOrNull(glb: ByteArray): ByteArray? {
        if (glb.size < 20) return null
        val buffer = ByteBuffer.wrap(glb).order(ByteOrder.LITTLE_ENDIAN)
        if (buffer.int != MAGIC_GLTF) return null
        buffer.int // version
        buffer.int // total length (not cross-checked; chunk lengths are authoritative)
        val jsonLength = buffer.int
        val jsonChunkType = buffer.int
        if (jsonChunkType != CHUNK_TYPE_JSON) return null
        if (buffer.remaining() < jsonLength) return null
        val json = ByteArray(jsonLength)
        buffer.get(json)
        return json
    }
}