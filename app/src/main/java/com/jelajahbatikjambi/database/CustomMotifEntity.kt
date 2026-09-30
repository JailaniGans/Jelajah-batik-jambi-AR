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
 */
@Entity(tableName = "custom_motifs")
data class CustomMotifEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val shortDescription: String,
    val imagePath: String,
    val modelPath: String,
    val createdAt: Long
)
