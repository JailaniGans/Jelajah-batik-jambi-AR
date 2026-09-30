package com.jelajahbatikjambi.ar

import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint

/**
 * One reference batik motif image with its precomputed ORB features,
 * used by [ImageTargetDetector] to recognize that motif in a live camera frame.
 * [widthMeters]/[heightMeters] are an assumed real-world size (see
 * [ImageTargetDetector.ASSUMED_LONG_SIDE_METERS]) — there's no way to know the
 * actual physical size of whatever printed/displayed copy the user holds up,
 * so distance/scale in the resulting pose is an estimate, not a measurement.
 */
class ImageTarget(
    val id: Int,
    val name: String,
    val widthPx: Int,
    val heightPx: Int,
    val widthMeters: Double,
    val heightMeters: Double,
    val keypoints: MatOfKeyPoint,
    val descriptors: Mat
)
