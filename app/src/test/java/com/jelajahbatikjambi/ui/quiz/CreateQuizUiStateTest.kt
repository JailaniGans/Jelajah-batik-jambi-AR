package com.jelajahbatikjambi.ui.quiz

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Form gate for the "Buat Soal Kuis" screen: prompt and all four options
 * must be filled, and — per the user's request that every question name the
 * motif it belongs to — a motif must be picked; there is no "no motif"
 * option to fall back on.
 */
class CreateQuizUiStateTest {

    private val filled = CreateQuizUiState(
        prompt = "Motif ini terinspirasi dari bentuk buah durian.",
        options = listOf("Angso Duo", "Durian Pecah", "Kapal Sanggat", "Tampuk Manggis"),
        selectedBatikId = 1
    )

    @Test
    fun `saving is allowed once prompt, options and a motif are set`() {
        assertTrue(filled.canSave)
    }

    @Test
    fun `saving is blocked while no motif is picked`() {
        assertFalse(filled.copy(selectedBatikId = null).canSave)
    }

    @Test
    fun `saving is blocked while the prompt or any option is blank`() {
        assertFalse(filled.copy(prompt = " ").canSave)
        assertFalse(filled.copy(options = listOf("A", "B", "C", "")).canSave)
    }
}
