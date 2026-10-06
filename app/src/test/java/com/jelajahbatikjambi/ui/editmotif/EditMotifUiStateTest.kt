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
}
