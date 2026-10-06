package com.jelajahbatikjambi.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.model.QuizQuestion
import com.jelajahbatikjambi.data.model.buildQuizQuestions
import com.jelajahbatikjambi.data.model.buildQuizQuestionsForMotif
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.data.repository.DiscoveryRepository
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.ui.common.SoundEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class QuizUiState(
    val isLoading: Boolean = true,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val score: Int = 0,
    val isFinished: Boolean = false
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
}

/**
 * Drives a single quiz session. Questions are snapshotted once at start from
 * whatever's discovered at that moment (not a live-reactive Flow) so the
 * question set/shuffle stays stable for the whole session even if discovery
 * state changes elsewhere.
 *
 * Combines auto-generated "guess the motif" questions (built-in + custom
 * motifs alike) with any questions the user authored themselves via
 * [CreateQuizScreen] (§ user request: "opsi untuk buat kuis nya") — shuffled
 * together into one session rather than run as two separate quizzes.
 *
 * When started right after scanning a motif in AR ([scopedBatikId] non-null —
 * § user request: "ketika klik mulai kuis pertanyaan sesuai dengan motif apa
 * yang saya scan"), the session is scoped to just that motif via
 * [buildQuizQuestionsForMotif] instead of the general discovered-motifs mix;
 * user-authored questions aren't tied to any motif, so they're left out of a
 * scoped session. Starting the quiz from the Collection screen still gets the
 * general mix ([scopedBatikId] null).
 */
class QuizViewModel(application: Application, private val scopedBatikId: Int? = null) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val discoveryRepository = DiscoveryRepository(database.discoveryDao())
    private val motifRepository = MotifRepository.getInstance(application)
    private val customQuizRepository = CustomQuizRepository(database.customQuizQuestionDao())

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val allBatik = motifRepository.getAllOnce()
            val questions = if (scopedBatikId != null) {
                allBatik.firstOrNull { it.id == scopedBatikId }
                    ?.let { buildQuizQuestionsForMotif(it, allBatik) }
                    .orEmpty()
            } else {
                val discoveredIds = discoveryRepository.discoveredBatikIds.first()
                val generatedQuestions = buildQuizQuestions(allBatik, discoveredIds)
                val customQuestions = customQuizRepository.getAllOnceAsQuizQuestions()
                (generatedQuestions + customQuestions).shuffled()
            }
            _uiState.value = QuizUiState(isLoading = false, questions = questions)
        }
    }

    fun submitAnswer(optionIndex: Int) {
        val state = _uiState.value
        if (state.selectedOptionIndex != null) return // already answered this question
        val question = state.currentQuestion ?: return

        val isCorrect = optionIndex == question.correctOptionIndex
        if (isCorrect) SoundEffects.playSuccess() else SoundEffects.playError()
        _uiState.value = state.copy(
            selectedOptionIndex = optionIndex,
            score = state.score + if (isCorrect) 1 else 0
        )
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        _uiState.value = if (nextIndex >= state.questions.size) {
            state.copy(isFinished = true)
        } else {
            state.copy(currentIndex = nextIndex, selectedOptionIndex = null)
        }
    }

    companion object {
        /** [scopedBatikId] isn't a constructor default Compose's [androidx.lifecycle.viewmodel.compose.viewModel] can supply on its own. */
        fun factory(application: Application, scopedBatikId: Int?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QuizViewModel(application, scopedBatikId) as T
            }
    }
}
