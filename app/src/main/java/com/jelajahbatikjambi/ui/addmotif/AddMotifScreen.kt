package com.jelajahbatikjambi.ui.addmotif

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.ui.common.SoundEffects
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Lets the user upload their own motif photo to make it scannable in AR
 * (§ user request: "tambahkan opsi untuk tambahkan motif yang bisa di scan
 * dengan upload .jpg"). [ActivityResultContracts.GetContent] needs no storage
 * permission on any supported API level, unlike a raw MediaStore/file picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMotifScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val viewModel: AddMotifViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::onImagePicked)
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            SoundEffects.playSuccess()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Motif") },
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(Dimensions.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
        ) {
            Text(
                text = "Unggah foto motif batik agar bisa dikenali kamera AR dan tampil sebagai objek 3D.",
                style = MaterialTheme.typography.bodyMedium
            )

            ImagePickerBox(
                previewBitmap = uiState.previewBitmap,
                onClick = { imagePicker.launch("image/*") }.withClickSound()
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Nama motif") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.category,
                onValueChange = viewModel::onCategoryChanged,
                label = { Text("Kategori") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.shortDescription,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Deskripsi singkat") },
                modifier = Modifier.fillMaxWidth()
            )

            QuizQuestionSection(
                enabled = uiState.createQuizQuestion,
                prompt = uiState.questionPrompt,
                options = uiState.questionOptions,
                correctOptionIndex = uiState.questionCorrectOptionIndex,
                onEnabledChanged = viewModel::onCreateQuizQuestionToggled,
                onPromptChanged = viewModel::onQuestionPromptChanged,
                onOptionChanged = viewModel::onQuestionOptionChanged,
                onCorrectOptionChanged = viewModel::onQuestionCorrectOptionSelected
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
                    Text("Simpan Motif")
                }
            }
        }
    }
}

@Composable
private fun ImagePickerBox(previewBitmap: android.graphics.Bitmap?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(Dimensions.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Pratinjau motif",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ketuk untuk memilih foto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * "Buat soal kuis untuk motif ini" block on the add-motif form. The checkbox
 * defaults to on — registering a new motif is when the user writes its quiz
 * question — and unticking it collapses the section back to the
 * add-motif-only flow. When shown, it's the same prompt/options/correct-answer
 * inputs as [com.jelajahbatikjambi.ui.quiz.CreateQuizScreen], saved together
 * with the motif and keyed to it.
 */
@Composable
private fun QuizQuestionSection(
    enabled: Boolean,
    prompt: String,
    options: List<String>,
    correctOptionIndex: Int,
    onEnabledChanged: (Boolean) -> Unit,
    onPromptChanged: (String) -> Unit,
    onOptionChanged: (Int, String) -> Unit,
    onCorrectOptionChanged: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingSm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = enabled,
                // withClickSound only wraps () -> Unit; the checkbox callback
                // carries a Boolean, so the tick is played manually here.
                onCheckedChange = {
                    SoundEffects.playClick()
                    onEnabledChanged(it)
                }
            )
            Spacer(modifier = Modifier.width(Dimensions.spacingSm))
            Text(
                text = "Buat soal kuis untuk motif ini",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (!enabled) return@Column

        Text(
            text = "Soal ini akan ditautkan ke motif dan muncul saat kuis motif ini.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChanged,
            label = { Text("Pertanyaan") },
            modifier = Modifier.fillMaxWidth()
        )

        options.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = correctOptionIndex == index,
                    onClick = { onCorrectOptionChanged(index) }.withClickSound()
                )
                OutlinedTextField(
                    value = option,
                    onValueChange = { onOptionChanged(index, it) },
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
    }
}
