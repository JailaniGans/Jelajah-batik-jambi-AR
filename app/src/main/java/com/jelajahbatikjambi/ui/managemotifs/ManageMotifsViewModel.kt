package com.jelajahbatikjambi.ui.managemotifs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.data.repository.DiscoveryRepository
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ManageMotifRow(
    val batik: BatikData,
    /** Whether this motif has been confirmed at least once through AR scanning. */
    val discovered: Boolean,
    /** Mirrors the edit screen's rule: delete only custom motifs, and never the app's last one. */
    val canDelete: Boolean
)

data class ManageMotifsUiState(
    val rows: List<ManageMotifRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Backs the "Kelola Motif" screen — the one place that lists *every* motif
 * (including ones AR hasn't discovered yet), so a just-added motif can be
 * edited or deleted even though it isn't on the Collection grid yet. Edits
 * navigate to the existing [com.jelajahbatikjambi.ui.editmotif.EditMotifScreen];
 * deletion goes through [MotifRepository.deleteCustomMotif], which also drops
 * the motif's photo, GLB, quiz questions, discovery record, and its AR target.
 */
class ManageMotifsViewModel(application: Application) : AndroidViewModel(application) {

    private val motifRepository = MotifRepository.getInstance(application)
    private val discoveryRepository = DiscoveryRepository(AppDatabase.getInstance(application).discoveryDao())

    private val _uiState = MutableStateFlow(ManageMotifsUiState())
    val uiState: StateFlow<ManageMotifsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                motifRepository.allMotifs,
                discoveryRepository.discoveredBatikIds
            ) { motifs, discovered ->
                ManageMotifsUiState(
                    rows = motifs.map { batik ->
                        ManageMotifRow(
                            batik = batik,
                            discovered = batik.id in discovered,
                            canDelete = !motifRepository.isBuiltIn(batik.id) && motifs.size > 1
                        )
                    },
                    isLoading = false
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun deleteMotif(combinedId: Int) {
        val row = _uiState.value.rows.firstOrNull { it.batik.id == combinedId } ?: return
        if (!row.canDelete) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { motifRepository.deleteCustomMotif(combinedId) }
            }
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(error = "Gagal menghapus motif. Coba lagi.")
            }
        }
    }
}