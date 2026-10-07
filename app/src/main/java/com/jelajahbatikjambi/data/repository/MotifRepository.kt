package com.jelajahbatikjambi.data.repository

import android.app.Application
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.database.BatikOverrideDao
import com.jelajahbatikjambi.database.BatikOverrideEntity
import com.jelajahbatikjambi.database.CustomMotifEntity
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Sub-directory of `filesDir` holding replacement photos for edited built-in motifs. */
private const val EDITED_IMAGE_DIR = "edited_images"

/** Sub-directories of `filesDir` the Add Motif flow writes a custom motif's photo/GLB into. */
private const val CUSTOM_IMAGE_DIR = "custom_images"
private const val CUSTOM_MODEL_DIR = "custom_models"

/**
 * The single source of truth for "what motifs exist and what do they say":
 * built-in content from the read-only `assets/data/batik.json`, with the
 * user's [BatikOverrideEntity] edits laid on top, plus user-added custom
 * motifs from Room. Every consumer (AR, Collection, Detail, Quiz, Edit)
 * reads from here so an edit shows up everywhere at once — including as a
 * live AR detection target — and survives app restarts because the edits
 * live in the database, not in memory.
 *
 * Deliberately a process-wide singleton (same pattern as
 * [AppDatabase.getInstance]): each screen would otherwise build its own
 * merge pipeline over the same two Room flows.
 */
class MotifRepository private constructor(private val application: Application) {

    private val batikRepository = BatikRepository(application.assets)
    private val database = AppDatabase.getInstance(application)
    private val customMotifRepository = CustomMotifRepository(database.customMotifDao())
    private val overrideDao: BatikOverrideDao = database.batikOverrideDao()

    /** Built-in motifs straight from `batik.json`, before any edit is applied. */
    val builtInMotifs: List<BatikData> = batikRepository.getAll()

    /** Built-in (edited) + custom motifs, in a stable order: built-ins first, then custom by creation time. */
    val allMotifs: Flow<List<BatikData>> = combine(
        overrideDao.observeAll(),
        customMotifRepository.observeAllAsBatikData()
    ) { overrides, custom ->
        mergeMotifs(builtInMotifs, overrides, custom)
    }

    /** Live view of one motif by id — emits again whenever it is edited. */
    fun motifById(batikId: Int): Flow<BatikData?> =
        allMotifs.map { motifs -> motifs.firstOrNull { it.id == batikId } }

    suspend fun getAllOnce(): List<BatikData> = allMotifs.first()

    suspend fun getOnceById(batikId: Int): BatikData? = motifById(batikId).first()

    /** Whether [batikId] refers to a bundled `batik.json` motif (editable via override) rather than a custom one. */
    fun isBuiltIn(batikId: Int): Boolean = builtInMotifs.any { it.id == batikId }

    suspend fun getOverride(batikId: Int): BatikOverrideEntity? = overrideDao.getByBatikId(batikId)

    suspend fun getCustomMotif(combinedId: Int): CustomMotifEntity? =
        customMotifRepository.getByCombinedId(combinedId)

    suspend fun saveCustomEdit(motif: CustomMotifEntity) = customMotifRepository.updateMotif(motif)

    /**
     * Where a replacement photo for built-in motif [batikId] is written. The
     * stable per-id name means re-editing overwrites the previous file
     * instead of leaking a new one, and [resetOverride] knows exactly what
     * to clean up.
     */
    fun editedImageFile(batikId: Int): File =
        File(File(application.filesDir, EDITED_IMAGE_DIR), "$batikId.jpg").apply { parentFile?.mkdirs() }

