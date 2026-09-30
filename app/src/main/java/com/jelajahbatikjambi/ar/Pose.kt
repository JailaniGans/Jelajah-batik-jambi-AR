package com.jelajahbatikjambi.ar

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Minimal math value types for the pose pipeline. Kept local rather than
 * pulling in Filament's math classes, so the AR engine layer stays free of
 * any renderer dependency until Phase 5 consumes [Pose] to drive Filament's
 * transform system.
 */
data class Vector3(val x: Float, val y: Float, val z: Float) {
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
    }
}

/** Componentwise sum — used to add the user's drag-and-drop offset (§ user request) on top of the tracked position. */
operator fun Vector3.plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)

data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

        /** [axis] must already be a unit vector; [angleRadians] is right-handed about it. */
        fun fromAxisAngle(axis: Vector3, angleRadians: Float): Quaternion {
            val half = angleRadians / 2f
            val s = sin(half)
            return Quaternion(axis.x * s, axis.y * s, axis.z * s, cos(half))
        }
    }
}

/**
 * Hamilton product — composes two rotations. Used to layer manual user
 * rotation (§27, drag-to-rotate) on top of the tracked marker pose without
 * needing Filament's math types in the `ar` package.
 */
operator fun Quaternion.times(other: Quaternion): Quaternion = Quaternion(
    x = w * other.x + x * other.w + y * other.z - z * other.y,
    y = w * other.y - x * other.z + y * other.w + z * other.x,
    z = w * other.z + x * other.y - y * other.x + z * other.w,
    w = w * other.w - x * other.x - y * other.y - z * other.z
)

/** Guards against drift after many small [times] compositions in a row (e.g. a long drag gesture). */
fun Quaternion.normalized(): Quaternion {
    val length = sqrt(x * x + y * y + z * z + w * w)
    return if (length == 0f) Quaternion.IDENTITY else Quaternion(x / length, y / length, z / length, w / length)
}

/** Smoothed, render-space pose of a tracked marker. See [CoordinateConverter]. */
data class Pose(val position: Vector3, val rotation: Quaternion)

/**
 * Raw solvePnP output, still in OpenCV camera space (meters, see [PoseEstimator]).
 * Not a data class: the array fields would give reference-equality semantics
 * for equals/hashCode, and this value is never compared or hashed.
 */
class RawPose(val rotationVector: DoubleArray, val translationVector: DoubleArray)
