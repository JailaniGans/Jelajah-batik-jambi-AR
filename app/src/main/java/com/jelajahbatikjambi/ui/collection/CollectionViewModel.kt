package com.jelajahbatikjambi.ui.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.data.repository.BatikRepository
import com.jelajahbatikjambi.data.repository.CustomMotifRepository
import com.jelajahbatikjambi.data.repository.DiscoveryRepository
import com.jelajahbatikjambi.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CollectionUiState(
    val discovered: List<BatikData> = emptyList(),
    val totalCount: Int = 0
)

/**
 * Exposes only the motifs the user has discovered so far (§15), plus the
 * total for a progress readout. Includes user-uploaded custom motifs
 * alongside the built-in ones (§ user request: "tambahkan motif dengan
 * upload .jpg") so the collection grid and total count reflect both sources.
 */
class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val batikRepository = BatikRepository(application.assets)
    private val database = AppDatabase.getInstance(application)
    private val discoveryRepository = DiscoveryRepository(database.discoveryDao())
    private val customMotifRepository = CustomMotifRepository(database.customMotifDao())

    private val allMotifs: Flow<List<BatikData>> = customMotifRepository.observeAllAsBatikData()
        .map { custom -> batikRepository.getAll() + custom }

    val uiState: StateFlow<CollectionUiState> = combine(
        discoveryRepository.discoveredBatikIds,
        allMotifs
    ) { discoveredIds, allBatik ->
        CollectionUiState(
            discovered = allBatik.filter { it.id in discoveredIds },
            totalCount = allBatik.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionUiState())
}
