package com.jelajahbatikjambi.render

import com.google.android.filament.Engine

/**
 * Applies a position + quaternion rotation + uniform scale to an entity's
 * TransformManager component, building the column-major 4x4 matrix Filament
 * expects (matching the OpenGL/GLM convention).
 *
 * Called every frame from [RenderLifecycle.updatePose] with
 * [com.jelajahbatikjambi.ar.ArController]'s live smoothed pose.
 */
class TransformController(private val engine: Engine) {

    fun setTransform(
        entity: Int,
        positionX: Float, positionY: Float, positionZ: Float,
        rotationX: Float, rotationY: Float, rotationZ: Float, rotationW: Float,
        scale: Float = 1f
    ) {
        val transformManager = engine.transformManager
        val instance = transformManager.getInstance(entity)
        if (instance == 0) return

        transformManager.setTransform(
            instance,
            buildMatrix(positionX, positionY, positionZ, rotationX, rotationY, rotationZ, rotationW, scale)
        )
    }

    /** Standard quaternion-to-rotation-matrix expansion, scaled and translated. */
    private fun buildMatrix(
        px: Float, py: Float, pz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float,
        scale: Float
    ): FloatArray {
        val xx = qx * qx; val yy = qy * qy; val zz = qz * qz
        val xy = qx * qy; val xz = qx * qz; val yz = qy * qz
        val wx = qw * qx; val wy = qw * qy; val wz = qw * qz

        val r00 = 1f - 2f * (yy + zz)
        val r01 = 2f * (xy - wz)
        val r02 = 2f * (xz + wy)

        val r10 = 2f * (xy + wz)
        val r11 = 1f - 2f * (xx + zz)
        val r12 = 2f * (yz - wx)

        val r20 = 2f * (xz - wy)
        val r21 = 2f * (yz + wx)
        val r22 = 1f - 2f * (xx + yy)

        // Column-major: [col0, col1, col2, col3], translation in col3.
        return floatArrayOf(
            r00 * scale, r10 * scale, r20 * scale, 0f,
            r01 * scale, r11 * scale, r21 * scale, 0f,
            r02 * scale, r12 * scale, r22 * scale, 0f,
            px, py, pz, 1f
        )
    }
}
