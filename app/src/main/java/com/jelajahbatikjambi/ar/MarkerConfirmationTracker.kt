package com.jelajahbatikjambi.ar

/**
 * Pure debounce state machine for marker detection/loss (§36-37), extracted
 * out of [ArController] so it can be unit tested without any OpenCV or
 * Android dependency. Feed it one [onMarkerSeen] or [onMarkerMissing] call
 * per processed frame; it reports what [ArController] should do via [Decision].
 *
 * Single-marker-at-a-time: while a marker is tracked, seeing a *different*
 * marker counts as a missing frame for the tracked one (call [onMarkerMissing]),
 * rather than switching immediately — this is what [ArController] does.
 */
class MarkerConfirmationTracker(
    private val detectionConfirmationFrames: Int = 3,
    private val lostConfirmationFrames: Int = 5
) {

    sealed interface Decision {
        /** Nothing tracked and no candidate pending. */
        data object Searching : Decision

        /** Seen, but not yet confirmed across enough consecutive frames. */
        data class Candidate(val markerId: Int) : Decision

        /** Confirmed this call (or already tracked and still visible). */
        data class Confirmed(val markerId: Int) : Decision

        /** Tracked marker missing, within the grace period — last pose should be kept. */
        data class Lost(val markerId: Int) : Decision
    }

    var trackedMarkerId: Int? = null
        private set

    private var missingFrameCount = 0
    private var pendingMarkerId: Int? = null
    private var pendingMarkerFrameCount = 0

    fun onMarkerSeen(markerId: Int): Decision {
        val currentlyTracked = trackedMarkerId
        if (currentlyTracked != null) {
            if (markerId == currentlyTracked) {
                missingFrameCount = 0
                return Decision.Confirmed(currentlyTracked)
            }
            return onMarkerMissing()
        }

        if (pendingMarkerId == markerId) {
            pendingMarkerFrameCount++
        } else {
            pendingMarkerId = markerId
            pendingMarkerFrameCount = 1
        }

        if (pendingMarkerFrameCount >= detectionConfirmationFrames) {
            trackedMarkerId = markerId
            missingFrameCount = 0
            pendingMarkerId = null
            pendingMarkerFrameCount = 0
            return Decision.Confirmed(markerId)
        }
        return Decision.Candidate(markerId)
    }

    fun onMarkerMissing(): Decision {
        pendingMarkerId = null
        pendingMarkerFrameCount = 0

        val trackedId = trackedMarkerId ?: return Decision.Searching

        missingFrameCount++
        if (missingFrameCount <= lostConfirmationFrames) {
            return Decision.Lost(trackedId)
        }

        trackedMarkerId = null
        missingFrameCount = 0
        return Decision.Searching
    }
}
