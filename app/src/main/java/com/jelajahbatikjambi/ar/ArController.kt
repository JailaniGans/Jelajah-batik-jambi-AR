package com.jelajahbatikjambi.ar

import android.util.Log
import android.util.Size
import android.util.SizeF
import androidx.camera.core.ImageProxy
import com.jelajahbatikjambi.camera.OpenCvImageConverter
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AR"
private const val DEFAULT_TARGET_FPS = 30

/**
 * Orchestrates the marker-detection and pose pipeline: camera frames go in
 * via [onFrame] (called from the CameraX analysis thread), [state], [pose],
 * [detectedRegion] and [debugMetrics] come out for the UI to observe. Holds
 * no reference to Compose or Activity types, per the engine/UI separation
 * the project follows.
 *
 * Detection/lost debouncing (§36-37) is delegated to [MarkerConfirmationTracker].
 * [targetFps] caps how often frames are actually processed (§35) — CameraX can
 * deliver frames faster than detection needs to run; anything above the cap
 * is skipped here (the [ImageProxy] is still closed normally by
 * [com.jelajahbatikjambi.camera.CameraAnalyzer]).
 *
 * [motifDetector] is injected rather than hardcoded so the caller ([com.jelajahbatikjambi.ui.ar.ArViewModel])
 * decides whether printed ArUco markers or real motif photos ([ImageTargetDetector])
 * are being tracked — this class doesn't care which.
 */
