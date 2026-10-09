package com.jelajahbatikjambi.render

import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds a textured cube GLB entirely on-device, for user-uploaded custom
 * motifs (§ user request — "tambahkan motif dengan upload .jpg") that have
 * no bundled asset to fall back on. Mirrors the offline Python generator used
 * to pre-bake the 4 built-in motif cubes (identical cube geometry and UVs,
 * and the same material) so a custom motif's 3D object looks and behaves
 * exactly like a built-in one — with one deliberate fix: the texture
 * sampler uses a single level (no mipmaps), since a mipmapped sampler on
 * gltfio's embedded-texture path rendered the cube black on-device.
 */
object TexturedCubeGlbGenerator {

    private const val HALF = 0.025f // 5cm cube, matches the built-in motif cubes
    private const val TEXTURE_SIZE = 512
    private const val JPEG_QUALITY = 85

    // "glTF", "JSON", "BIN\0" read as little-endian uint32 — the same bytes a
    // Python `struct.pack("<I4s", ...)` would write, expressed as an int so
    // java.nio.ByteBuffer can emit them in one putInt call.
    private const val MAGIC_GLTF = 0x46546C67
    private const val CHUNK_TYPE_JSON = 0x4E4F534A
    private const val CHUNK_TYPE_BIN = 0x004E4942

    // Each face: 4 corner positions (CCW winding viewed from outside) + its normal.
    private val FACES: List<Pair<List<FloatArray>, FloatArray>> = listOf(
        listOf(f(1, -1, -1), f(1, 1, -1), f(1, 1, 1), f(1, -1, 1)) to f(1, 0, 0),
        listOf(f(-1, -1, 1), f(-1, 1, 1), f(-1, 1, -1), f(-1, -1, -1)) to f(-1, 0, 0),
        listOf(f(-1, 1, -1), f(-1, 1, 1), f(1, 1, 1), f(1, 1, -1)) to f(0, 1, 0),
        listOf(f(-1, -1, 1), f(-1, -1, -1), f(1, -1, -1), f(1, -1, 1)) to f(0, -1, 0),
        listOf(f(-1, -1, 1), f(1, -1, 1), f(1, 1, 1), f(-1, 1, 1)) to f(0, 0, 1),
        listOf(f(1, -1, -1), f(-1, -1, -1), f(-1, 1, -1), f(1, 1, -1)) to f(0, 0, -1)
    )
    private val FACE_UV = listOf(f2(0, 1), f2(1, 1), f2(1, 0), f2(0, 0))

