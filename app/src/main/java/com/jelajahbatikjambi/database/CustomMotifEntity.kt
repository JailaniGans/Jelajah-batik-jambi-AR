package com.jelajahbatikjambi.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A motif the user added themselves by uploading a photo (§ user request —
 * "tambahkan motif dengan upload .jpg"), as opposed to one of the 4 built-in
 * motifs bundled in `assets/data/batik.json`. [imagePath]/[modelPath] are
 * absolute file paths in app-private internal storage (the uploaded photo,
 * and a textured cube GLB generated on-device from it via
 * [com.jelajahbatikjambi.render.TexturedCubeGlbGenerator]) — never
 * `assets/`-relative paths, which is how consumers distinguish the two
 * (see [com.jelajahbatikjambi.ui.common.AssetImage]).
 *
 * [meaning]/[history] are null until the user fills them in on the edit
 * screen (they were added in schema v4); null surfaces as the honest
 * "not verified" placeholder instead of invented cultural content (§38).
 * Files are overwritten in place on edit rather than replaced by a new
 * uuid, so no orphaned photo/model accumulates per edit.
 */
@Entity(tableName = "custom_motifs")
data class CustomMotifEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val shortDescription: String,
    val imagePath: String,
    val modelPath: String,
    val meaning: String? = null,
    val history: String? = null,
    val createdAt: Long
)
