package com.jelajahbatikjambi.ui.editmotif

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.database.CustomQuizQuestionEntity
import com.jelajahbatikjambi.render.TexturedCubeGlbGenerator
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditMotifUiState(
    val isLoading: Boolean = true,
    val batikId: Int = -1,
    /** True when editing a bundled `batik.json` motif (saves as an override row) rather than a custom one. */
    val isBuiltIn: Boolean = false,
    /** Built-in only: an edit is already stored, so "Kembalikan ke asli" has something to undo. */
    val isEdited: Boolean = false,
    /** Currently effective photo (edited file, bundled asset, or uploaded file) shown before the user picks a new one. */
    val previewBitmap: Bitmap? = null,
    /** A newly chosen photo, or null to keep the current one. */
    val pickedBitmap: Bitmap? = null,
    val name: String = "",
    val category: String = "",
    val shortDescription: String = "",
    val meaning: String = "",
    val history: String = "",
    /** Quiz questions keyed to this motif, live — added/edited elsewhere shows up here. */
    val questions: List<CustomQuizQuestionEntity> = emptyList(),
    /** All motifs in the app — drives whether this one may be deleted (more than one must remain). */
    val totalMotifs: Int = 0,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val deletedSuccessfully: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
) {
    val canSave: Boolean
        get() = !isLoading && name.isNotBlank() && category.isNotBlank() && shortDescription.isNotBlank()

    /** Built-in motifs are never deletable; a custom one only while it isn't the app's last motif (§ user request). */
    val canDelete: Boolean
        get() = !isLoading && !isBuiltIn && totalMotifs > 1
}

/**
 * Drives the edit screen for both motif kinds, persisting through
 * [MotifRepository] so an edit outlives the app:
 *
 * - **Built-in** motifs can't be rewritten in `assets/`, so the save writes
 *   a photo to `filesDir/edited_images/{id}.jpg` (only when a new one was
 *   picked) and stores an overlay row — [MotifRepository.saveBuiltInEdit].
 *   "Kembalikan ke asli" deletes that row and its photo.
 * - **Custom** motifs are edited in place: the picked photo overwrites the
 *   same `custom_images/{uuid}.jpg`, and the textured-cube GLB is
 *   regenerated over the same `custom_models/{uuid}.glb`, so re-editing
 *   never orphans files.
 *
 * Either way the change lands in Room, which is why it's still there after
 * the process is killed — and why the AR detector, Collection, Detail and
 * Quiz (all reading [MotifRepository]) pick it up without a restart.
 *
 * Besides the motif's own fields, the screen manages the quiz questions
 * keyed to it (§ user request: "info motif ... bisa di edit sekaligus
 * soalnya"): the list is a live Room Flow, questions are deleted straight
 * from here, and a custom motif itself can be deleted when more than one
 * motif remains ([EditMotifUiState.canDelete]).
 */
class EditMotifViewModel(application: Application, private val batikId: Int) : AndroidViewModel(application) {

    private val motifRepository = MotifRepository.getInstance(application)
    private val customQuizRepository =
        CustomQuizRepository(AppDatabase.getInstance(application).customQuizQuestionDao())

    private val _uiState = MutableStateFlow(EditMotifUiState(batikId = batikId))
    val uiState: StateFlow<EditMotifUiState> = _uiState.asStateFlow()

