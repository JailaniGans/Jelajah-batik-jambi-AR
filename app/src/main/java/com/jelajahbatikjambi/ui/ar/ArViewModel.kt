package com.jelajahbatikjambi.ui.ar

import android.app.Application
import android.util.Size
import android.util.SizeF
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.ar.ArController
import com.jelajahbatikjambi.ar.ArState
import com.jelajahbatikjambi.ar.DebugMetrics
import com.jelajahbatikjambi.ar.DetectedRegion
import com.jelajahbatikjambi.ar.ImageTargetDetector
import com.jelajahbatikjambi.ar.MotifDetector
import com.jelajahbatikjambi.ar.Pose
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.data.repository.DiscoveryRepository
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.ui.common.SoundEffects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Bridges the AR engine ([ArController]) and the Batik content
 * ([MotifRepository]) to the UI: forwards camera frames in, exposes tracking
 * state / pose / the currently confirmed [BatikData] out. Also saves a
 * [DiscoveryRepository] record the first time each motif is confirmed
 * (§16 — "Marker detected → Batik ditemukan → Save discovery → Collection
 * unlocked"). Scoped to the nav back stack entry, so it survives
 * configuration changes unlike a plain `remember` in [ArScreen] would.
 *
 * Detection targets the real motif photos directly (per explicit user
 * request, after repeated testing showed that's what's actually wanted,
 * rather than a separate printed ArUco marker) via [ImageTargetDetector],
 * built from [MotifRepository]'s own `imagePath`/`markerId` fields — a
 * single source of truth instead of a second hardcoded image list. Its FPS
 * target is far lower than ArUco's default: ORB feature matching + RANSAC
 * homography against multiple reference images is considerably more
 * expensive per frame than ArUco's grid-based detection.
 *
 * [MotifRepository] feeds *both* sources into the detector: custom motifs
 * the user uploads (§ user request: "tambahkan motif dengan upload .jpg")
 * and replacement photos for edited built-in motifs arrive through the same
 * live Flow, diffed by `imagePath` and pushed in via
 * [ImageTargetDetector.updateReferenceImages] — so a freshly added or
 * freshly re-photoed motif is scannable immediately, including while this
 * screen is already open, with no detector rebuild and no restart.
 *
 * [pose] and [detectedBatik] are "sticky" (§ user request: don't let the 3D
 * object/info panel/quiz button vanish just because the camera moved and
 * tracking briefly dropped out) — [ArController] itself still reports
 * [ArState.Searching]/[ArState.MarkerLost] truthfully for the status pill
 * and scan reticle, but once a marker has been confirmed at least once, the
 * last known pose and matched [BatikData] are kept here rather than cleared,
 * updating only when a *different* marker is subsequently confirmed.
 */
class ArViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val discoveryRepository = DiscoveryRepository(database.discoveryDao())
    private val motifRepository = MotifRepository.getInstance(application)

    // The single app-wide detector owned by MotifRepository: built-ins (none
    // since the bundle was emptied) load at construction, and new custom
    // motifs are pre-registered at save time by the Add Motif flow. This
    // screen only pushes live changes into that same instance.
    private val motifDetector: MotifDetector = motifRepository.motifDetector

    private val controller = ArController(motifDetector = motifDetector, targetFps = IMAGE_TARGET_FPS)

    val state: StateFlow<ArState> = controller.state
    val debugMetrics: StateFlow<DebugMetrics> = controller.debugMetrics
    val detectedRegion: StateFlow<DetectedRegion?> = controller.detectedRegion

    // Keeps the last non-null pose instead of snapping back to null the
    // moment ArController clears it (Searching) — the 3D object stays put
    // on screen (and stays interactive/rotatable) through a lost-tracking
    // blip instead of disappearing and reappearing.
    val pose: StateFlow<Pose?> = controller.pose
        .scan(null as Pose?) { previous, current -> current ?: previous }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Same stickiness for which marker is considered "confirmed": once set,
    // only a genuinely different marker being confirmed replaces it — a
    // dropout that falls back to Searching/MarkerLost does not clear it.
    private val stickyMarkerId: StateFlow<Int?> = controller.state
        .map { (it as? ArState.Tracking)?.markerId }
        .scan(null as Int?) { previous, current -> current ?: previous }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * True from the *first* confirmed marker of this AR session onwards,
     * and never back to false while the screen stays open (§ user request:
     * once a motif has been found, keep presenting it as found even after
     * the camera moves away and tracking is genuinely lost again).
     *
     * Deliberately session-scoped: this ViewModel is bound to the AR nav
     * back-stack entry, so leaving the screen clears it and the next visit
     * starts fresh from "Mencari motif...". Note it's the *presentation*
     * that's sticky — [state] itself keeps reporting Searching/MarkerLost
     * truthfully for the reticle and anything that cares about live tracking.
     */
    val hasDiscovered: StateFlow<Boolean> = stickyMarkerId
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Built-in motifs (with the user's edits applied) plus any custom ones
    // they've uploaded — kept as its own StateFlow so detectedBatik can
    // resolve a tracked markerId to BatikData for either source uniformly.
    private val allMotifs: StateFlow<List<BatikData>> = motifRepository.allMotifs
        .stateIn(viewModelScope, SharingStarted.Eagerly, motifRepository.builtInMotifs)

    /**
     * Whether at least one motif is registered for scanning. On a fresh
     * install there is nothing to detect (no bundled motifs anymore), so the
     * AR screen says so instead of pointing the user at invisible targets.
     */
    val hasRegisteredMotifs: StateFlow<Boolean> = allMotifs
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The last confirmed marker's [BatikData] — drives the AR panel and quiz shortcut (§9, §12). */
    val detectedBatik: StateFlow<BatikData?> = combine(stickyMarkerId, allMotifs) { markerId, motifs ->
        markerId?.let { id -> motifs.firstOrNull { it.markerId == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // markerId -> reference photo currently loaded into the detector. Only
    // ever touched from the sync coroutine below (plus seeding here in init,
    // which happens-before it starts), so a plain map is enough.
    private val registeredReferences = mutableMapOf<Int, String>()

    init {
        // The detector was just built from these paths, so they count as
        // registered — only a *changed* photo (an edit) or a new motif will
        // be pushed in afterwards.
        motifRepository.builtInMotifs.forEach { batik ->
            batik.imagePath?.let { registeredReferences[batik.markerId] = it }
        }

        detectedBatik
            .filterNotNull()
            .distinctUntilChangedBy { it.id }
            .onEach {
                discoveryRepository.markDiscovered(it.id)
                SoundEffects.playSuccess()
            }
            .launchIn(viewModelScope)

        // Kept in sync with the same single source of truth the rest of the
        // app reads: built-in edits and newly-added custom motifs both show
        // up as an imagePath this detector hasn't seen yet. Off the main
        // thread because updating means decoding a photo and running ORB.
        viewModelScope.launch(Dispatchers.Default) {
            motifRepository.allMotifs.collect { motifs -> syncDetectorReferences(motifs) }
        }
    }

    /**
     * Pushes reference photos that changed (or haven't been loaded yet) into
     * the live detector. The reverse case — a photo reverting to the bundled
     * asset after "Kembalikan ke asli" — is caught by the same diff: the
     * asset path differs from the edited path that's registered, so the
     * original photo is loaded back in.
     */
    private fun syncDetectorReferences(motifs: List<BatikData>) {
        // A deleted motif's photo must stop matching too — diff against the
        // registered set, not just the current one.
        val currentIds = motifs.mapTo(mutableSetOf()) { it.markerId }
        val removedIds = registeredReferences.keys.filter { it !in currentIds }
        if (removedIds.isNotEmpty()) {
            motifRepository.removeDetectorReferenceImages(removedIds.toSet())
            removedIds.forEach { registeredReferences.remove(it) }
        }

        val changed = motifs.mapNotNull { motif ->
            val path = motif.imagePath ?: return@mapNotNull null
            if (registeredReferences[motif.markerId] == path) {
                null
            } else {
                ImageTargetDetector.ReferenceImage(id = motif.markerId, name = motif.name, path = path) to path
            }
        }
        if (changed.isEmpty()) return

        motifRepository.updateDetectorReferenceImages(changed.map { it.first })
        changed.forEach { (reference, path) -> registeredReferences[reference.id] = path }
    }

    fun onCameraFrame(image: ImageProxy) {
        controller.onFrame(image)
    }

    fun setCameraCharacteristics(
        focalLengthMm: Float?,
        sensorPhysicalSize: SizeF?,
        sensorPixelArraySize: Size?
    ) {
        controller.setCameraCharacteristics(focalLengthMm, sensorPhysicalSize, sensorPixelArraySize)
    }

    private companion object {
        // ORB feature detection + descriptor matching + RANSAC homography
        // against multiple reference images is meaningfully more expensive
        // per frame than ArUco's grid-based detection.
        const val IMAGE_TARGET_FPS = 10
    }
}
