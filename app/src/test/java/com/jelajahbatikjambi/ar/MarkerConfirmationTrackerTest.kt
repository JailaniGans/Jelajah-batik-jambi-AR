package com.jelajahbatikjambi.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerConfirmationTrackerTest {

    @Test
    fun `a marker seen fewer times than the threshold stays a candidate`() {
        val tracker = MarkerConfirmationTracker(detectionConfirmationFrames = 3, lostConfirmationFrames = 5)

        val first = tracker.onMarkerSeen(7)
        val second = tracker.onMarkerSeen(7)

        assertTrue(first is MarkerConfirmationTracker.Decision.Candidate)
        assertTrue(second is MarkerConfirmationTracker.Decision.Candidate)
        assertNull(tracker.trackedMarkerId)
    }

    @Test
    fun `a marker seen for the threshold frame count gets confirmed`() {
        val tracker = MarkerConfirmationTracker(detectionConfirmationFrames = 3, lostConfirmationFrames = 5)

        tracker.onMarkerSeen(7)
        tracker.onMarkerSeen(7)
        val third = tracker.onMarkerSeen(7)

        assertEquals(MarkerConfirmationTracker.Decision.Confirmed(7), third)
        assertEquals(7, tracker.trackedMarkerId)
    }

    @Test
    fun `switching candidate id resets the confirmation count`() {
        val tracker = MarkerConfirmationTracker(detectionConfirmationFrames = 3, lostConfirmationFrames = 5)

        tracker.onMarkerSeen(1)
        tracker.onMarkerSeen(1)
        val afterSwitch = tracker.onMarkerSeen(2) // different id — restarts the count at 1

        assertEquals(MarkerConfirmationTracker.Decision.Candidate(2), afterSwitch)
        assertNull(tracker.trackedMarkerId)
    }

    @Test
    fun `missing a tracked marker briefly reports Lost but keeps it tracked`() {
        val tracker = MarkerConfirmationTracker(detectionConfirmationFrames = 1, lostConfirmationFrames = 5)
        tracker.onMarkerSeen(9) // confirmed immediately (threshold = 1)

        val decision = tracker.onMarkerMissing()

        assertEquals(MarkerConfirmationTracker.Decision.Lost(9), decision)
        assertEquals(9, tracker.trackedMarkerId)
    }

    @Test
    fun `a tracked marker reappearing within the grace period resumes tracking`() {
        val tracker = MarkerConfirmationTracker(detectionConfirmationFrames = 1, lostConfirmationFrames = 5)
        tracker.onMarkerSeen(9)
        tracker.onMarkerMissing()
        tracker.onMarkerMissing()

        val decision = tracker.onMarkerSeen(9)

        assertEquals(MarkerConfirmationTracker.Decision.Confirmed(9), decision)
        assertEquals(9, tracker.trackedMarkerId)
    }

    @Test
    fun `missing a tracked marker beyond the grace period drops it back to Searching`() {
        val tracker = MarkerConfirmationTracker(detectionConfirmationFrames = 1, lostConfirmationFrames = 2)
        tracker.onMarkerSeen(9)

        tracker.onMarkerMissing() // 1: Lost
        tracker.onMarkerMissing() // 2: Lost
        val decision = tracker.onMarkerMissing() // 3: exceeds grace period

        assertEquals(MarkerConfirmationTracker.Decision.Searching, decision)
        assertNull(tracker.trackedMarkerId)
    }

    @Test
    fun `never having seen a marker reports Searching on missing`() {
        val tracker = MarkerConfirmationTracker()

        val decision = tracker.onMarkerMissing()

        assertEquals(MarkerConfirmationTracker.Decision.Searching, decision)
    }
}
