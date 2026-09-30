package com.jelajahbatikjambi.ar

import org.opencv.core.Mat

/**
 * Common contract for anything that can locate a tracked target in a
 * grayscale camera frame and report its real-world size — implemented by
 * both [ArucoDetector] (printed fiducial markers) and [ImageTargetDetector]
 * (real batik motif photos). [ArController] depends only on this interface,
 * so it doesn't care which detection strategy is actually wired in.
 */
interface MotifDetector {
    fun detect(grayscaleFrame: Mat): List<MarkerResult>

    /** (width, height) in meters of the physical target with this id, for [PoseEstimator]. */
    fun physicalSizeMeters(id: Int): Pair<Double, Double>
}
