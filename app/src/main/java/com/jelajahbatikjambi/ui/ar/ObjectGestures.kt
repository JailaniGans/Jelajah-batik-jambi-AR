package com.jelajahbatikjambi.ui.ar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange

/**
 * Custom multi-touch handler for manipulating the tracked 3D object: one
 * finger rotates it (§27, the original drag-to-rotate interaction), while
 * two or more fingers instead pan (drag-and-drop — § user request) and pinch
 * (zoom in/out — § user request). Neither [androidx.compose.foundation.gestures.detectDragGestures]
 * nor [androidx.compose.foundation.gestures.detectTransformGestures] lets a
 * single call site switch behavior by pointer count like this, so this reads
 * raw pointer events directly instead.
 *
 * Coexists with a separate `detectTapGestures(onDoubleTap = ...)` in another
 * `pointerInput` block on the same modifier for the existing reset gesture —
 * a tap involves no position change, so this detector never consumes it.
 */
suspend fun PointerInputScope.detectObjectManipulationGestures(
    onRotate: (dx: Float, dy: Float) -> Unit,
    onPan: (dx: Float, dy: Float) -> Unit,
    onZoom: (scaleFactor: Float) -> Unit
) {
    awaitEachGesture {
        var previousCentroid: Offset? = null
        var previousSpan: Float? = null

        while (true) {
            val event = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }
            if (pointers.isEmpty()) break

            if (pointers.size == 1) {
                val change = pointers[0]
                val delta = change.positionChange()
                if (delta != Offset.Zero) {
                    onRotate(delta.x, delta.y)
                    change.consume()
                }
                previousCentroid = null
                previousSpan = null
            } else {
                val positions = pointers.map { it.position }
                val centroid = centroidOf(positions)
                val span = averagePairwiseDistance(positions)

                val prevCentroid = previousCentroid
                if (prevCentroid != null) {
                    val panDelta = centroid - prevCentroid
                    if (panDelta != Offset.Zero) onPan(panDelta.x, panDelta.y)
                }
                val prevSpan = previousSpan
                if (prevSpan != null && prevSpan > 0f && span > 0f) {
                    onZoom(span / prevSpan)
                }

                previousCentroid = centroid
                previousSpan = span
                pointers.forEach { it.consume() }
            }
        }
    }
}

private fun centroidOf(points: List<Offset>): Offset {
    var sumX = 0f
    var sumY = 0f
    for (point in points) {
        sumX += point.x
        sumY += point.y
    }
    return Offset(sumX / points.size, sumY / points.size)
}

private fun averagePairwiseDistance(points: List<Offset>): Float {
    if (points.size < 2) return 0f
    var total = 0f
    var count = 0
    for (i in points.indices) {
        for (j in i + 1 until points.size) {
            total += (points[i] - points[j]).getDistance()
            count++
        }
    }
    return if (count == 0) 0f else total / count
}
