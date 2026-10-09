package com.jelajahbatikjambi.data.model

/**
 * Metadata for one Batik Jambi motif, keyed by the ArUco [markerId] that
 * reveals it in the AR scanner. `assets/data/batik.json` is now empty — the
 * app ships with no bundled motifs (§46): every motif is registered by the
 * user through the Tambah Motif flow.
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
