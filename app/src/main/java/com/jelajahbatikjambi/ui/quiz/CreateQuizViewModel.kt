package com.jelajahbatikjambi.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.database.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateQuizUiState(
    val prompt: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
) {
    val canSave: Boolean
        get() = prompt.isNotBlank() && options.all { it.isNotBlank() }
}

/**
 * Lets the user author their own multiple-choice quiz question
 * (§ user request: "opsi untuk buat kuis nya"), stored via
 * [CustomQuizRepository] and merged into every future quiz session by
 * [QuizViewModel].
 */
class CreateQuizViewModel(application: Application) : AndroidViewModel(application) {

    private val customQuizRepository = CustomQuizRepository(AppDatabase.getInstance(application).customQuizQuestionDao())

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

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

    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            customQuizRepository.addQuestion(
                prompt = state.prompt.trim(),
                options = state.options.map { it.trim() },
                correctOptionIndex = state.correctOptionIndex
            )
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }
}
