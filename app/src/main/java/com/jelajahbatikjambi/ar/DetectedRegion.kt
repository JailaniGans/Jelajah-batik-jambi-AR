package com.jelajahbatikjambi.ar

/**
 * The most recently detected target's 4 corners, normalized to [0,1] within
 * the analysis frame (origin top-left, same convention as image/UV space).
 * Used to draw a live bounding box directly over the matched region on
 * screen — see [com.jelajahbatikjambi.ui.ar.MarkerReticle] — rather than a
 * fixed placeholder box.
 */
data class DetectedRegion(val corners: List<Pair<Float, Float>>)
