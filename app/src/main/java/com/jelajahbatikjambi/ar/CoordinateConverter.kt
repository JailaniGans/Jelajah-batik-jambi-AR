package com.jelajahbatikjambi.ar

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.geometry.Geometry
import kotlin.math.sqrt

/**
 * Converts a [RawPose] — OpenCV's solvePnP output in camera space — into
 * [Pose], the engine's render-space representation consumed by [PoseSmoother]
 * and, from Phase 5 on, Filament's transform system.
 *
 * Coordinate systems:
 * - **OpenCV camera space (input)**: right-handed, X right, Y down, Z forward
 *   (into the scene, away from the camera). Units: meters, matching the
 *   marker size passed to [PoseEstimator].
 * - **Render/world space (output)**: right-handed, X right, Y up, Z backward
 *   (out of the screen, toward the viewer) — the OpenGL convention Filament
 *   also uses. Units: meters, unchanged (1:1 scale with camera space).
 *
 * The two conventions differ only by flipping the Y and Z axes. Position is
 * flipped directly; rotation is flipped by conjugating the rotation matrix
 * with the same flip (R_gl = F · R_cv · F, F = diag(1, -1, -1)), which is
 * the correct way to change basis for a rotation, not just negate an angle.
 */
object CoordinateConverter {

    fun toPose(rawPose: RawPose): Pose {
        val rotationMatrixCv = rodriguesToMatrix(rawPose.rotationVector)
        val rotationMatrixGl = conjugateWithAxisFlip(rotationMatrixCv)

        val position = Vector3(
            x = rawPose.translationVector[0].toFloat(),
            y = -rawPose.translationVector[1].toFloat(),
            z = -rawPose.translationVector[2].toFloat()
        )
        val rotation = matrixToQuaternion(rotationMatrixGl)

        return Pose(position = position, rotation = rotation)
    }

    private fun rodriguesToMatrix(rotationVector: DoubleArray): DoubleArray {
        val rvecMat = Mat(3, 1, CvType.CV_64F)
        rvecMat.put(0, 0, *rotationVector)
        val rotationMat = Mat()
        Geometry.Rodrigues(rvecMat, rotationMat)

        val out = DoubleArray(9)
        rotationMat.get(0, 0, out)

        rvecMat.release()
        rotationMat.release()
        return out
    }

    /** Row-major 3x3 in, row-major 3x3 out: R_gl = F * R_cv * F, F = diag(1, -1, -1). */
    private fun conjugateWithAxisFlip(r: DoubleArray): DoubleArray {
        val result = DoubleArray(9)
        for (row in 0 until 3) {
            val signRow = if (row == 0) 1.0 else -1.0
            for (col in 0 until 3) {
                val signCol = if (col == 0) 1.0 else -1.0
                result[row * 3 + col] = r[row * 3 + col] * signRow * signCol
            }
        }
        return result
    }

    /** Standard trace-based rotation-matrix-to-quaternion conversion (Shepperd's method). */
    private fun matrixToQuaternion(m: DoubleArray): Quaternion {
        val m00 = m[0]; val m01 = m[1]; val m02 = m[2]
        val m10 = m[3]; val m11 = m[4]; val m12 = m[5]
        val m20 = m[6]; val m21 = m[7]; val m22 = m[8]

        val trace = m00 + m11 + m22
        return when {
            trace > 0 -> {
                val s = sqrt(trace + 1.0) * 2.0
                Quaternion(
                    x = ((m21 - m12) / s).toFloat(),
                    y = ((m02 - m20) / s).toFloat(),
                    z = ((m10 - m01) / s).toFloat(),
                    w = (0.25 * s).toFloat()
                )
            }

            m00 > m11 && m00 > m22 -> {
                val s = sqrt(1.0 + m00 - m11 - m22) * 2.0
                Quaternion(
                    x = (0.25 * s).toFloat(),
                    y = ((m01 + m10) / s).toFloat(),
                    z = ((m02 + m20) / s).toFloat(),
                    w = ((m21 - m12) / s).toFloat()
                )
            }

            m11 > m22 -> {
                val s = sqrt(1.0 + m11 - m00 - m22) * 2.0
                Quaternion(
                    x = ((m01 + m10) / s).toFloat(),
                    y = (0.25 * s).toFloat(),
                    z = ((m12 + m21) / s).toFloat(),
                    w = ((m02 - m20) / s).toFloat()
                )
            }

            else -> {
                val s = sqrt(1.0 + m22 - m00 - m11) * 2.0
                Quaternion(
                    x = ((m02 + m20) / s).toFloat(),
                    y = ((m12 + m21) / s).toFloat(),
                    z = (0.25 * s).toFloat(),
                    w = ((m10 - m01) / s).toFloat()
                )
            }
        }
    }
}
