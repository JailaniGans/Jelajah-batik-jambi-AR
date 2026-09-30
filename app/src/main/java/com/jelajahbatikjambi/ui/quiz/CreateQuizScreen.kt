package com.jelajahbatikjambi.ui.quiz

import androidx.compose.foundation.layout.Arrangement
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.ui.common.SoundEffects
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Lets the user author their own "pilihan ganda" quiz question
 * (§ user request: "opsi untuk buat kuis nya") — a prompt, four options, and
 * which one is correct. Saved questions are folded into every future quiz
 * session alongside the auto-generated "guess the motif" ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val viewModel: CreateQuizViewModel = viewModel()
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
                title = { Text("Buat Soal Kuis") },
                navigationIcon = {
                    IconButton(onClick = onBack.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimensions.spacingLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
        ) {
            Text(
                text = "Buat soal pilihan ganda Anda sendiri untuk ditambahkan ke sesi kuis.",
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
                        onClick = { viewModel.onCorrectOptionSelected(index) }
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

            Button(
                onClick = { viewModel.save() }.withClickSound(),
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight)
            ) {
                Text("Simpan Soal")
            }
        }
    }
}