    private fun f(x: Int, y: Int, z: Int) = floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())
    private fun f2(x: Int, y: Int) = floatArrayOf(x.toFloat(), y.toFloat())

    fun generate(sourceBitmap: Bitmap): ByteArray {
        val positions = mutableListOf<FloatArray>()
        val normals = mutableListOf<FloatArray>()
        val uvs = mutableListOf<FloatArray>()
        val indices = mutableListOf<Int>()

        for ((corners, normal) in FACES) {
            val base = positions.size
            for (corner in corners) {
                positions.add(floatArrayOf(corner[0] * HALF, corner[1] * HALF, corner[2] * HALF))
                normals.add(normal)
            }
            uvs.addAll(FACE_UV)
            indices.addAll(listOf(base, base + 1, base + 2, base, base + 2, base + 3))
        }

        // Unpadded sizes are what the JSON accessors/bufferViews must report
        // (glTF byteLength is the real data size, not the 4-byte-aligned one).
        val posRaw = packFloats(positions)
        val normalRaw = packFloats(normals)
        val uvRaw = packFloats(uvs)
        val indexRaw = packShorts(indices)
        val imageRaw = encodeTexture(sourceBitmap)

        val posPadded = pad4(posRaw)
        val normalPadded = pad4(normalRaw)
        val uvPadded = pad4(uvRaw)
        val indexPadded = pad4(indexRaw)
        val imagePadded = pad4(imageRaw)

        val posOffset = 0
        val normalOffset = posOffset + posPadded.size
        val uvOffset = normalOffset + normalPadded.size
        val indexOffset = uvOffset + uvPadded.size
        val imageOffset = indexOffset + indexPadded.size
        val binLength = imageOffset + imagePadded.size
        val binChunkData = posPadded + normalPadded + uvPadded + indexPadded + imagePadded

        val layout = BufferLayout(
            posOffset, posRaw.size,
            normalOffset, normalRaw.size,
            uvOffset, uvRaw.size,
            indexOffset, indexRaw.size,
            imageOffset, imageRaw.size
        )
        val gltfJson = buildGltfJson(
            positions = positions,
            normalCount = normals.size,
            uvCount = uvs.size,
            indexCount = indices.size,
            layout = layout,
            binLength = binLength
        )

        val jsonBytes = pad4(gltfJson.toString().toByteArray(Charsets.UTF_8), ' '.code.toByte())
        val jsonChunk = chunk(CHUNK_TYPE_JSON, jsonBytes)
        val binChunk = chunk(CHUNK_TYPE_BIN, binChunkData)

        val totalLength = 12 + jsonChunk.size + binChunk.size
        val header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(MAGIC_GLTF)
            .putInt(2)
            .putInt(totalLength)
            .array()

        return header + jsonChunk + binChunk
    }

    private class BufferLayout(
        val posOffset: Int, val posLength: Int,
        val normalOffset: Int, val normalLength: Int,
        val uvOffset: Int, val uvLength: Int,
        val indexOffset: Int, val indexLength: Int,
        val imageOffset: Int, val imageLength: Int
    )

    private fun buildGltfJson(
        positions: List<FloatArray>,
        normalCount: Int,
        uvCount: Int,
        indexCount: Int,
        layout: BufferLayout,
        binLength: Int
    ): JSONObject {
        val xs = positions.map { it[0].toDouble() }
        val ys = positions.map { it[1].toDouble() }
        val zs = positions.map { it[2].toDouble() }

        return JSONObject().apply {
            put("asset", JSONObject().put("version", "2.0").put("generator", "JelajahBatikJambi on-device motif-cube generator"))
            put("scene", 0)
            put("scenes", JSONArray().put(JSONObject().put("nodes", JSONArray().put(0))))
            put("nodes", JSONArray().put(JSONObject().put("mesh", 0).put("name", "CustomMotif")))
            put(
                "meshes",
                JSONArray().put(
                    JSONObject()
                        .put("name", "CustomMotif")
                        .put(
                            "primitives",
                            JSONArray().put(
                                JSONObject()
                                    .put("attributes", JSONObject().put("POSITION", 0).put("NORMAL", 1).put("TEXCOORD_0", 2))
                                    .put("indices", 3)
                                    .put("material", 0)
                            )
                        )
                )
            )
            put(
                "materials",
                JSONArray().put(
                    JSONObject()
                        .put("name", "CustomMotifMaterial")
                        .put(
                            "pbrMetallicRoughness",
                            JSONObject()
                                .put("baseColorTexture", JSONObject().put("index", 0))
                                .put("baseColorFactor", JSONArray().put(1.0).put(1.0).put(1.0).put(1.0))
                                .put("metallicFactor", 0.0)
                                .put("roughnessFactor", 0.8)
                        )
                )
            )
            put("textures", JSONArray().put(JSONObject().put("sampler", 0).put("source", 0)))
            put(
                "samplers",
                JSONArray().put(
                    // minFilter 9729 (LINEAR) — deliberately NOT a mipmapped
                    // filter (9987 LINEAR_MIPMAP_LINEAR): gltfio's embedded-
                    // texture path does not always generate mipmaps, and a
                    // mip-requiring sampler then samples a missing level,
                    // which renders as a *black* cube on some devices (seen
                    // on-device: 5cm cube with correct JPEG texture inside
                    // the GLB, but rendered fully black until minFilter was
                    // flattened to a single level). A 512px texture on a 5cm
                    // cube loses nothing without mipmaps.
                    JSONObject().put("magFilter", 9729).put("minFilter", 9729).put("wrapS", 10497).put("wrapT", 10497)
                )
            )
            put("images", JSONArray().put(JSONObject().put("bufferView", 4).put("mimeType", "image/jpeg")))
            put(
                "accessors",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("bufferView", 0).put("componentType", 5126).put("count", positions.size).put("type", "VEC3")
                            .put("min", JSONArray().put(xs.min()).put(ys.min()).put(zs.min()))
                            .put("max", JSONArray().put(xs.max()).put(ys.max()).put(zs.max()))
                    )
                    .put(JSONObject().put("bufferView", 1).put("componentType", 5126).put("count", normalCount).put("type", "VEC3"))
                    .put(JSONObject().put("bufferView", 2).put("componentType", 5126).put("count", uvCount).put("type", "VEC2"))
                    .put(JSONObject().put("bufferView", 3).put("componentType", 5123).put("count", indexCount).put("type", "SCALAR"))
            )
            put(
                "bufferViews",
                JSONArray()
                    .put(JSONObject().put("buffer", 0).put("byteOffset", layout.posOffset).put("byteLength", layout.posLength).put("target", 34962))
                    .put(JSONObject().put("buffer", 0).put("byteOffset", layout.normalOffset).put("byteLength", layout.normalLength).put("target", 34962))
                    .put(JSONObject().put("buffer", 0).put("byteOffset", layout.uvOffset).put("byteLength", layout.uvLength).put("target", 34962))
                    .put(JSONObject().put("buffer", 0).put("byteOffset", layout.indexOffset).put("byteLength", layout.indexLength).put("target", 34963))
                    .put(JSONObject().put("buffer", 0).put("byteOffset", layout.imageOffset).put("byteLength", layout.imageLength))
            )
            put("buffers", JSONArray().put(JSONObject().put("byteLength", binLength)))
        }
    }

    private fun encodeTexture(source: Bitmap): ByteArray {
        val side = minOf(source.width, source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        val cropped = Bitmap.createBitmap(source, x, y, side, side)
        val resized = Bitmap.createScaledBitmap(cropped, TEXTURE_SIZE, TEXTURE_SIZE, true)
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return stream.toByteArray()
    }

    private fun packFloats(vectors: List<FloatArray>): ByteArray {
        val componentCount = vectors.sumOf { it.size }
        val buffer = ByteBuffer.allocate(componentCount * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (vector in vectors) for (component in vector) buffer.putFloat(component)
        return buffer.array()
    }

    private fun packShorts(values: List<Int>): ByteArray {
        val buffer = ByteBuffer.allocate(values.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach { buffer.putShort(it.toShort()) }
        return buffer.array()
    }

    private fun pad4(data: ByteArray, padByte: Byte = 0): ByteArray {
        val remainder = data.size % 4
        return if (remainder == 0) data else data + ByteArray(4 - remainder) { padByte }
    }

    private fun chunk(chunkType: Int, data: ByteArray): ByteArray {
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(data.size)
            .putInt(chunkType)
            .array()
        return header + data
    }
}
