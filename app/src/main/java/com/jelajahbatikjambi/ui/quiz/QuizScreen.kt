package com.jelajahbatikjambi.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Multiple-choice quiz (§ user request). With no [batikId] (started from the
 * Collection screen) the session holds every question the user has authored
 * ([CreateQuizScreen]); the app generates none of its own. With [batikId]
 * set — started right after scanning that motif in AR (§ user request:
 * "ketika klik mulai kuis pertanyaan sesuai dengan motif apa yang saya
 * scan") — it holds only the questions keyed to that motif.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(batikId: Int?, onBack: () -> Unit, onCreateQuiz: () -> Unit, onManageCustomQuiz: () -> Unit) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: QuizViewModel = viewModel(factory = QuizViewModel.factory(application, batikId))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // A session snapshotted empty (first visit, or right after the user
    // created a question from the + button and came back) picks up the new
    // question; a session already in progress keeps its snapshot.
    LifecycleResumeEffect(Unit) {
        viewModel.reloadIfEmpty()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kuis Motif Batik") },
                navigationIcon = {
                    IconButton(onClick = onBack.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onManageCustomQuiz.withClickSound()) {
                        Icon(Icons.Filled.Edit, contentDescription = "Kelola Soal Kuis")
                    }
                    IconButton(onClick = onCreateQuiz.withClickSound()) {
                        Icon(Icons.Filled.Add, contentDescription = "Buat Soal Kuis")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
                uiState.questions.isEmpty() -> EmptyState(
                    batikId = batikId,
                    modifier = Modifier.fillMaxSize()
                )
                uiState.isFinished -> ResultState(
                    score = uiState.score,
                    total = uiState.questions.size,
                    onBack = onBack,
                    onPlayAgain = viewModel::restart,
                    modifier = Modifier.fillMaxSize()
                )
                else -> QuestionState(
                    uiState = uiState,
                    onSelectOption = viewModel::submitAnswer,
                    onNext = viewModel::nextQuestion,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// internal rather than private: instrumented tests render these directly
// with a hand-built QuizUiState, rather than through the Room-backed
// ViewModel — persisted discovery state would make an end-to-end test flaky
// across runs (adb install -r keeps app data, so "nothing discovered yet"
// isn't reliably true on a real device after earlier manual testing).
@Composable
internal fun EmptyState(batikId: Int? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (batikId != null) "Belum ada soal untuk motif ini" else "Belum ada soal kuis",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (batikId != null) {
                "Tekan tombol + di atas untuk membuat soal untuk motif ini — pilihannya sudah terisi otomatis."
            } else {
                "Tekan tombol + di atas untuk membuat soal kuis pertama Anda, lalu pilih motif yang ingin diujikan."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun QuestionState(
    uiState: QuizUiState,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = uiState.currentQuestion ?: return

    Column(
        modifier = modifier.padding(Dimensions.spacingLg),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
    ) {
        LinearProgressIndicator(
            progress = { (uiState.currentIndex + 1).toFloat() / uiState.questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Soal ${uiState.currentIndex + 1} dari ${uiState.questions.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Motif Batik Jambi manakah ini?",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = question.prompt,
            style = MaterialTheme.typography.bodyLarge
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingSm)) {
            question.options.forEachIndexed { index, option ->
                QuizOptionButton(
                    text = option,
                    isSelected = uiState.selectedOptionIndex == index,
                    isCorrectOption = index == question.correctOptionIndex,
                    isAnswered = uiState.selectedOptionIndex != null,
                    onClick = { onSelectOption(index) }
                )
            }
        }

        if (uiState.selectedOptionIndex != null) {
            Button(
                onClick = onNext.withClickSound(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight)
            ) {
                val isLastQuestion = uiState.currentIndex + 1 >= uiState.questions.size
                Text(if (isLastQuestion) "Lihat Skor" else "Lanjut")
            }
        }
    }
}

/**
 * Right/wrong feedback uses an icon in addition to color — color alone
 * (the original version of this button) isn't distinguishable for colorblind
 * users, and a quiz answer is exactly the kind of state that must not rely
 * on color as the only signal.
 */
@Composable
private fun QuizOptionButton(
    text: String,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    isAnswered: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        !isAnswered -> MaterialTheme.colorScheme.surfaceVariant
        isCorrectOption -> Color(0xFF4C6B4F) // BatikGreen — matches app palette, reads as "correct"
        isSelected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isAnswered && (isCorrectOption || isSelected)) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClick,
        enabled = !isAnswered,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.buttonHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, modifier = Modifier.weight(1f))
            if (isAnswered && (isCorrectOption || isSelected)) {
                Icon(
                    imageVector = if (isCorrectOption) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = if (isCorrectOption) "Jawaban benar" else "Jawaban salah"
                )
            }
        }
    }
}

/**
 * Score screen with two exits: "Main Lagi" starts a fresh session (same
 * scoping, re-shuffled), "Selesai" leaves the quiz. Score sits above both so
 * the primary action is the replay — [onPlayAgain] has a default only so
 * existing callers/tests that just care about the score don't have to care.
 */
@Composable
internal fun ResultState(
    score: Int,
    total: Int,
    onBack: () -> Unit,
    onPlayAgain: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(Dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(bottom = Dimensions.spacingLg)
        ) {
            Text(
                text = "$score / $total",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = Dimensions.spacingXl, vertical = Dimensions.spacingLg)
            )
        }
        Text(
            text = "Skor Anda",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimensions.spacingLg))
        Button(
            onClick = onPlayAgain.withClickSound(),
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.buttonHeight)
        ) {
            Text("Main Lagi")
        }
        Spacer(modifier = Modifier.height(Dimensions.spacingMd))
        OutlinedButton(
            onClick = onBack.withClickSound(),
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.buttonHeight)
        ) {
            Text("Selesai")
        }
    }
}
