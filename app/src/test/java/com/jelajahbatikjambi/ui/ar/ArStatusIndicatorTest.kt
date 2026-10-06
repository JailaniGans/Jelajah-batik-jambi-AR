package com.jelajahbatikjambi.ui.ar

import com.jelajahbatikjambi.ar.ArState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks in the sticky "found" presentation of the AR status pill (§ user
 * request): after the first confirmed motif in the session, every
 * "not found yet" label is replaced by "Motif ditemukan" — while a genuine
 * pipeline error still has to come through untranslated.
 */
class ArStatusIndicatorTest {

    @Test
    fun `before any discovery the pill reports the live scan state`() {
        assertEquals("Menyiapkan kamera...", statusPillText(ArState.Initializing, hasDiscovered = false))
        assertEquals("Mencari motif...", statusPillText(ArState.Searching, hasDiscovered = false))
        assertEquals("Mengunci motif...", statusPillText(ArState.MarkerDetected(0), hasDiscovered = false))
        assertEquals("Arahkan kamera ke motif", statusPillText(ArState.MarkerLost(0), hasDiscovered = false))
        assertEquals("Motif ditemukan", statusPillText(ArState.Tracking(0), hasDiscovered = false))
    }

    @Test
    fun `after the first discovery every lost-tracking state still reads as found`() {
        assertEquals(
            "Motif ditemukan",
            statusPillText(ArState.Searching, hasDiscovered = true)
        )
        assertEquals(
            "Motif ditemukan",
            statusPillText(ArState.MarkerLost(0), hasDiscovered = true)
        )
        assertEquals(
            "Motif ditemukan",
            statusPillText(ArState.MarkerDetected(1), hasDiscovered = true)
        )
        assertEquals(
            "Motif ditemukan",
            statusPillText(ArState.Tracking(1), hasDiscovered = true)
        )
    }

    @Test
    fun `an error is never dressed up as a successful scan`() {
        val error = ArState.Error("OpenCV tidak tersedia")

        assertEquals("OpenCV tidak tersedia", statusPillText(error, hasDiscovered = true))
        assertEquals("OpenCV tidak tersedia", statusPillText(error, hasDiscovered = false))
    }
}
