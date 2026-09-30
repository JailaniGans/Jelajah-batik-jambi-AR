package com.jelajahbatikjambi.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Bridges CameraX frames to the AR pipeline.
 * Runs on the analysis background thread (see [CameraController]) and always
 * closes the frame, even if [onFrame] throws, so CameraX can deliver the next one.
 * [onFrame] is a no-op by default until the OpenCV/ArUco pipeline is wired in.
 */
class CameraAnalyzer(
    private val onFrame: (ImageProxy) -> Unit = {}
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            onFrame(image)
        } finally {
            image.close()
        }
    }
}
