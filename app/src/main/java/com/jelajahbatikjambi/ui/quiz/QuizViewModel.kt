package com.jelajahbatikjambi.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.model.QuizQuestion
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.ui.common.SoundEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * whatever's stored at that moment (not a live-reactive Flow) so the
 * question set/shuffle stays stable for the whole session even if
 * questions are edited elsewhere — *unless* the stored set itself changes
 * while the screen is in the background, which is exactly what happens when
 * the user leaves through the screen's "+" or "Kelola Soal" button to add a
 * question: [reloadIfChanged] then rebuilds the session so the new question
 * is actually in the quiz (§ user request: "soal yang sudah ditambahkan
 * tidak muncul di kuis").
 *
 * The set is exactly what the user authored themselves via
 * [CreateQuizScreen] (§ user request: "opsi untuk buat kuis nya") — the app
 * generates no questions of its own, so this table is the whole question
 * set (§ user request: "hapus soal built in saja tidak dengan motif nya").
 *
 * When started right after scanning a motif in AR ([scopedBatikId] non-null —
 * § user request: "ketika klik mulai kuis pertanyaan sesuai dengan motif apa
 * yang saya scan"), the session holds only the questions keyed to that
 * motif — the "buat soal untuk motif ini" option on Add Motif, or the motif
 * picker on Create Quiz. Starting the quiz from the Collection screen
 * ([scopedBatikId] null) gets every stored question, whatever motif each is
 * tied to.
 */
class QuizViewModel(application: Application, private val scopedBatikId: Int? = null) : AndroidViewModel(application) {

    private val customQuizRepository =
        CustomQuizRepository(AppDatabase.getInstance(application).customQuizQuestionDao())

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Builds the question set from scratch. Split out of [init] so "Main
     * Lagi" can call it again: the fresh read is what re-shuffles the order
     * and picks up anything the user created or deleted since the session
     * started, keeping the "stable for the whole session" snapshot rule
     * intact — it just defines what a *new* session snapshots.
     */
    private fun load() {
        viewModelScope.launch {
            _uiState.value = QuizUiState(isLoading = true)
            // User-authored questions only, tied to this motif for a scoped
            // session; every stored question otherwise.
            val questions = customQuizRepository.getAllOnceAsQuizQuestions()
                .filter { scopedBatikId == null || it.batikId == scopedBatikId }
                .shuffled()
            _uiState.value = QuizUiState(isLoading = false, questions = questions)
        }
    }

    /**
     * Called when the quiz screen returns to the foreground — the user just
     * came back from the "+" create-question form or from the manage screen.
     * The stored question set is read fresh and compared with this session's
     * snapshot: when the two match, nothing changed, so the session carries
     * on untouched (same shuffle, same score, same progress); when they
     * differ — a question was added, edited or deleted while away — the
     * session is rebuilt from that read, so a newly added question shows up
     * immediately instead of only after "Main Lagi".
     */
    fun reloadIfChanged() {
        // A load is already in flight; it reads whatever is stored by the time
        // it runs, so this call could only risk overwriting it with a staler set.
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            val stored = customQuizRepository.getAllOnceAsQuizQuestions()
                .filter { scopedBatikId == null || it.batikId == scopedBatikId }
            val current = _uiState.value
            // Same guard as above, re-checked because a load may have started
            // while this read was in flight.
            if (current.isLoading || hasSameQuestions(current.questions, stored)) return@launch
            _uiState.value = QuizUiState(isLoading = false, questions = stored.shuffled())
        }
    }

    /**
     * "Main Lagi": a brand-new session with the same scoping — score,
     * progress and answer state are dropped, and [load] re-reads everything
     * so the round is freshly shuffled. If the rebuilt set turns out empty
     * (e.g. the last custom question was deleted mid-session) the UI falls
     * through to its existing empty state rather than a 0-question quiz.
     */
    fun restart() {
        load()
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

/**
 * Multiset comparison of two question sets: order never matters — sessions
 * are shuffled anyway — but duplicate rows do, so adding a second question
 * with the same wording still counts as the change [QuizViewModel.reloadIfChanged]
 * has to pick up.
 */
private fun hasSameQuestions(a: List<QuizQuestion>, b: List<QuizQuestion>): Boolean {
    if (a.size != b.size) return false
    val unmatched = a.groupingBy { it }.eachCount().toMutableMap()
    for (question in b) {
        val left = unmatched[question] ?: return false
        if (left == 1) unmatched.remove(question) else unmatched[question] = left - 1
    }
    return true
}
