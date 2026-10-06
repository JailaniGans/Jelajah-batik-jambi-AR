package com.jelajahbatikjambi.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The user's edits to one of the built-in motifs bundled in
 * `assets/data/batik.json` — that file is read-only, so instead of rewriting
 * it, an edited built-in motif gets a row here which is laid over the
 * original on load (see
 * [com.jelajahbatikjambi.data.repository.applyBatikOverride]). The untouched
 * `batik.json` therefore always remains the fallback, which is what makes
 * "Kembalikan ke asli" a row deletion rather than a stored backup.
 *
 * [batikId] is the built-in motif's own id and is never editable: discovery
 * records ([DiscoveryEntity]), AR tracking ([com.jelajahbatikjambi.ar]
 * `markerId`) and quiz scoping all key off it, so edits must not renumber
 * motifs. Only display fields and the photo are user-changeable.
 *
 * [imagePath] is an absolute path under `filesDir/edited_images/` — null
 * means "no replacement photo was chosen", i.e. keep the bundled asset
 * image. When it is non-null it serves both as the displayed 2D photo and
 * as the AR detection reference (the path is picked up by
 * [com.jelajahbatikjambi.ar.ImageTargetDetector], which treats paths
 * starting with "/" as internal-storage files).
 */
@Entity(tableName = "batik_overrides")
data class BatikOverrideEntity(
    @PrimaryKey val batikId: Int,
    val name: String,
    val category: String,
    val shortDescription: String,
    val meaning: String,
    val history: String,
    val imagePath: String?,
    val updatedAt: Long
)
