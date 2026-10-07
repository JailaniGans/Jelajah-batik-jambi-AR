package com.jelajahbatikjambi.ui.editquizquestion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.database.CustomQuizQuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditQuizQuestionUiState(
    val isLoading: Boolean = true,
    val questionId: Long = -1L,
    val prompt: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    /** Every motif in the app (built-in + custom) — the picker's options. */
    val motifs: List<BatikData> = emptyList(),
    /** The motif this question is tied to; required — there is no "no motif" option. */
    val selectedBatikId: Int? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
) {
    val canSave: Boolean
        get() = !isLoading && prompt.isNotBlank() && options.all { it.isNotBlank() } && selectedBatikId != null
}

/**
 * Edits one quiz question the user authored themselves, persisting through
 * [CustomQuizRepository] so the change lands in Room and outlives the app
 * being closed. The row's id never changes, so the question keeps its place
 * in the manage list; there is no "reset" case — a user-authored question
 * has no built-in original to return to (see [EditMotifViewModel] for the
 * built-in-motif equivalent).
 */
class EditQuizQuestionViewModel(
    application: Application,
    private val questionId: Long
) : AndroidViewModel(application) {

    private val repository = CustomQuizRepository(
        AppDatabase.getInstance(application).customQuizQuestionDao()
    )
    private val motifRepository = MotifRepository.getInstance(application)

    private val _uiState = MutableStateFlow(EditQuizQuestionUiState(questionId = questionId))
    val uiState: StateFlow<EditQuizQuestionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val motifs = withContext(Dispatchers.IO) { motifRepository.getAllOnce() }
            val entity = withContext(Dispatchers.IO) { repository.getById(questionId) }
            if (entity == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Soal tidak ditemukan."
                )
                return@launch
            }
            _uiState.value = EditQuizQuestionUiState(
                isLoading = false,
                questionId = questionId,
                prompt = entity.prompt,
                options = entity.options(),
                correctOptionIndex = entity.correctOptionIndex,
                motifs = motifs,
                // A question stored before ties became mandatory (-1), or one
                // whose motif is gone, starts unselected — the user must
                // pick a motif before saving.
                selectedBatikId = entity.batikId.takeIf { id -> motifs.any { it.id == id } }
            )
        }
    }

    fun onPromptChanged(value: String) {
        _uiState.value = _uiState.value.copy(prompt = value)
    }

    fun onOptionChanged(index: Int, value: String) {
        val updated = _uiState.value.options.toMutableList().also { it[index] = value }
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

        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // Re-read rather than trusting a stale copy: the manage
                    // screen's Flow could have replaced the row meanwhile.
                    val existing = repository.getById(questionId)
                        ?: error("Soal tidak ditemukan")
                    repository.updateQuestion(
                        existing.copy(
                            prompt = state.prompt.trim(),
                            optionA = state.options[0].trim(),
                            optionB = state.options[1].trim(),
                            optionC = state.options[2].trim(),
                            optionD = state.options[3].trim(),
                            correctOptionIndex = state.correctOptionIndex,
                            batikId = checkNotNull(state.selectedBatikId)
                        )
                    )
                }
            }
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isSaving = false, savedSuccessfully = true)
            } else {
                _uiState.value.copy(isSaving = false, error = "Gagal menyimpan perubahan. Coba lagi.")
            }
        }
    }

    companion object {
        /** [questionId] isn't a constructor default Compose's [androidx.lifecycle.viewmodel.compose.viewModel] can supply on its own. */
        fun factory(application: Application, questionId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditQuizQuestionViewModel(application, questionId) as T
            }
    }
}

private fun CustomQuizQuestionEntity.options() = listOf(optionA, optionB, optionC, optionD)
