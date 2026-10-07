package com.jelajahbatikjambi.ui.editquizquestion

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.ui.common.SoundEffects
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Edits a quiz question the user authored themselves — same form as
 * [com.jelajahbatikjambi.ui.quiz.CreateQuizScreen], pre-filled with what is
 * actually stored, saving through Room so the edit outlives the app.
 * Read-only for built-in content: only user-authored questions get here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuizQuestionScreen(questionId: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val viewModel: EditQuizQuestionViewModel = viewModel(
        factory = EditQuizQuestionViewModel.factory(
            context.applicationContext as Application,
            questionId
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            SoundEffects.playSuccess()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Soal Kuis") },
                navigationIcon = {
                    IconButton(onClick = onBack.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimensions.spacingLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
        ) {
            Text(
                text = "Perubahan tersimpan di perangkat ini dan tetap ada setelah aplikasi ditutup.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = viewModel::onPromptChanged,
                label = { Text("Pertanyaan") },
                modifier = Modifier.fillMaxWidth()
            )

            uiState.options.forEachIndexed { index, option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = uiState.correctOptionIndex == index,
                        onClick = { viewModel.onCorrectOptionSelected(index) }.withClickSound()
                    )
                    OutlinedTextField(
                        value = option,
                        onValueChange = { viewModel.onOptionChanged(index, it) },
                        label = { Text("Pilihan ${index + 1}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                text = "Pilih tombol bulat di samping jawaban yang benar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { viewModel.save() }.withClickSound(),
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(Dimensions.spacingLg),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Simpan Perubahan")
                }
            }
        }
    }
}
