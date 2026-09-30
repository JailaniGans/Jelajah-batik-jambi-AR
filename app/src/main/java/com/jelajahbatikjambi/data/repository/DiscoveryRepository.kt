package com.jelajahbatikjambi.data.repository

import com.jelajahbatikjambi.database.DiscoveryDao
import com.jelajahbatikjambi.database.DiscoveryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tracks which Batik motifs the user has discovered via AR (§16). A motif is
 * marked discovered the first time its marker is confirmed
 * ([com.jelajahbatikjambi.ar.ArState.Tracking]); later re-scans are no-ops.
 */
class DiscoveryRepository(private val dao: DiscoveryDao) {

    val discoveredBatikIds: Flow<Set<Int>> =
        dao.observeAll().map { entities -> entities.map { it.batikId }.toSet() }

    suspend fun markDiscovered(batikId: Int) {
        dao.insert(DiscoveryEntity(batikId = batikId, discoveredAt = System.currentTimeMillis()))
    }
}
