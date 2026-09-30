package com.jelajahbatikjambi.ui.ar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jelajahbatikjambi.ar.ArState
import com.jelajahbatikjambi.ar.DetectedRegion
import com.jelajahbatikjambi.ui.theme.BatikGold
import com.jelajahbatikjambi.ui.theme.BatikGreen

/**
 * Scan-area indicator (§30). Before anything is detected, shows a
 * gently-breathing corner-bracket hint centered on screen — a subtle pulse so
 * the idle scanner still reads as "alive" and searching, not stuck. Once
 * [region] is non-null — the actual matched quad, in frame-normalized [0,1]
 * coordinates from [com.jelajahbatikjambi.ar.ArController.detectedRegion] —
 * draws a live "lock-on" outline around that region instead, like a barcode
 * scanner: gold and still-breathing while a match is being confirmed
 * ([ArState.MarkerDetected]), settling into a solid green outline with corner
 * accents once [ArState.Tracking] is reached.
 *
 * The coordinate mapping is an approximation: it assumes the camera preview
 * fills this composable uniformly, which only holds exactly if the analysis
 * resolution's aspect ratio matches the preview's. Good enough for a visual
 * confirmation overlay, not intended as a precise measurement.
 */
@Composable
fun MarkerReticle(state: ArState, region: DetectedRegion?, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 3.dp.toPx() }
    val bracketStrokeWidthPx = with(density) { 4.dp.toPx() }
    val bracketBoxPx = with(density) { 220.dp.toPx() }
    val cornerDotRadiusPx = with(density) { 4.dp.toPx() }

    val isConfirmed = state is ArState.Tracking
    val isBreathing = !isConfirmed

    val infiniteTransition = rememberInfiniteTransition(label = "reticleBreathe")
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticleBreatheAlpha"
    )
    val alpha = if (isBreathing) breatheAlpha else 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        if (region != null && region.corners.size == 4) {
            val color = if (isConfirmed) BatikGreen else BatikGold
            drawDetectedRegion(region, strokeWidthPx, cornerDotRadiusPx, color, alpha)
        } else if (state is ArState.Searching || state is ArState.Initializing) {
            drawSearchBracket(bracketBoxPx, bracketStrokeWidthPx, alpha)
        }
    }
}

private fun DrawScope.drawDetectedRegion(
    region: DetectedRegion,
    strokeWidthPx: Float,
    cornerDotRadiusPx: Float,
    color: Color,
    alpha: Float
) {
    val points = region.corners.map { (nx, ny) -> Offset(nx * size.width, ny * size.height) }
    for (i in points.indices) {
        val next = points[(i + 1) % points.size]
        drawLine(color.copy(alpha = alpha), points[i], next, strokeWidthPx, cap = StrokeCap.Round)
    }
    for (point in points) {
        drawCircle(color.copy(alpha = alpha), radius = cornerDotRadiusPx, center = point)
    }
}

private fun DrawScope.drawSearchBracket(boxSize: Float, strokeWidth: Float, alpha: Float) {
    val left = (size.width - boxSize) / 2f
    val top = (size.height - boxSize) / 2f
    val bracket = boxSize * 0.18f
    val color = Color.White.copy(alpha = 0.85f * alpha)

    fun corner(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        drawLine(color, Offset(x0, y0), Offset(x1, y1), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth, cap = StrokeCap.Round)
    }

    corner(left, top + bracket, left, top, left + bracket, top)
    corner(left + boxSize - bracket, top, left + boxSize, top, left + boxSize, top + bracket)
    corner(left, top + boxSize - bracket, left, top + boxSize, left + bracket, top + boxSize)
    corner(
        left + boxSize - bracket, top + boxSize,
        left + boxSize, top + boxSize,
        left + boxSize, top + boxSize - bracket
    )
}
