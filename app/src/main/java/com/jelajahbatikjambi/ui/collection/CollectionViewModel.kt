package com.jelajahbatikjambi.ui.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.data.repository.DiscoveryRepository
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.database.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CollectionUiState(
    val discovered: List<BatikData> = emptyList(),
    val totalCount: Int = 0
)

/**
 * Exposes only the motifs the user has discovered so far (§15), plus the
 * total for a progress readout. Reads every motif from [MotifRepository] —
 * built-in (including any edits the user has saved), plus user-uploaded
 * custom motifs — so the grid and total count reflect both sources and
 * update live when a motif is edited.
 */
class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val discoveryRepository = DiscoveryRepository(database.discoveryDao())
    private val motifRepository = MotifRepository.getInstance(application)

    val uiState: StateFlow<CollectionUiState> = combine(
        discoveryRepository.discoveredBatikIds,
        motifRepository.allMotifs
    ) { discoveredIds, allBatik ->
        CollectionUiState(
            discovered = allBatik.filter { it.id in discoveredIds },
            totalCount = allBatik.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionUiState())
}
