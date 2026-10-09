package com.jelajahbatikjambi.ui.addmotif

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.repository.CustomMotifRepository
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.render.TexturedCubeGlbGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class AddMotifUiState(
    val previewBitmap: Bitmap? = null,
    val name: String = "",
    val category: String = "",
    val shortDescription: String = "",
    /** "Buat soal kuis untuk motif ini" section — on by default: a new motif
     *  is exactly when the user writes its question. Unticking skips it. */
    val createQuizQuestion: Boolean = true,
    val questionPrompt: String = "",
    val questionOptions: List<String> = listOf("", "", "", ""),
    val questionCorrectOptionIndex: Int = 0,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
) {
    /** The question fields only gate saving when the section is ticked. */
    val canSave: Boolean
        get() = previewBitmap != null && name.isNotBlank() && category.isNotBlank() &&
            shortDescription.isNotBlank() &&
            (!createQuizQuestion || (questionPrompt.isNotBlank() && questionOptions.all { it.isNotBlank() }))
}

/**
 * Lets the user register a new scannable motif from their own photo
 * (§ user request: "tambahkan opsi untuk tambahkan motif yang bisa di scan
 * dengan upload .jpg"): copies the picked image into app-private storage,
 * builds a textured cube GLB from it on-device via
 * [TexturedCubeGlbGenerator], and saves a [com.jelajahbatikjambi.database.CustomMotifEntity]
 * pointing at both files. [com.jelajahbatikjambi.ui.ar.ArViewModel] then picks
 * up the new motif automatically through its live Room Flow.
 *
 * The "buat soal kuis" section is ticked by default (the app has no bundled
 * questions — the user writes them, naturally when registering a new motif);
 * unticking it falls back to the add-motif-only flow. When ticked, a
 * [com.jelajahbatikjambi.database.CustomQuizQuestionEntity] keyed to the new
 * motif's combined id is written in the same coroutine — saved after the
 * motif, because the question needs that id, and in the same [runCatching] so
 * a partial save (motif without its question) reports as a failure.
 */
class AddMotifViewModel(application: Application) : AndroidViewModel(application) {

    private val customMotifRepository = CustomMotifRepository(AppDatabase.getInstance(application).customMotifDao())
    private val customQuizRepository = CustomQuizRepository(AppDatabase.getInstance(application).customQuizQuestionDao())

    private val _uiState = MutableStateFlow(AddMotifUiState())
    val uiState: StateFlow<AddMotifUiState> = _uiState.asStateFlow()

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<android.app.Application>().contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
            _uiState.value = if (bitmap != null) {
                _uiState.value.copy(previewBitmap = bitmap)
            } else {
                _uiState.value.copy(error = "Gagal membuka gambar yang dipilih.")
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onCategoryChanged(value: String) {
        _uiState.value = _uiState.value.copy(category = value)
    }

    fun onDescriptionChanged(value: String) {
        _uiState.value = _uiState.value.copy(shortDescription = value)
    }

    fun onCreateQuizQuestionToggled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(createQuizQuestion = enabled, error = null)
    }

    fun onQuestionPromptChanged(value: String) {
        _uiState.value = _uiState.value.copy(questionPrompt = value)
    }

    fun onQuestionOptionChanged(index: Int, value: String) {
        val updated = _uiState.value.questionOptions.toMutableList().also { it[index] = value }
        _uiState.value = _uiState.value.copy(questionOptions = updated)
    }

    fun onQuestionCorrectOptionSelected(index: Int) {
        _uiState.value = _uiState.value.copy(questionCorrectOptionIndex = index)
    }

    fun save() {
        val state = _uiState.value
        val bitmap = state.previewBitmap ?: return
        if (!state.canSave || state.isSaving) return

        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val context = getApplication<android.app.Application>()
                    val uuid = UUID.randomUUID().toString()

                    val imagesDir = File(context.filesDir, "custom_images").apply { mkdirs() }
                    val imageFile = File(imagesDir, "$uuid.jpg")
                    imageFile.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    val modelsDir = File(context.filesDir, "custom_models").apply { mkdirs() }
                    val modelFile = File(modelsDir, "$uuid.glb")
                    modelFile.writeBytes(TexturedCubeGlbGenerator.generate(bitmap))

                    val rawId = customMotifRepository.addMotif(
                        name = state.name.trim(),
                        category = state.category.trim(),
                        shortDescription = state.shortDescription.trim(),
                        imagePath = imageFile.absolutePath,
                        modelPath = modelFile.absolutePath
                    )

                    // The question is keyed to the motif's *combined* id (the
                    // one Detail/AR/quiz all use), which only exists once the
                    // row above has been inserted.
                    if (state.createQuizQuestion) {
                        customQuizRepository.addQuestion(
                            prompt = state.questionPrompt.trim(),
                            options = state.questionOptions.map { it.trim() },
                            correctOptionIndex = state.questionCorrectOptionIndex,
                            batikId = customMotifRepository.combinedIdOf(rawId)
                        )
                    }
                }
            }
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isSaving = false, savedSuccessfully = true)
            } else {
                _uiState.value.copy(isSaving = false, error = "Gagal menyimpan motif. Coba lagi.")
            }
        }
    }
}
