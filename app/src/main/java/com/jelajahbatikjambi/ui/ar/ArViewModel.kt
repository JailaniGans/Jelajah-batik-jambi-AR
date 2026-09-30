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
import com.jelajahbatikjambi.data.repository.BatikRepository
import com.jelajahbatikjambi.data.repository.CustomMotifRepository
import com.jelajahbatikjambi.data.repository.DiscoveryRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.ui.common.SoundEffects
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

/**
 * Bridges the AR engine ([ArController]) and the Batik content
 * ([BatikRepository]) to the UI: forwards camera frames in, exposes tracking
 * state / pose / the currently confirmed [BatikData] out. Also saves a
 * [DiscoveryRepository] record the first time each motif is confirmed
 * (§16 — "Marker detected → Batik ditemukan → Save discovery → Collection
 * unlocked"). Scoped to the nav back stack entry, so it survives
 * configuration changes unlike a plain `remember` in [ArScreen] would.
 *
 * Detection targets the real motif photos directly (per explicit user
 * request, after repeated testing showed that's what's actually wanted,
 * rather than a separate printed ArUco marker) via [ImageTargetDetector],
 * built from [BatikRepository]'s own `imagePath`/`markerId` fields — a single
 * source of truth instead of a second hardcoded image list. Its FPS target is
 * far lower than ArUco's default: ORB feature matching + RANSAC homography
 * against multiple reference images is considerably more expensive per frame
 * than ArUco's grid-based detection.
 *
 * User-uploaded custom motifs (§ user request: "tambahkan motif dengan
 * upload .jpg") are registered on top of the built-in set: the detector still
 * constructs synchronously from the built-in motifs so AR startup has no new
 * latency, then [CustomMotifRepository]'s live Flow feeds any custom motifs
 * in via [ImageTargetDetector.addReferenceImages] as they appear — including
 * ones added while this screen is already open.
 *
 * [pose] and [detectedBatik] are "sticky" (§ user request: don't let the 3D
 * object/info panel/quiz button vanish just because the camera moved and
 * tracking briefly dropped out) — [ArController] itself still reports
 * [ArState.Searching]/[ArState.MarkerLost] truthfully for the status pill and
 * scan reticle, but once a marker has been confirmed at least once, the last
 * known pose and matched [BatikData] are kept here rather than cleared,
 * updating only when a *different* marker is subsequently confirmed.
 */
class ArViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BatikRepository(application.assets)
    private val database = AppDatabase.getInstance(application)
    private val discoveryRepository = DiscoveryRepository(database.discoveryDao())
    private val customMotifRepository = CustomMotifRepository(database.customMotifDao())

    private val imageTargetDetector = ImageTargetDetector(
        assetManager = application.assets,
        referenceImages = repository.getAll().mapNotNull { batik ->
            batik.imagePath?.let { path ->
                ImageTargetDetector.ReferenceImage(id = batik.markerId, name = batik.name, path = path)
            }
        }
    )
    private val motifDetector: MotifDetector = imageTargetDetector

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

    // Built-in motifs loaded once, plus any custom ones the user has
    // uploaded — kept as its own StateFlow so detectedBatik can resolve a
    // tracked markerId to BatikData for either source uniformly.
    private val allMotifs: StateFlow<List<BatikData>> = customMotifRepository.observeAllAsBatikData()
        .map { custom -> repository.getAll() + custom }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.getAll())

    /** The last confirmed marker's [BatikData] — drives the AR panel and quiz shortcut (§9, §12). */
    val detectedBatik: StateFlow<BatikData?> = combine(stickyMarkerId, allMotifs) { markerId, motifs ->
        markerId?.let { id -> motifs.firstOrNull { it.markerId == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        detectedBatik
            .filterNotNull()
            .distinctUntilChangedBy { it.id }
            .onEach {
                discoveryRepository.markDiscovered(it.id)
                SoundEffects.playSuccess()
            }
            .launchIn(viewModelScope)

        // Feed newly-added custom motifs into the live detector as they
        // appear, without rebuilding it (addReferenceImages only appends).
        var registeredMarkerIds = emptySet<Int>()
        customMotifRepository.observeAllAsBatikData()
            .onEach { customMotifs ->
                val newOnes = customMotifs.filter { it.markerId !in registeredMarkerIds }
                if (newOnes.isNotEmpty()) {
                    imageTargetDetector.addReferenceImages(
                        newOnes.mapNotNull { motif ->
                            motif.imagePath?.let { path ->
                                ImageTargetDetector.ReferenceImage(id = motif.markerId, name = motif.name, path = path)
                            }
                        }
                    )
                    registeredMarkerIds = registeredMarkerIds + newOnes.map { it.markerId }
                }
            }
            .launchIn(viewModelScope)
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