class ArController(
    private val motifDetector: MotifDetector,
    detectionConfirmationFrames: Int = 2,
    lostConfirmationFrames: Int = 5,
    targetFps: Int = DEFAULT_TARGET_FPS
) {

    private val _state = MutableStateFlow<ArState>(
        if (OpenCvSupport.isAvailable) ArState.Searching else ArState.Initializing
    )
    val state: StateFlow<ArState> = _state.asStateFlow()

    private val _pose = MutableStateFlow<Pose?>(null)
    val pose: StateFlow<Pose?> = _pose.asStateFlow()

    private val _debugMetrics = MutableStateFlow(DebugMetrics())
    val debugMetrics: StateFlow<DebugMetrics> = _debugMetrics.asStateFlow()

    private val _detectedRegion = MutableStateFlow<DetectedRegion?>(null)
    /** The raw matched quad for this frame, regardless of confirmation state — drives the live scan-box overlay. */
    val detectedRegion: StateFlow<DetectedRegion?> = _detectedRegion.asStateFlow()

    private val poseEstimator = PoseEstimator()
    private val poseSmoother = PoseSmoother()
    private val confirmationTracker = MarkerConfirmationTracker(detectionConfirmationFrames, lostConfirmationFrames)

    private val minFrameIntervalNanos = 1_000_000_000L / targetFps
    private var lastProcessedFrameNanos = 0L
    private var framesInWindow = 0
    private var windowStartNanos = 0L

    // Set once from Camera2 characteristics when the camera binds (see ArScreen);
    // null fields fall back to CameraIntrinsics.estimate.
    @Volatile private var focalLengthMm: Float? = null
    @Volatile private var sensorPhysicalSize: SizeF? = null
    @Volatile private var sensorPixelArraySize: Size? = null

    private var cachedIntrinsics: CameraIntrinsics? = null
    private var cachedIntrinsicsSize: Pair<Int, Int>? = null

    fun setCameraCharacteristics(
        focalLengthMm: Float?,
        sensorPhysicalSize: SizeF?,
        sensorPixelArraySize: Size?
    ) {
        this.focalLengthMm = focalLengthMm
        this.sensorPhysicalSize = sensorPhysicalSize
        this.sensorPixelArraySize = sensorPixelArraySize
    }

    fun onFrame(image: ImageProxy) {
        if (!OpenCvSupport.isAvailable) {
            _state.value = ArState.Error("OpenCV tidak tersedia")
            return
        }

        val now = System.nanoTime()
        if (now - lastProcessedFrameNanos < minFrameIntervalNanos) return
        lastProcessedFrameNanos = now
        recordProcessedFrame(now)

        val mat = OpenCvImageConverter.toGrayscaleMat(image)
        try {
            val detectionStart = System.nanoTime()
            val marker = motifDetector.detect(mat).firstOrNull()
            val detectionMs = (System.nanoTime() - detectionStart) / 1_000_000

            _detectedRegion.value = marker?.let { m ->
                DetectedRegion(
                    corners = m.corners.map { (it.x / image.width).toFloat() to (it.y / image.height).toFloat() }
                )
            }

            var poseMs = 0L

            fun applyDecision(decision: MarkerConfirmationTracker.Decision) {
                when (decision) {
                    is MarkerConfirmationTracker.Decision.Searching -> {
                        _pose.value = null
                        poseSmoother.reset()
                        _state.value = ArState.Searching
                    }

                    is MarkerConfirmationTracker.Decision.Candidate -> {
                        _state.value = ArState.MarkerDetected(decision.markerId)
                    }

                    is MarkerConfirmationTracker.Decision.Lost -> {
                        // Grace period: keep the last pose so the object/panel don't vanish yet.
                        _state.value = ArState.MarkerLost(decision.markerId)
                    }

                    is MarkerConfirmationTracker.Decision.Confirmed -> {
                        // Only reachable with a non-null marker: Confirmed is only ever
                        // returned by onMarkerSeen(), never onMarkerMissing().
                        checkNotNull(marker)
                        val poseStart = System.nanoTime()
                        _state.value = trackPose(marker, image, decision.markerId)
                        poseMs = (System.nanoTime() - poseStart) / 1_000_000
                    }
                }
            }

            if (marker == null) {
                applyDecision(confirmationTracker.onMarkerMissing())
            } else {
                applyDecision(confirmationTracker.onMarkerSeen(marker.id))
            }

            _debugMetrics.value = _debugMetrics.value.copy(detectionMs = detectionMs, poseMs = poseMs)
        } catch (t: Throwable) {
            Log.e(TAG, "Marker detection/pose estimation failed", t)
            _state.value = ArState.Error("Deteksi marker gagal")
        } finally {
            mat.release()
        }
    }

    private fun trackPose(marker: MarkerResult, image: ImageProxy, markerId: Int): ArState {
        val intrinsics = resolveIntrinsics(image.width, image.height)
        val (widthMeters, heightMeters) = motifDetector.physicalSizeMeters(markerId)
        val rawPose = poseEstimator.estimate(marker, intrinsics, widthMeters, heightMeters)

        return if (rawPose != null) {
            _pose.value = poseSmoother.smooth(CoordinateConverter.toPose(rawPose))
            ArState.Tracking(markerId)
        } else {
            ArState.MarkerDetected(markerId)
        }
    }

    private fun recordProcessedFrame(nowNanos: Long) {
        if (windowStartNanos == 0L) windowStartNanos = nowNanos
        framesInWindow++

        val windowElapsedNanos = nowNanos - windowStartNanos
        if (windowElapsedNanos >= 1_000_000_000L) {
            val fps = (framesInWindow * 1_000_000_000.0 / windowElapsedNanos).roundToInt()
            _debugMetrics.value = _debugMetrics.value.copy(processingFps = fps)
            framesInWindow = 0
            windowStartNanos = nowNanos
        }
    }

    private fun resolveIntrinsics(width: Int, height: Int): CameraIntrinsics {
        val cached = cachedIntrinsics
        if (cached != null && cachedIntrinsicsSize == (width to height)) return cached

        val resolved = CameraIntrinsics.from(
            focalLengthMm = focalLengthMm,
            sensorPhysicalSize = sensorPhysicalSize,
            sensorPixelArraySize = sensorPixelArraySize,
            imageWidthPx = width,
            imageHeightPx = height
        )
        cachedIntrinsics = resolved
        cachedIntrinsicsSize = width to height
        return resolved
    }
}