    init {
        load()
        // Live: a question added (from this screen's Tambah button, Add Motif
        // or Create Quiz) or deleted elsewhere shows up without a reload —
        // and survives load() replacing the rest of the state, because it
        // only ever writes the questions field.
        viewModelScope.launch {
            customQuizRepository.observeByBatikId(batikId)
                .collect { questions -> update { it.copy(questions = questions) } }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val allMotifs = motifRepository.getAllOnce()
            val motif = allMotifs.firstOrNull { it.id == batikId }
            if (motif == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Motif tidak ditemukan."
                )
                return@launch
            }
            val isBuiltIn = motifRepository.isBuiltIn(batikId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isBuiltIn = isBuiltIn,
                isEdited = isBuiltIn && motifRepository.getOverride(batikId) != null,
                previewBitmap = motif.imagePath?.let { decodeImage(it) },
                pickedBitmap = null,
                name = motif.name,
                category = motif.category,
                shortDescription = motif.shortDescription,
                meaning = motif.meaning,
                history = motif.history,
                totalMotifs = allMotifs.size
            )
        }
    }

    fun onImagePicked(uri: android.net.Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
            _uiState.value = if (bitmap != null) {
                _uiState.value.copy(pickedBitmap = bitmap, previewBitmap = bitmap, error = null)
            } else {
                _uiState.value.copy(error = "Gagal membuka gambar yang dipilih.")
            }
        }
    }

    fun onNameChanged(value: String) = update { it.copy(name = value) }

    fun onCategoryChanged(value: String) = update { it.copy(category = value) }

    fun onDescriptionChanged(value: String) = update { it.copy(shortDescription = value) }

    fun onMeaningChanged(value: String) = update { it.copy(meaning = value) }

    fun onHistoryChanged(value: String) = update { it.copy(history = value) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return

        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (state.isBuiltIn) saveBuiltIn(state) else saveCustom(state)
                }
            }
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isSaving = false, savedSuccessfully = true)
            } else {
                _uiState.value.copy(isSaving = false, error = "Gagal menyimpan perubahan. Coba lagi.")
            }
        }
    }

    /**
     * "Kembalikan ke asli" (built-in motifs only): drops the overlay row and
     * the replacement photo, then reloads the form so it shows the original
     * `batik.json` values again. A no-op if there was nothing edited.
     */
    fun resetToOriginal() {
        val state = _uiState.value
        if (!state.isBuiltIn || !state.isEdited) return

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { motifRepository.resetOverride(batikId) }
            }
            if (result.isSuccess) {
                load()
            } else {
                _uiState.value = _uiState.value.copy(error = "Gagal mengembalikan motif. Coba lagi.")
            }
        }
    }

    /** Removes one quiz question from this motif's list (the motif itself stays). */
    fun deleteQuestion(question: CustomQuizQuestionEntity) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { customQuizRepository.deleteQuestion(question) }
            }
            if (result.isFailure) {
                update { it.copy(error = "Gagal menghapus soal. Coba lagi.") }
            }
        }
    }

    /**
     * Deletes a custom motif outright — photo, GLB, discovery record and
     * its quiz questions go with it ([MotifRepository.deleteCustomMotif]).
     * Guarded by [EditMotifUiState.canDelete]; success flips
     * [EditMotifUiState.deletedSuccessfully] so the screen can navigate away.
     */
    fun deleteMotif() {
        val state = _uiState.value
        if (!state.canDelete || state.isDeleting) return

        _uiState.value = state.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { motifRepository.deleteCustomMotif(batikId) }
            }
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isDeleting = false, deletedSuccessfully = true)
            } else {
                _uiState.value.copy(isDeleting = false, error = "Gagal menghapus motif. Coba lagi.")
            }
        }
    }

    private suspend fun saveBuiltIn(state: EditMotifUiState) {
        // null keeps whatever photo is currently in effect — picking a new
        // photo is the only thing that touches the filesystem here.
        val newImagePath = state.pickedBitmap?.let { bitmap ->
            val file = motifRepository.editedImageFile(batikId)
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            file.absolutePath
        }
        motifRepository.saveBuiltInEdit(
            batikId = batikId,
            name = state.name,
            category = state.category,
            shortDescription = state.shortDescription,
            meaning = state.meaning,
            history = state.history,
            newImageFilePath = newImagePath
        )
    }

    private suspend fun saveCustom(state: EditMotifUiState) {
        val entity = motifRepository.getCustomMotif(batikId)
            ?: error("Motif custom tidak ditemukan")
        state.pickedBitmap?.let { bitmap ->
            // Overwrite the existing files (same paths) instead of writing
            // new uuids, so each edit doesn't strand the previous photo/GLB.
            File(entity.imagePath).outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            File(entity.modelPath).writeBytes(TexturedCubeGlbGenerator.generate(bitmap))
        }
        motifRepository.saveCustomEdit(
            entity.copy(
                name = state.name.trim(),
                category = state.category.trim(),
                shortDescription = state.shortDescription.trim(),
                meaning = state.meaning.trim().ifBlank { null },
                history = state.history.trim().ifBlank { null }
            )
        )
    }

    private fun update(transform: (EditMotifUiState) -> EditMotifUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private fun decodeImage(path: String): Bitmap? = runCatching {
        if (path.startsWith("/")) {
            BitmapFactory.decodeFile(path)
        } else {
            getApplication<Application>().assets.open(path).use(BitmapFactory::decodeStream)
        }
    }.getOrNull()

    companion object {
        /** [batikId] isn't a constructor default Compose's [androidx.lifecycle.viewmodel.compose.viewModel] can supply on its own. */
        fun factory(application: Application, batikId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditMotifViewModel(application, batikId) as T
            }
    }
}
