package com.jelajahbatikjambi.ui.editmotif

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Form gate for the edit screen: name/category/description must be filled
 * before saving (mirroring AddMotifViewModel.canSave), while makna/sejarah
 * stay optional — a blank there falls back to the original text rather than
 * blocking the save.
 */
class EditMotifUiStateTest {

    @Test
    fun `saving is blocked while the screen is still loading`() {
        val state = EditMotifUiState(isLoading = true, name = "A", category = "B", shortDescription = "C")

        assertFalse(state.canSave)
    }

    @Test
    fun `saving is blocked while any required field is blank`() {
        assertFalse(
            EditMotifUiState(
                isLoading = false, name = "", category = "B", shortDescription = "C"
            ).canSave
        )
        assertFalse(
            EditMotifUiState(
                isLoading = false, name = "A", category = "  ", shortDescription = "C"
            ).canSave
        )
        assertFalse(
            EditMotifUiState(
                isLoading = false, name = "A", category = "B", shortDescription = ""
            ).canSave
        )
    }

    @Test
    fun `saving is allowed once required fields are filled even if meaning and history are empty`() {
        val state = EditMotifUiState(
            isLoading = false,
            name = "A",
            category = "B",
            shortDescription = "C",
            meaning = "",
            history = ""
        )

        assertTrue(state.canSave)
    }

    @Test
    fun `built-in motifs can never be deleted`() {
        val state = EditMotifUiState(
            isLoading = false,
            isBuiltIn = true,
            totalMotifs = 5
        )

        assertFalse(state.canDelete)
    }

    @Test
    fun `a custom motif can be deleted while more than one motif exists`() {
        val state = EditMotifUiState(
            isLoading = false,
            isBuiltIn = false,
            totalMotifs = 5
        )

        assertTrue(state.canDelete)
    }

    @Test
    fun `the last remaining motif cannot be deleted`() {
        assertFalse(
            EditMotifUiState(isLoading = false, isBuiltIn = false, totalMotifs = 1).canDelete
        )
        // Still loading means the count isn't known yet — stay on the safe side.
        assertFalse(
            EditMotifUiState(isLoading = true, isBuiltIn = false, totalMotifs = 0).canDelete
        )
    }
}
