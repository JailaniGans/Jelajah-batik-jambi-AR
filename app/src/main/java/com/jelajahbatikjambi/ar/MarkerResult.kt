package com.jelajahbatikjambi.ar

import org.opencv.core.Point

/**
 * One detected ArUco marker for a single frame.
 * [corners] are the four marker corners in image pixel space, clockwise
 * starting at the top-left, as returned by OpenCV's ArucoDetector.
 */
data class MarkerResult(
    val id: Int,
    val corners: List<Point>
)
