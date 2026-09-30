package com.jelajahbatikjambi.data.repository

import android.content.res.AssetManager
import com.jelajahbatikjambi.data.model.BatikData
import org.json.JSONArray

private const val BATIK_DATA_ASSET_PATH = "data/batik.json"

/**
 * Loads Batik Jambi motif metadata from the bundled `assets/data/batik.json`
 * (§46) — offline-first, no backend for the MVP (§45). Uses the built-in
 * org.json parser rather than a serialization library: the dataset is a
 * handful of small static records, so a dedicated JSON dependency isn't
 * warranted (§56).
 */
class BatikRepository(private val assetManager: AssetManager) {

    private val allBatik: List<BatikData> by lazy {
        val json = assetManager.open(BATIK_DATA_ASSET_PATH).use { it.reader().readText() }
        parseBatikJson(json)
    }

    fun getAll(): List<BatikData> = allBatik

    fun getByMarkerId(markerId: Int): BatikData? = allBatik.firstOrNull { it.markerId == markerId }

    fun getById(id: Int): BatikData? = allBatik.firstOrNull { it.id == id }
}

/**
 * Parses the `batik.json` array format into [BatikData]. Pulled out of
 * [BatikRepository] as a plain function (no [AssetManager]) so the parsing
 * logic itself is unit-testable in a local JVM test, without needing an
 * Android runtime just to read a hardcoded JSON string.
 */
fun parseBatikJson(json: String): List<BatikData> {
    val array = JSONArray(json)
    return (0 until array.length()).map { index ->
        val obj = array.getJSONObject(index)
        BatikData(
            id = obj.getInt("id"),
            markerId = obj.getInt("markerId"),
            name = obj.getString("name"),
            category = obj.getString("category"),
            shortDescription = obj.getString("shortDescription"),
            meaning = obj.getString("meaning"),
            history = obj.getString("history"),
            imagePath = obj.optString("imagePath").ifBlank { null },
            modelPath = obj.optString("modelPath").ifBlank { null }
        )
    }
}
