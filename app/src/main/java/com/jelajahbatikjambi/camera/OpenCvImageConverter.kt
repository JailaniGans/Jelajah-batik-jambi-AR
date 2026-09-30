package com.jelajahbatikjambi.camera

import androidx.camera.core.ImageProxy
import org.opencv.core.CvType
import org.opencv.core.Mat

/**
 * Converts CameraX YUV_420_888 frames into OpenCV Mats.
 * Only the Y (luminance) plane is copied — ArUco detection runs on grayscale,
 * so a full YUV→RGB conversion per frame is unnecessary work.
 * The Y plane's pixel stride is always 1 on YUV_420_888, but its row stride
 * can exceed the image width (padding), so rows are copied one at a time.
 */
object OpenCvImageConverter {

    fun toGrayscaleMat(image: ImageProxy): Mat {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val width = image.width
        val height = image.height

        val mat = Mat(height, width, CvType.CV_8UC1)
        val row = ByteArray(width)
        for (y in 0 until height) {
            buffer.position(y * rowStride)
            buffer.get(row, 0, width)
            mat.put(y, 0, row)
        }
        return mat
    }
}
