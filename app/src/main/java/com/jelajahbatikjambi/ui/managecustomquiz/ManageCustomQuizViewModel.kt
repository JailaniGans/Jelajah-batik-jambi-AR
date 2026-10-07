package com.jelajahbatikjambi.ui.managecustomquiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.database.CustomQuizQuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ManageCustomQuizUiState(
    val questions: List<CustomQuizQuestionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Backs the "Kelola Soal Kuis" screen: a live list of the quiz questions the
 * user authored themselves, plus deletion. Edits happen on a separate screen
 * — both read/write through Room, so changes survive the app being closed.
 */
class ManageCustomQuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CustomQuizRepository(
        AppDatabase.getInstance(application).customQuizQuestionDao()
    )

    val uiState: StateFlow<ManageCustomQuizUiState> = repository.observeAllAsEntities()
        .map { entities ->
            ManageCustomQuizUiState(questions = entities, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ManageCustomQuizUiState()
        )

    fun deleteQuestion(question: CustomQuizQuestionEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { repository.deleteQuestion(question) }
            }
        }
    }
}
