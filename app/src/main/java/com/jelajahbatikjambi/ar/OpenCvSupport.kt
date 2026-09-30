package com.jelajahbatikjambi.ar

import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core

private const val TAG = "OPENCV"

/**
 * Loads the OpenCV native library once for the process lifetime.
 * [initialize] is called from [com.jelajahbatikjambi.JelajahBatikJambiApp] on app start;
 * [isAvailable] is safe to read from any thread afterwards.
 */
object OpenCvSupport {

    @Volatile
    var isAvailable: Boolean = false
        private set

    fun initialize(): Boolean {
        if (!isAvailable) {
            isAvailable = OpenCVLoader.initLocal()
            if (isAvailable) {
                Log.i(TAG, "OpenCV native library loaded: ${Core.VERSION}")
            } else {
                Log.e(TAG, "OpenCV native library failed to load")
            }
        }
        return isAvailable
    }
}
