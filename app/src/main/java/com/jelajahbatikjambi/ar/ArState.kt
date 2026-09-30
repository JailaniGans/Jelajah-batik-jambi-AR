package com.jelajahbatikjambi.ar

/**
 * Lifecycle of the AR pipeline as seen by the UI.
 * [MarkerDetected] means a marker was seen but hasn't been confirmed across
 * enough consecutive frames yet (or a confirmed marker's pose isn't ready);
 * [Tracking] means it's confirmed and a smoothed [Pose] is available from
 * [ArController.pose]. [MarkerLost] is the grace period after a tracked
 * marker briefly drops out — the last pose is kept — before falling back to
 * [Searching] if it doesn't reappear in time.
 */
sealed interface ArState {

    data object Initializing : ArState

    data object Searching : ArState

    data class MarkerDetected(
        val markerId: Int
    ) : ArState

    data class Tracking(
        val markerId: Int
    ) : ArState

    data class MarkerLost(
        val markerId: Int
    ) : ArState

    data class Error(
        val message: String
    ) : ArState
}
