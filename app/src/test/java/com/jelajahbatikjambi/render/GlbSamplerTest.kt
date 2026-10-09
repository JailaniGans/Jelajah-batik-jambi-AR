package com.jelajahbatikjambi.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * [GlbSampler] drives the one-time black-cube repair
 * (com.jelajahbatikjambi.data.repository.MotifRepository.repairLegacyCustomGlbs),
 * so its parser must reliably tell an old mipmapped GLB (minFilter 9987)
 * apart from a fixed single-level one (9729) — without blowing up on corrupt
 * files. Pure JVM test: builds minimal GLB byte arrays directly.
 */
class GlbSamplerTest {

    private val magicGltf = 0x46546C67
    private val chunkJson = 0x4E4F534A
    private val chunkBin = 0x004E4942

    @Test
    fun minFilter_readsOldMipmappedSampler() {
        assertEquals(9987, GlbSampler.minFilterOrNull(glbWithSampler(9987)))
        assertEquals(9984, GlbSampler.minFilterOrNull(glbWithSampler(9984)))
    }

    @Test
    fun minFilter_readsFixedSingleLevelSampler() {
        assertEquals(9729, GlbSampler.minFilterOrNull(glbWithSampler(9729)))
        assertEquals(9728, GlbSampler.minFilterOrNull(glbWithSampler(9728)))
    }

    @Test
    fun minFilter_handlesJsonFollowedByBinChunk() {
        // Mirrors the real layout: JSON chunk (space-padded) then a BIN chunk.
        val json = """{"samplers":[{"minFilter":9729}]}"""
        val paddedJson = pad4(json.toByteArray())
        val binData = ByteArray(16)
        val totalLength = 12 + 8 + paddedJson.size + 8 + binData.size
        val buffer = ByteBuffer.allocate(totalLength)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(magicGltf).putInt(2).putInt(totalLength)
            .putInt(paddedJson.size).putInt(chunkJson).put(paddedJson)
            .putInt(binData.size).putInt(chunkBin).put(binData)
        assertEquals(9729, GlbSampler.minFilterOrNull(buffer.array()))
    }

    @Test
    fun minFilter_returnsNullForUnreadableFiles() {
        assertNull(GlbSampler.minFilterOrNull(ByteArray(0)))
        assertNull(GlbSampler.minFilterOrNull(ByteArray(19)))
        assertNull(GlbSampler.minFilterOrNull(ByteArray(20))) // right size, wrong magic
        assertNull(GlbSampler.minFilterOrNull(ByteArray(64) { 0x7F }))
        assertNull(GlbSampler.minFilterOrNull(ByteArray(64) { 'A'.code.toByte() }))
        // Wrong chunk type after a valid header — not a GLB's JSON chunk.
        val header = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(magicGltf).putInt(2).putInt(20).putInt(0).putInt(chunkBin)
        assertNull(GlbSampler.minFilterOrNull(header.array()))
    }

    @Test
    fun minFilter_returnsNullWhenNoSampler() {
        assertNull(GlbSampler.minFilterOrNull(glbWithJson("""{"materials":[]}""")))
        assertNull(GlbSampler.minFilterOrNull(glbWithJson("""{"samplers":[{}]}""")))
    }

    @Test
    fun requiresRegeneration_flagsOldMipmappedAndUnreadable() {
        assertTrue(GlbSampler.requiresRegeneration(null))           // missing file
        assertTrue(GlbSampler.requiresRegeneration(ByteArray(0)))   // corrupt
        assertTrue(GlbSampler.requiresRegeneration(glbWithSampler(9985)))
        assertTrue(GlbSampler.requiresRegeneration(glbWithSampler(9986)))
        assertTrue(GlbSampler.requiresRegeneration(glbWithSampler(9987)))
    }

    @Test
    fun requiresRegeneration_keepsFixedSingleLevel() {
        assertFalse(GlbSampler.requiresRegeneration(glbWithSampler(9728)))
        assertFalse(GlbSampler.requiresRegeneration(glbWithSampler(9729)))
    }

    // --- helpers ---

    private fun glbWithSampler(minFilter: Int): ByteArray =
        glbWithJson("""{"samplers":[{"minFilter":$minFilter}]}""")

    private fun glbWithJson(json: String): ByteArray {
        val paddedJson = pad4(json.toByteArray())
        val buffer = ByteBuffer.allocate(12 + 8 + paddedJson.size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(magicGltf).putInt(2).putInt(12 + 8 + paddedJson.size)
            .putInt(paddedJson.size).putInt(chunkJson).put(paddedJson)
        return buffer.array()
    }

    private fun pad4(data: ByteArray): ByteArray {
        val remainder = data.size % 4
        return if (remainder == 0) data else data + ByteArray(4 - remainder) { ' '.code.toByte() }
    }
}