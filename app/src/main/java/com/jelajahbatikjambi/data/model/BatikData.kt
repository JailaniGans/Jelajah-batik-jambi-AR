package com.jelajahbatikjambi.data.model

/**
 * Metadata for one Batik Jambi motif, keyed by the ArUco [markerId] that
 * reveals it in the AR scanner. See `assets/data/batik.json` for the
 * bundled content (§46) — currently placeholder text pending verification
 * with an authoritative Jambi cultural source (§38).
 */
data class BatikData(
    val id: Int,
    val markerId: Int,
    val name: String,
    val category: String,
    val shortDescription: String,
    val meaning: String,
    val history: String,
    val imagePath: String?,
    val modelPath: String?
)
