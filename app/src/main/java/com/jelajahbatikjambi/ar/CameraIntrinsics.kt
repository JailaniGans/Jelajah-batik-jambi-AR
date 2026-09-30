package com.jelajahbatikjambi.ar

import android.util.Size
import android.util.SizeF
import org.opencv.core.CvType
import org.opencv.core.Mat

/**
 * Pinhole camera intrinsics used to build the 3x3 camera matrix that
 * [PoseEstimator] passes to OpenCV's solvePnP:
 *
 *     [ fx  0  cx ]
 *     [ 0  fy  cy ]
 *     [ 0   0   1 ]
 *
 * fx/fy/cx/cy are in pixel units and only valid for the image resolution
 * they were computed for — never share one instance across resolutions.
 */
data class CameraIntrinsics(
    val fx: Double,
    val fy: Double,
    val cx: Double,
    val cy: Double
) {
    fun toCameraMatrix(): Mat {
        val mat = Mat(3, 3, CvType.CV_64F)
        mat.put(0, 0, *doubleArrayOf(fx, 0.0, cx, 0.0, fy, cy, 0.0, 0.0, 1.0))
        return mat
    }

    companion object {

        /**
         * Derives intrinsics from the device's actual lens/sensor characteristics
         * (Camera2 focal length + physical sensor size), scaled to the analysis
         * resolution actually used. Falls back to [estimate] when a device
         * doesn't report these fields, which is allowed by the Camera2 API.
         *
         * This assumes the analysis frame is a uniform scale of the full pixel
         * array (no asymmetric crop) — true for CameraX's default output on
         * the vast majority of devices, and good enough for a stable-looking
         * pose without a dedicated calibration flow.
         */
        fun from(
            focalLengthMm: Float?,
            sensorPhysicalSize: SizeF?,
            sensorPixelArraySize: Size?,
            imageWidthPx: Int,
            imageHeightPx: Int
        ): CameraIntrinsics {
            if (focalLengthMm == null || sensorPhysicalSize == null || sensorPixelArraySize == null ||
                sensorPhysicalSize.width <= 0f || sensorPhysicalSize.height <= 0f
            ) {
                return estimate(imageWidthPx, imageHeightPx)
            }

            val fxFullSensor = focalLengthMm * sensorPixelArraySize.width / sensorPhysicalSize.width
            val fyFullSensor = focalLengthMm * sensorPixelArraySize.height / sensorPhysicalSize.height

            val scaleX = imageWidthPx / sensorPixelArraySize.width.toDouble()
            val scaleY = imageHeightPx / sensorPixelArraySize.height.toDouble()

            return CameraIntrinsics(
                fx = fxFullSensor * scaleX,
                fy = fyFullSensor * scaleY,
                cx = imageWidthPx / 2.0,
                cy = imageHeightPx / 2.0
            )
        }

        /**
         * Rough estimate used when real lens/sensor data isn't available.
         * Assumes focal length in pixels roughly equals image width, which
         * corresponds to a ~53° horizontal field of view — typical of phone
         * rear cameras.
         */
        fun estimate(imageWidthPx: Int, imageHeightPx: Int): CameraIntrinsics {
            val focalPx = imageWidthPx.toDouble()
            return CameraIntrinsics(
                fx = focalPx,
                fy = focalPx,
                cx = imageWidthPx / 2.0,
                cy = imageHeightPx / 2.0
            )
        }
    }
}
