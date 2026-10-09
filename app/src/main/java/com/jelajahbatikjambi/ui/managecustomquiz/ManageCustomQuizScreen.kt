package com.jelajahbatikjambi.ui.managecustomquiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.database.CustomQuizQuestionEntity
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Lists the quiz questions the user authored themselves (§ user request:
 * "opsi untuk buat kuis nya") so each one can be edited or deleted. New
 * questions are added from the existing "Buat Soal Kuis" form. Reading and
 * writing both go through Room, so what's listed here outlives the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCustomQuizScreen(
    onBack: () -> Unit,
    onAddQuestion: () -> Unit,
    onEditQuestion: (Long) -> Unit
) {
    val viewModel: ManageCustomQuizViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var questionToDelete by remember { mutableStateOf<CustomQuizQuestionEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Soal Kuis") },
                navigationIcon = {
                    IconButton(onClick = onBack.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                uiState.questions.isEmpty() -> EmptyState(
                    onAddQuestion = onAddQuestion,
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimensions.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
                ) {
                    items(uiState.questions, key = { it.id }) { question ->
                        QuestionRow(
                            question = question,
                            motifName = uiState.motifNames[question.batikId],
                            onEdit = { onEditQuestion(question.id) }.withClickSound(),
                            onDelete = { questionToDelete = question }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = { showDeleteAllDialog = true }.withClickSound(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(Dimensions.spacingSm))
                            Text("Hapus Semua Soal")
                        }
                    }
                }
            }
        }

        questionToDelete?.let { question ->
            AlertDialog(
                onDismissRequest = { questionToDelete = null },
                title = { Text("Hapus soal kuis?") },
                text = {
                    Text("\"${question.prompt}\" akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteQuestion(question)
                            questionToDelete = null
                        }
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { questionToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Hapus semua soal kuis?") },
                text = {
                    Text(
                        "${uiState.questions.size} soal akan dihapus permanen. " +
                            "Tindakan ini tidak bisa dibatalkan."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAllQuestions()
                            showDeleteAllDialog = false
                        }
                    ) {
                        Text("Hapus Semua")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyState(onAddQuestion: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Belum ada soal buatan sendiri",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tekan tombol + di layar kuis untuk membuat soal, lalu kelola soalnya di sini.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimensions.spacingLg))
        TextButton(onClick = onAddQuestion.withClickSound()) {
            Text("Buat Soal Kuis")
        }
    }
}

@Composable
private fun QuestionRow(
    question: CustomQuizQuestionEntity,
    motifName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingXs))
                Text(
                    // Legacy untied rows (-1) still exist from before ties
                    // became mandatory; every new question names a motif.
                    text = if (question.batikId >= 0) {
                        "Motif: ${motifName ?: "—"}"
                    } else {
                        "Tanpa motif (soal lama)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Jawaban benar: ${question.options()[question.correctOptionIndex]}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Dimensions.spacingSm))
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit soal",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Hapus soal",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun CustomQuizQuestionEntity.options() = listOf(optionA, optionB, optionC, optionD)
