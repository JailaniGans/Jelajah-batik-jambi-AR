package com.jelajahbatikjambi.ar

import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint2f
import org.opencv.objdetect.DetectorParameters
import org.opencv.objdetect.Dictionary
import org.opencv.objdetect.Objdetect
import org.opencv.objdetect.ArucoDetector as CvArucoDetector

/**
 * Thin wrapper around OpenCV's objdetect ArUco API.
 * Not thread-safe by itself — callers must confine use to a single thread,
 * which [com.jelajahbatikjambi.ar.ArController] does via the CameraX analysis executor.
 *
 * Uses OpenCV's "ArUco3" fast-detection mode: a coarse-to-fine search on a
 * downscaled version of the frame first, only refining on the full-resolution
 * image where a candidate was found. This is the detector's own purpose-built
 * speed mode (vs. hand-tuning threshold window/step, which trades off
 * robustness under uneven lighting) — a meaningfully faster default with no
 * accuracy cost for markers the size ours are.
 *
 * Not wired into [ArController] by default — the app currently uses
 * [ImageTargetDetector] instead (recognizing the real motif photo directly).
 * Kept available for reverting to, or combining with, printed markers later.
 */
class ArucoDetector(
    dictionaryId: Int = Objdetect.DICT_4X4_50,
    private val markerSizeMeters: Double = PoseEstimator.DEFAULT_MARKER_SIZE_METERS
) : MotifDetector {

    private val dictionary: Dictionary = Objdetect.getPredefinedDictionary(dictionaryId)
    private val parameters = DetectorParameters().apply {
        set_useAruco3Detection(true)
    }
    private val nativeDetector = CvArucoDetector(dictionary, parameters)

    override fun physicalSizeMeters(id: Int): Pair<Double, Double> = markerSizeMeters to markerSizeMeters

    override fun detect(grayscaleFrame: Mat): List<MarkerResult> {
        val corners = mutableListOf<Mat>()
        val rejected = mutableListOf<Mat>()
        val idsMat = Mat()

        nativeDetector.detectMarkers(grayscaleFrame, corners, idsMat, rejected)

        val results = if (idsMat.empty()) {
            emptyList()
        } else {
            val ids = MatOfInt(idsMat).toArray()
            corners.mapIndexed { index, cornerMat ->
                MarkerResult(
                    id = ids[index],
                    corners = MatOfPoint2f(cornerMat).toList()
                )
            }
        }

        corners.forEach { it.release() }
        rejected.forEach { it.release() }
        idsMat.release()

        return results
    }
}