    /**
     * Stores the user's edits to a built-in motif as an overlay row.
     * [newImageFilePath] = null means "photo unchanged": keep whatever photo
     * is currently in effect (a previously edited file, or the bundled
     * asset). [batikId] and [com.jelajahbatikjambi.data.model.BatikData.markerId]
     * are never touched — see [BatikOverrideEntity].
     */
    suspend fun saveBuiltInEdit(
        batikId: Int,
        name: String,
        category: String,
        shortDescription: String,
        meaning: String,
        history: String,
        newImageFilePath: String?
    ) {
        val previous = overrideDao.getByBatikId(batikId)
        overrideDao.upsert(
            BatikOverrideEntity(
                batikId = batikId,
                name = name.trim(),
                category = category.trim(),
                shortDescription = shortDescription.trim(),
                meaning = meaning.trim(),
                history = history.trim(),
                imagePath = newImageFilePath ?: previous?.imagePath,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * "Kembalikan ke asli": drops the overlay so the motif reads from
     * `batik.json` again, and removes the replacement photo file — the
     * original bundled asset needs no restoration because it was never
     * modified.
     */
    suspend fun resetOverride(batikId: Int) {
        val previous = overrideDao.getByBatikId(batikId) ?: return
        overrideDao.deleteByBatikId(batikId)
        previous.imagePath?.let { path ->
            val file = File(path)
            // Only ever delete inside our own directory — belt and braces
            // against a stale row pointing somewhere unexpected.
            if (file.parentFile?.name == EDITED_IMAGE_DIR) {
                runCatching { file.delete() }
            }
        }
    }

    /**
     * Removes a custom motif and everything keyed to it: its photo and GLB
     * files (only ever inside our own upload directories), its quiz
     * questions, and its discovery record — nothing may dangle after the
     * row is gone. Built-in motifs aren't deletable; they live in the
     * read-only `batik.json`. Callers decide when deletion is allowed
     * (§ user request: "motif baru bisa di hapus jika motif lebih dari 1" —
     * see `canDelete` on the edit screen's state).
     */
    suspend fun deleteCustomMotif(combinedId: Int) {
        val entity = getCustomMotif(combinedId)
            ?: error("Motif custom tidak ditemukan")
        customMotifRepository.deleteMotif(entity)
        database.customQuizQuestionDao().deleteByBatikId(combinedId)
        database.discoveryDao().deleteByBatikId(combinedId)
        listOf(entity.imagePath, entity.modelPath).forEach { path ->
            val file = File(path)
            // Same belt-and-braces rule as resetOverride: only ever delete
            // inside the directories the upload flow itself created.
            if (file.parentFile?.name == CUSTOM_IMAGE_DIR || file.parentFile?.name == CUSTOM_MODEL_DIR) {
                runCatching { file.delete() }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: MotifRepository? = null

        fun getInstance(application: Application): MotifRepository =
            instance ?: synchronized(this) {
                instance ?: MotifRepository(application).also { instance = it }
            }
    }
}

/**
 * Lays one override over its built-in motif. Pure so it can be unit-tested
 * on the JVM without a database or AssetManager; a null override is the
 * no-edit case and returns [base] untouched.
 *
 * A *blank* overlay field means "left empty in the editor", not "erase the
 * content" — the original text is kept, so a half-filled form can't blank
 * out a section on the detail page.
 *
 * Only display fields and the photo move: `id`, `markerId` and `modelPath`
 * always come from [base], which is what keeps discovery records, AR
 * tracking and the bundled 3D model stable across edits.
 */
fun applyBatikOverride(base: BatikData, override: BatikOverrideEntity?): BatikData {
    if (override == null) return base
    return base.copy(
        name = override.name.ifBlank { base.name },
        category = override.category.ifBlank { base.category },
        shortDescription = override.shortDescription.ifBlank { base.shortDescription },
        meaning = override.meaning.ifBlank { base.meaning },
        history = override.history.ifBlank { base.history },
        imagePath = override.imagePath ?: base.imagePath
    )
}

/** Built-ins (each with its override applied, if any) followed by custom motifs. Pure, see [applyBatikOverride]. */
fun mergeMotifs(
    builtIn: List<BatikData>,
    overrides: List<BatikOverrideEntity>,
    custom: List<BatikData>
): List<BatikData> {
    val overridesById = overrides.associateBy { it.batikId }
    return builtIn.map { applyBatikOverride(it, overridesById[it.id]) } + custom
}
