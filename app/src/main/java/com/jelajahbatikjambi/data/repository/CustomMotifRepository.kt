package com.jelajahbatikjambi.data.repository

import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.database.CustomMotifDao
import com.jelajahbatikjambi.database.CustomMotifEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offset applied to a [CustomMotifEntity]'s Room-generated id so it can share
 * the same id space as built-in [BatikData] records (ids 1..N from
 * `batik.json`) without ever colliding. Used as both [BatikData.id] and
 * [BatikData.markerId] (and as the
 * [com.jelajahbatikjambi.ar.ImageTargetDetector.ReferenceImage] id) for a
 * custom motif, so a single id looks it up consistently everywhere
 * (Collection, Detail, AR detection, quiz).
 */
private const val CUSTOM_ID_OFFSET = 1000

/** Shown for [BatikData.meaning]/[BatikData.history] the user hasn't filled in yet (§38). */
private const val UNVERIFIED_MEANING =
    "Motif ini ditambahkan sendiri oleh pengguna; makna budaya belum diverifikasi."
private const val UNVERIFIED_HISTORY =
    "Motif ini ditambahkan sendiri oleh pengguna; riwayat budaya belum diverifikasi."

/**
 * Wraps [CustomMotifDao], exposing user-uploaded motifs (§ user request:
 * "tambahkan motif dengan upload .jpg") as plain [BatikData] so every screen
 * that already consumes [BatikRepository]'s output can merge the two sources
 * without any custom-motif-specific branching.
 */
class CustomMotifRepository(private val dao: CustomMotifDao) {

    fun observeAllAsBatikData(): Flow<List<BatikData>> =
        dao.observeAll().map { entities -> entities.map { it.toBatikData() } }

    suspend fun getAllOnceAsBatikData(): List<BatikData> =
        dao.getAllOnce().map { it.toBatikData() }

    suspend fun addMotif(
        name: String,
        category: String,
        shortDescription: String,
        imagePath: String,
        modelPath: String
    ): Long = dao.insert(
        CustomMotifEntity(
            name = name,
            category = category,
            shortDescription = shortDescription,
            imagePath = imagePath,
            modelPath = modelPath,
            createdAt = System.currentTimeMillis()
        )
    )

    /**
     * Looks up the raw entity behind a combined (offset) id — the edit screen
     * needs the stored file paths, not the merged [BatikData] view, so it can
     * overwrite the same photo/GLB files in place.
     */
    suspend fun getByCombinedId(combinedId: Int): CustomMotifEntity? {
        val localId = combinedId - CUSTOM_ID_OFFSET
        if (localId < 0) return null
        return dao.getById(localId.toLong())
    }

    /** Persists an edit to an existing custom motif (paths unchanged unless the caller rewrote the files). */
    suspend fun updateMotif(motif: CustomMotifEntity) = dao.update(motif)
}

private fun CustomMotifEntity.toBatikData(): BatikData {
    val combinedId = CUSTOM_ID_OFFSET + id.toInt()
    return BatikData(
        id = combinedId,
        markerId = combinedId,
        name = name,
        category = category,
        shortDescription = shortDescription,
        // Honest placeholder rather than fabricated cultural fact (§38) — a
        // user-uploaded motif has no verified meaning/history behind it, and
        // the edit screen only replaces these once the user actually types
        // something (a cleared field falls back to the placeholder again).
        meaning = meaning?.takeIf { it.isNotBlank() } ?: UNVERIFIED_MEANING,
        history = history?.takeIf { it.isNotBlank() } ?: UNVERIFIED_HISTORY,
        imagePath = imagePath,
        modelPath = modelPath
    )
}
