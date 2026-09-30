package com.jelajahbatikjambi.ar

import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Smooths raw per-frame poses so the rendered 3D object doesn't jitter.
 * Position uses linear interpolation, rotation uses quaternion slerp, both
 * moving toward the latest raw pose by [smoothingFactor] on each update —
 * the smoothed pose eases toward the marker rather than snapping to it.
 *
 * Not thread-safe: confine an instance to one thread (the CameraX analysis
 * executor, via [ArController]), and call [reset] when tracking switches to
 * a different marker so the two markers' poses are never interpolated together.
 */
class PoseSmoother(private val smoothingFactor: Float = 0.35f) {

    private var smoothed: Pose? = null

    fun reset() {
        smoothed = null
    }

    fun smooth(rawPose: Pose): Pose {
        val previous = smoothed
        val next = if (previous == null) {
            rawPose
        } else {
            Pose(
                position = lerp(previous.position, rawPose.position, smoothingFactor),
                rotation = slerp(previous.rotation, rawPose.rotation, smoothingFactor)
            )
        }
        smoothed = next
        return next
    }

    private fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 = Vector3(
        x = a.x + (b.x - a.x) * t,
        y = a.y + (b.y - a.y) * t,
        z = a.z + (b.z - a.z) * t
    )

    private fun slerp(a: Quaternion, b: Quaternion, t: Float): Quaternion {
        var bx = b.x
        var by = b.y
        var bz = b.z
        var bw = b.w
        var cosHalfTheta = a.x * bx + a.y * by + a.z * bz + a.w * bw

        // Take the shorter path around the hypersphere (q and -q represent
        // the same rotation, but interpolating the "long way" looks wrong).
        if (cosHalfTheta < 0f) {
            bx = -bx; by = -by; bz = -bz; bw = -bw
            cosHalfTheta = -cosHalfTheta
        }

        if (cosHalfTheta > 0.9995f) {
            // Nearly identical rotations: sin(halfTheta) below is ~0, so the
            // slerp formula would divide by ~0. Linear interpolation here is
            // visually indistinguishable at this angle.
            return normalize(
                Quaternion(
                    x = a.x + (bx - a.x) * t,
                    y = a.y + (by - a.y) * t,
                    z = a.z + (bz - a.z) * t,
                    w = a.w + (bw - a.w) * t
                )
            )
        }

        val halfTheta = acos(cosHalfTheta.coerceIn(-1f, 1f))
        val sinHalfTheta = sin(halfTheta)
        val ratioA = sin((1 - t) * halfTheta) / sinHalfTheta
        val ratioB = sin(t * halfTheta) / sinHalfTheta

        return Quaternion(
            x = a.x * ratioA + bx * ratioB,
            y = a.y * ratioA + by * ratioB,
            z = a.z * ratioA + bz * ratioB,
            w = a.w * ratioA + bw * ratioB
        )
    }

    private fun normalize(q: Quaternion): Quaternion {
        val len = sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        return if (len == 0f) Quaternion.IDENTITY else Quaternion(q.x / len, q.y / len, q.z / len, q.w / len)
    }
}
