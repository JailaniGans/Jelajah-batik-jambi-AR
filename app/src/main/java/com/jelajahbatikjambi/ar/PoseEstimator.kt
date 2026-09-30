package com.jelajahbatikjambi.ar

import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point3
import org.opencv.geometry.Geometry

/**
 * Solves for a single tracked target's pose relative to the camera using
 * OpenCV's solvePnP against its known real-world rectangle size — used for
 * both square ArUco markers ([Geometry.SOLVEPNP_IPPE_SQUARE]) and general
 * rectangular targets like a batik motif photo ([Geometry.SOLVEPNP_IPPE]).
 *
 * Output stays in OpenCV's camera-space rotation/translation vectors —
 * conversion into the engine's render-space [Pose] happens in [CoordinateConverter].
 */
class PoseEstimator {

    // No per-device lens calibration exists yet, so distortion is assumed
    // negligible (typical for modern phone cameras after HAL correction).
    private val zeroDistortion = MatOfDouble(0.0, 0.0, 0.0, 0.0)

    fun estimate(
        marker: MarkerResult,
        intrinsics: CameraIntrinsics,
        widthMeters: Double = DEFAULT_MARKER_SIZE_METERS,
        heightMeters: Double = widthMeters
    ): RawPose? {
        if (marker.corners.size != 4) return null

        // Corners in their own coordinate frame (Z=0 plane, center at origin,
        // Y up), ordered clockwise from top-left to match the corner order
        // both ArucoDetector and ImageTargetDetector return.
        val objectPoints = MatOfPoint3f(
            Point3(-widthMeters / 2.0, heightMeters / 2.0, 0.0),
            Point3(widthMeters / 2.0, heightMeters / 2.0, 0.0),
            Point3(widthMeters / 2.0, -heightMeters / 2.0, 0.0),
            Point3(-widthMeters / 2.0, -heightMeters / 2.0, 0.0)
        )
        val imagePoints = MatOfPoint2f(*marker.corners.toTypedArray())
        val cameraMatrix = intrinsics.toCameraMatrix()
        val rvec = Mat()
        val tvec = Mat()
        // IPPE_SQUARE is only valid for a true square object; a non-square
        // rectangle (most photos) needs the general planar IPPE variant.
        val method = if (widthMeters == heightMeters) Geometry.SOLVEPNP_IPPE_SQUARE else Geometry.SOLVEPNP_IPPE

        return try {
            val solved = Geometry.solvePnP(
                objectPoints,
                imagePoints,
                cameraMatrix,
                zeroDistortion,
                rvec,
                tvec,
                false,
                method
            )
            if (!solved) null else RawPose(
                rotationVector = rvec.toDoubleArray3(),
                translationVector = tvec.toDoubleArray3()
            )
        } finally {
            objectPoints.release()
            imagePoints.release()
            cameraMatrix.release()
            rvec.release()
            tvec.release()
        }
    }

    companion object {
        const val DEFAULT_MARKER_SIZE_METERS = 0.05
    }
}

private fun Mat.toDoubleArray3(): DoubleArray {
    val out = DoubleArray(3)
    get(0, 0, out)
    return out
}
