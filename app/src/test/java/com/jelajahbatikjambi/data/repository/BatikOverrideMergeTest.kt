package com.jelajahbatikjambi.data.repository

import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.database.BatikOverrideEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Covers the merge that turns "bundled batik.json + stored edits + custom
 * motifs" into the single list every screen reads. The invariants asserted
 * here are what make editing safe: identities never move, a missing edit is
 * a true no-op, and an empty field can't wipe out original content.
 */
class BatikOverrideMergeTest {

    private val builtIn = BatikData(
        id = 1,
        markerId = 0,
        name = "Angso Duo",
        category = "Motif Batik Jambi",
        shortDescription = "Deskripsi asli.",
        meaning = "Makna asli (placeholder).",
        history = "Sejarah asli (placeholder).",
        imagePath = "images/angso_duo.jpeg",
        modelPath = "models/motif_angso_duo.glb"
    )

    private fun overrideOf(
        batikId: Int = 1,
        name: String = "Nama Editan",
        category: String = "Kategori Editan",
        shortDescription: String = "Deskripsi editan.",
        meaning: String = "Makna editan.",
        history: String = "Sejarah editan.",
        imagePath: String? = null
    ) = BatikOverrideEntity(
        batikId = batikId,
        name = name,
        category = category,
        shortDescription = shortDescription,
        meaning = meaning,
        history = history,
        imagePath = imagePath,
        updatedAt = 123L
    )

    @Test
    fun `no override leaves the bundled motif untouched`() {
        assertSame(builtIn, applyBatikOverride(builtIn, null))
    }

    @Test
    fun `override replaces every editable text field`() {
        val merged = applyBatikOverride(builtIn, overrideOf())

        assertEquals("Nama Editan", merged.name)
        assertEquals("Kategori Editan", merged.category)
        assertEquals("Deskripsi editan.", merged.shortDescription)
        assertEquals("Makna editan.", merged.meaning)
        assertEquals("Sejarah editan.", merged.history)
    }

    @Test
    fun `identity and model stay pinned to the bundled motif`() {
        // Discovery records key on id, AR tracking on markerId, and the 3D
        // overlay on modelPath — an edit must not be able to move any of them.
        val merged = applyBatikOverride(builtIn, overrideOf(imagePath = "/data/files/edited_images/1.jpg"))

        assertEquals(1, merged.id)
        assertEquals(0, merged.markerId)
        assertEquals("models/motif_angso_duo.glb", merged.modelPath)
    }

    @Test
    fun `override without a new photo keeps the bundled image`() {
        assertEquals(builtIn.imagePath, applyBatikOverride(builtIn, overrideOf(imagePath = null)).imagePath)
    }

    @Test
    fun `override with a new photo points at the edited file`() {
        val merged = applyBatikOverride(builtIn, overrideOf(imagePath = "/data/files/edited_images/1.jpg"))

        assertEquals("/data/files/edited_images/1.jpg", merged.imagePath)
    }

    @Test
    fun `blank overlay fields keep the original text instead of erasing it`() {
        val merged = applyBatikOverride(
            builtIn,
            overrideOf(name = "   ", category = "", shortDescription = "", meaning = "", history = "")
        )

        assertEquals("Angso Duo", merged.name)
        assertEquals("Motif Batik Jambi", merged.category)
        assertEquals("Deskripsi asli.", merged.shortDescription)
        assertEquals("Makna asli (placeholder).", merged.meaning)
        assertEquals("Sejarah asli (placeholder).", merged.history)
    }

    @Test
    fun `merge puts edited built-ins first and custom motifs after`() {
        val otherBuiltIn = builtIn.copy(id = 2, markerId = 1, name = "Tampuk Manggis")
        val custom = builtIn.copy(id = 1001, markerId = 1001, name = "Motif Saya")

        val merged = mergeMotifs(
            builtIn = listOf(builtIn, otherBuiltIn),
            overrides = listOf(overrideOf(name = "Nama Baru")),
            custom = listOf(custom)
        )

        assertEquals(listOf(1, 2, 1001), merged.map { it.id })
        assertEquals("Nama Baru", merged[0].name)
        assertEquals("Tampuk Manggis", merged[1].name)
        assertEquals("Motif Saya", merged[2].name)
    }

    @Test
    fun `an override for an unknown motif id is ignored`() {
        val merged = mergeMotifs(
            builtIn = listOf(builtIn),
            overrides = listOf(overrideOf(batikId = 99, name = "Asing")),
            custom = emptyList()
        )

        assertEquals(listOf(builtIn), merged)
    }
}
