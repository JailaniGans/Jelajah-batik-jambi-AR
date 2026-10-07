package com.jelajahbatikjambi.ui.editquizquestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Form gate for the "Edit Soal Kuis" screen — same rule as creating one:
 * prompt, four options and a motif are all required. A question stored
 * before ties became mandatory (batikId -1) starts with no motif picked,
 * so saving is blocked until the user assigns one.
 */
class EditQuizQuestionUiStateTest {

    private val filled = EditQuizQuestionUiState(
        isLoading = false,
        prompt = "Motif ini terinspirasi dari bentuk buah durian.",
        options = listOf("Angso Duo", "Durian Pecah", "Kapal Sanggat", "Tampuk Manggis"),
        selectedBatikId = 1001
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
    fun `saving is blocked while the screen is still loading`() {
        assertFalse(filled.copy(isLoading = true).canSave)
    }

    @Test
    fun `saving is blocked while the prompt or any option is blank`() {
        assertFalse(filled.copy(prompt = "").canSave)
        assertFalse(filled.copy(options = listOf("A", "B", "", "D")).canSave)
    }
}
