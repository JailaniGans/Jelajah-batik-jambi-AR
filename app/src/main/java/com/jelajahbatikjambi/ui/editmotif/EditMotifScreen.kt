package com.jelajahbatikjambi.ui.editmotif

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.database.CustomQuizQuestionEntity
import com.jelajahbatikjambi.ui.common.SoundEffects
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Edit an existing motif — built-in or custom — covering text fields, a
 * replacement photo (§ user request: edit data batik, persisted on device),
 * and the quiz questions keyed to this motif (§ user request: "info motif
 * ... bisa di edit sekaligus soalnya"): each listed question opens the
 * question editor, can be deleted here, and new ones are created from this
 * screen with the motif pre-selected. Custom motifs also get "Hapus Motif"
 * (behind a confirmation dialog) when more than one motif remains.
 * The form is pre-filled from [EditMotifViewModel] (i.e. from what's
 * actually stored, edits included), saving writes to Room + internal
 * storage, and built-in motifs additionally get "Kembalikan ke asli" behind
 * a confirmation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMotifScreen(
    batikId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onEditQuestion: (Long) -> Unit,
    onAddQuestion: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: EditMotifViewModel = viewModel(
        factory = EditMotifViewModel.factory(context.applicationContext as Application, batikId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::onImagePicked)
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<CustomQuizQuestionEntity?>(null) }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            SoundEffects.playSuccess()
            onSaved()
        }
    }

    LaunchedEffect(uiState.deletedSuccessfully) {
        if (uiState.deletedSuccessfully) {
            SoundEffects.playSuccess()
            onDeleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Motif") },
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
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
        ) {
            Text(
                text = "Perubahan tersimpan di perangkat ini dan tetap ada setelah aplikasi ditutup. " +
                    "Foto baru juga dipakai untuk mengenali motif di AR.",
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
            OutlinedTextField(
                value = uiState.meaning,
                onValueChange = viewModel::onMeaningChanged,
                label = { Text("Makna") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.history,
                onValueChange = viewModel::onHistoryChanged,
                label = { Text("Sejarah") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // ---- Quiz questions keyed to this motif (live list) ----
            Text(
                text = "Soal Kuis untuk Motif Ini",
                style = MaterialTheme.typography.titleMedium
            )

            if (uiState.questions.isEmpty()) {
                Text(
                    text = "Belum ada soal untuk motif ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.questions.forEach { question ->
                QuestionCard(
                    question = question,
                    onEdit = { onEditQuestion(question.id) },
                    onDelete = { questionToDelete = question }
                )
            }

            OutlinedButton(
                onClick = onAddQuestion.withClickSound(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.spacingLg)
                )
                Spacer(modifier = Modifier.width(Dimensions.spacingSm))
                Text("Tambah Soal untuk Motif Ini")
            }

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

            // Only meaningful for a bundled motif that already has edits —
            // otherwise there is nothing to go back to.
            if (uiState.isBuiltIn && uiState.isEdited) {
                OutlinedButton(
                    onClick = { showResetDialog = true }.withClickSound(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembalikan ke asli")
                }
            }

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Kembalikan ke asli?") },
                    text = {
                        Text("Semua perubahan pada motif ini — termasuk foto yang diganti — akan dihapus dan motif kembali ke data bawaan aplikasi.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showResetDialog = false
                                viewModel.resetToOriginal()
                            }
                        ) {
                            Text("Kembalikan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Custom motifs only, and only while more than one motif exists —
            // the app never gets emptied out (§ user request).
            if (uiState.canDelete) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true }.withClickSound(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (uiState.isDeleting) "Menghapus..." else "Hapus Motif")
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Hapus motif ini?") },
                    text = {
                        Text(
                            "\"${uiState.name}\" beserta foto, model 3D, catatan penemuan, " +
                                "dan soal kuis terkaitnya akan dihapus permanen. " +
                                "Tindakan ini tidak bisa dibatalkan."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.deleteMotif()
                            }
                        ) {
                            Text("Hapus", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
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
                            Text("Hapus", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { questionToDelete = null }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}

/**
 * One row of the "Soal Kuis untuk Motif Ini" list: the question itself,
 * its correct answer for context, and the two things this screen can do
 * with it — open it in the question editor, or delete it (the motif stays).
 */
@Composable
private fun QuestionCard(
    question: CustomQuizQuestionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimensions.spacingMd)) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Dimensions.spacingXs))
            Text(
                text = "Jawaban benar: ${question.options()[question.correctOptionIndex]}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXs)
            ) {
                TextButton(onClick = onEdit.withClickSound()) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.spacingLg)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.spacingXs))
                    Text("Edit")
                }
                TextButton(
                    onClick = onDelete.withClickSound(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.spacingLg)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.spacingXs))
                    Text("Hapus")
                }
            }
        }
    }
}

private fun CustomQuizQuestionEntity.options() =
    listOf(optionA, optionB, optionC, optionD)

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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .padding(Dimensions.spacingSm)
            ) {
                Text(
                    text = "Ketuk untuk mengganti foto",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
