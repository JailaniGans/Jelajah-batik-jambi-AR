package com.jelajahbatikjambi.ar

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseSmootherTest {

    private val identity = Pose(Vector3.ZERO, Quaternion.IDENTITY)

    @Test
    fun `first smooth call returns the raw pose unchanged`() {
        val smoother = PoseSmoother()
        val raw = Pose(Vector3(1f, 2f, 3f), Quaternion.IDENTITY)

        val result = smoother.smooth(raw)

        assertEquals(raw, result)
    }

    @Test
    fun `second smooth call moves partway from the previous pose toward the new one`() {
        val smoother = PoseSmoother(smoothingFactor = 0.5f)
        smoother.smooth(Pose(Vector3(0f, 0f, 0f), Quaternion.IDENTITY))

        val result = smoother.smooth(Pose(Vector3(10f, 0f, 0f), Quaternion.IDENTITY))

        // t=0.5 halfway between 0 and 10.
        assertEquals(5f, result.position.x, 0.001f)
    }

    @Test
    fun `reset clears state so the next smooth call snaps to raw again`() {
        val smoother = PoseSmoother(smoothingFactor = 0.5f)
        smoother.smooth(Pose(Vector3(0f, 0f, 0f), Quaternion.IDENTITY))
        smoother.reset()

        val raw = Pose(Vector3(10f, 0f, 0f), Quaternion.IDENTITY)
        val result = smoother.smooth(raw)

        assertEquals(raw, result)
    }

    @Test
    fun `smoothed rotation stays a normalized quaternion`() {
        val smoother = PoseSmoother(smoothingFactor = 0.5f)
        smoother.smooth(identity)

        // 90 degrees around Y: (0, sin(45deg), 0, cos(45deg)).
        val rotated = Pose(Vector3.ZERO, Quaternion(0f, 0.7071068f, 0f, 0.7071068f))
        val result = smoother.smooth(rotated)

        val length = sqrt(
            result.rotation.x * result.rotation.x +
                result.rotation.y * result.rotation.y +
                result.rotation.z * result.rotation.z +
                result.rotation.w * result.rotation.w
        )
        assertTrue("expected a unit quaternion, got length $length", kotlin.math.abs(length - 1f) < 0.001f)
    }
}
