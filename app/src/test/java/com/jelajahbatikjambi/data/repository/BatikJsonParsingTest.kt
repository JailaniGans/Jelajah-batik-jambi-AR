package com.jelajahbatikjambi.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Exercises [parseBatikJson] directly with a hardcoded JSON string — no
 * [android.content.res.AssetManager] involved, so this runs as a plain local
 * JVM test. Uses the standalone `org.json:json` test dependency, since the
 * `org.json` classes bundled in the Android SDK stub jar throw at runtime
 * when a unit test isn't run under Robolectric or on-device.
 */
class BatikJsonParsingTest {

    @Test
    fun `parses all fields from a full entry`() {
        val json = """
            [
              {
                "id": 1,
                "markerId": 0,
                "name": "Angso Duo",
                "category": "Motif Batik Jambi",
                "shortDescription": "Deskripsi singkat.",
                "meaning": "Makna.",
                "history": "Sejarah.",
                "imagePath": "images/angso_duo.jpeg",
                "modelPath": "models/placeholder_cube.glb"
              }
            ]
        """.trimIndent()

        val result = parseBatikJson(json)

        assertEquals(1, result.size)
        val batik = result.first()
        assertEquals(1, batik.id)
        assertEquals(0, batik.markerId)
        assertEquals("Angso Duo", batik.name)
        assertEquals("Motif Batik Jambi", batik.category)
        assertEquals("Deskripsi singkat.", batik.shortDescription)
        assertEquals("Makna.", batik.meaning)
        assertEquals("Sejarah.", batik.history)
        assertEquals("images/angso_duo.jpeg", batik.imagePath)
        assertEquals("models/placeholder_cube.glb", batik.modelPath)
    }

    @Test
    fun `blank optional image and model paths become null`() {
        val json = """
            [
              {
                "id": 2,
                "markerId": 1,
                "name": "Tampuk Manggis",
                "category": "Motif Batik Jambi",
                "shortDescription": "d",
                "meaning": "m",
                "history": "h",
                "imagePath": "",
                "modelPath": ""
              }
            ]
        """.trimIndent()

        val result = parseBatikJson(json)

        assertNull(result.first().imagePath)
        assertNull(result.first().modelPath)
    }

    @Test
    fun `parses multiple entries in order`() {
        val json = """
            [
              {"id": 1, "markerId": 0, "name": "A", "category": "c", "shortDescription": "d", "meaning": "m", "history": "h"},
              {"id": 2, "markerId": 1, "name": "B", "category": "c", "shortDescription": "d", "meaning": "m", "history": "h"}
            ]
        """.trimIndent()

        val result = parseBatikJson(json)

        assertEquals(listOf(1, 2), result.map { it.id })
        assertEquals(listOf("A", "B"), result.map { it.name })
    }

    @Test
    fun `an empty array parses to an empty list`() {
        assertEquals(emptyList<Any>(), parseBatikJson("[]"))
    }
}
