package com.jelajahbatikjambi.ar

/**
 * Snapshot of AR pipeline performance for the debug overlay (§50).
 * Only ever read by UI gated behind `BuildConfig.DEBUG` — computing it is
 * cheap enough to leave enabled in release builds too.
 */
data class DebugMetrics(
    val processingFps: Int = 0,
    val detectionMs: Long = 0,
    val poseMs: Long = 0
)
