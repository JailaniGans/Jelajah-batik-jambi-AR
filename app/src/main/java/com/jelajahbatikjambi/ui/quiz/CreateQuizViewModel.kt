package com.jelajahbatikjambi.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreateQuizUiState(
    val prompt: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    /** Every motif in the app (built-in + custom) — the picker's options. */
    val motifs: List<BatikData> = emptyList(),
    /** The motif this question will be tied to; required — there is no "no motif" option. */
    val selectedBatikId: Int? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
) {
    val canSave: Boolean
        get() = prompt.isNotBlank() && options.all { it.isNotBlank() } && selectedBatikId != null
}

/**
 * Lets the user author their own multiple-choice quiz question
 * (§ user request: "opsi untuk buat kuis nya"), stored via
 * [CustomQuizRepository] and folded into every future quiz session by
 * [QuizViewModel]. The question is tied to the motif picked in the form
 * (§ user request: "menentukan soal yang di buat itu ke motif yang telah di
 * pilih user") — [initialBatikId] pre-selects it when the form is opened
 * from a context that already knows the motif (a scoped quiz, or that
 * motif's edit screen).
 */
class CreateQuizViewModel(
    application: Application,
    private val initialBatikId: Int?
) : AndroidViewModel(application) {

    private val customQuizRepository = CustomQuizRepository(AppDatabase.getInstance(application).customQuizQuestionDao())
    private val motifRepository = MotifRepository.getInstance(application)

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val motifs = withContext(Dispatchers.IO) { motifRepository.getAllOnce() }
            _uiState.value = _uiState.value.copy(
                motifs = motifs,
                // Only pre-select an id that still exists — a motif deleted
                // between navigation and this screen loading leaves the
                // picker empty for the user to fill in.
                selectedBatikId = initialBatikId?.takeIf { id -> motifs.any { it.id == id } }
            )
        }
    }

    fun onPromptChanged(value: String) {
        _uiState.value = _uiState.value.copy(prompt = value)
    }

    fun onOptionChanged(index: Int, value: String) {
        val updated = _uiState.value.options.toMutableList().apply { this[index] = value }
        _uiState.value = _uiState.value.copy(options = updated)
    }

    fun onCorrectOptionSelected(index: Int) {
        _uiState.value = _uiState.value.copy(correctOptionIndex = index)
    }

    fun onMotifSelected(batikId: Int) {
        _uiState.value = _uiState.value.copy(selectedBatikId = batikId)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            customQuizRepository.addQuestion(
                prompt = state.prompt.trim(),
                options = state.options.map { it.trim() },
                correctOptionIndex = state.correctOptionIndex,
                batikId = checkNotNull(state.selectedBatikId)
            )
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }

    companion object {
        /** [initialBatikId] isn't a constructor default Compose's [androidx.lifecycle.viewmodel.compose.viewModel] can supply on its own. */
        fun factory(application: Application, initialBatikId: Int?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CreateQuizViewModel(application, initialBatikId) as T
            }
    }
}
