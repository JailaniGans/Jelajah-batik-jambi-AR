package com.jelajahbatikjambi.ar

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.features.BFMatcher
import org.opencv.features.DescriptorMatcher
import org.opencv.features.ORB
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc

/**
 * Recognizes a real Batik Jambi motif photo/cloth directly via feature
 * matching + RANSAC homography — not a naive similarity score. Batik motifs
 * are often highly repetitive, so a bare match-count or correlation
 * threshold (an earlier, simpler version of this class used
 * [Imgproc.matchTemplate] that way) produces false positives; requiring a
 * geometrically *consistent* planar homography with enough RANSAC inliers is
 * what actually distinguishes "this really is the target" from "these
 * regions happen to look similar".
 *
 * Uses ORB rather than AKAZE: AKAZE (and KAZE/BRISK) are not present in the
 * `org.opencv:opencv` Android AAR's Java bindings for this OpenCV version —
 * verified directly against the AAR's class list, not assumed from
 * desktop-OpenCV tutorials, which cover the full C++/Python distribution.
 * ORB gives the same binary-descriptor + Hamming-distance matching pipeline
 * those tutorials describe, at a lower per-frame cost than the other
 * available alternative (SIFT's floating-point descriptors) — worth
 * prioritizing since this runs on every live camera frame, not once per image.
 *
 * Pipeline per frame:
 * 1. ORB keypoints/descriptors on the current frame.
 * 2. k-NN match (k=2) against each reference image's descriptors.
 * 3. Lowe's ratio test — keep a match only when the best candidate is
 *    convincingly closer than the second-best, discarding ambiguous ones
 *    (this is where most repetitive-pattern false matches get filtered).
 * 4. RANSAC homography on the surviving matches; require both a minimum
 *    *inlier* count and a minimum inlier *ratio* (inliers / good matches),
 *    not just a minimum match count — geometric consistency, not quantity,
 *    is the actual signal, and the ratio catches the case where a large
 *    absolute inlier count is still mostly outliers.
 * 5. Perspective-transform the reference image's corners through the
 *    homography to get the matched quad in the current frame, returned as a
 *    [MarkerResult] — reusing the same corner contract [ArucoDetector] uses,
 *    so [PoseEstimator] needs no detector-specific branching.
 * 6. Only the best-scoring target (most inliers) is reported, keeping the
 *    existing single-target-at-a-time tracking model.
 */
class ImageTargetDetector(
    private val assetManager: AssetManager,
    referenceImages: List<ReferenceImage>,
    private val maxFeaturesPerFrame: Int = 500,
    private val minGoodMatches: Int = 15,
    private val minInlierRatio: Double = 0.4,
    private val ratioTestThreshold: Double = 0.75,
    private val ransacReprojThresholdPx: Double = 5.0
) : MotifDetector {

    /**
     * [path] is either a bundled `assets/images/` path, or — for a
     * user-uploaded custom motif — an absolute file path in internal
     * storage; a path starting with "/" is treated as the latter (same
     * convention as [com.jelajahbatikjambi.ui.common.AssetImage]).
     */
    data class ReferenceImage(val id: Int, val name: String, val path: String)

    private class Target(
        val id: Int,
        val widthPx: Int,
        val heightPx: Int,
        val widthMeters: Double,
        val heightMeters: Double,
        val keypoints: MatOfKeyPoint,
        val descriptors: Mat
    )

    private val frameOrb = ORB.create(maxFeaturesPerFrame)
    // Reference images are static and loaded once, so a richer feature count
    // here costs nothing at runtime — more candidate keypoints means more
    // chances to match whatever partial/angled view the camera frame has.
    private val referenceOrb = ORB.create(1000)
    private val matcher: DescriptorMatcher = BFMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)

    // @Volatile: addReferenceImages() reassigns this from a coroutine (main
    // thread), while detect() reads it from the CameraX analysis thread.
    @Volatile
    private var targets: List<Target> = referenceImages.map { ref -> loadTarget(ref) }

    /**
     * Appends newly-added custom motifs to the live detection set without
     * rebuilding the detector — used when a motif is added while the AR
     * screen may already be running (§ user request: "tambahkan motif
     * dengan upload .jpg"). Existing targets keep matching uninterrupted.
     */
    fun addReferenceImages(newImages: List<ReferenceImage>) {
        if (newImages.isEmpty()) return
        targets = targets + newImages.map { loadTarget(it) }
    }

    private fun loadTarget(ref: ReferenceImage): Target {
        val bitmap = if (ref.path.startsWith("/")) {
            BitmapFactory.decodeFile(ref.path)
        } else {
            assetManager.open(ref.path).use { BitmapFactory.decodeStream(it) }
        }
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()

        val keypoints = MatOfKeyPoint()
        val descriptors = Mat()
        referenceOrb.detectAndCompute(gray, Mat(), keypoints, descriptors)

        val longSidePx = maxOf(gray.cols(), gray.rows()).toDouble()
        val scale = ASSUMED_LONG_SIDE_METERS / longSidePx

        val target = Target(
            id = ref.id,
            widthPx = gray.cols(),
            heightPx = gray.rows(),
            widthMeters = gray.cols() * scale,
            heightMeters = gray.rows() * scale,
            keypoints = keypoints,
            descriptors = descriptors
        )
        gray.release()
        return target
    }

    override fun detect(grayscaleFrame: Mat): List<MarkerResult> {
        val frameKeypoints = MatOfKeyPoint()
        val frameDescriptors = Mat()
        frameOrb.detectAndCompute(grayscaleFrame, Mat(), frameKeypoints, frameDescriptors)

        if (frameDescriptors.empty()) {
            frameKeypoints.release()
            frameDescriptors.release()
            return emptyList()
        }

        val frameKp = frameKeypoints.toArray()
        var best: MarkerResult? = null
        var bestInlierCount = 0

        for (target in targets) {
            if (target.descriptors.empty()) continue

            val knnMatches = mutableListOf<MatOfDMatch>()
            matcher.knnMatch(frameDescriptors, target.descriptors, knnMatches, 2)

            val goodMatches = knnMatches.mapNotNull { m ->
                val pair = m.toArray()
                if (pair.size == 2 && pair[0].distance < ratioTestThreshold * pair[1].distance) pair[0] else null
            }
            if (goodMatches.size < minGoodMatches) continue

            val targetKp = target.keypoints.toArray()
            val srcPoints = MatOfPoint2f(*goodMatches.map { targetKp[it.trainIdx].pt }.toTypedArray())
            val dstPoints = MatOfPoint2f(*goodMatches.map { frameKp[it.queryIdx].pt }.toTypedArray())
            val inlierMask = Mat()

            val homography = Geometry.findHomography(
                srcPoints, dstPoints, Geometry.RANSAC, ransacReprojThresholdPx, inlierMask, 2000, 0.995
            )
            srcPoints.release()
            dstPoints.release()

            val inlierCount = if (homography.empty()) 0 else Core.countNonZero(inlierMask)
            inlierMask.release()

            val inlierRatio = inlierCount.toDouble() / goodMatches.size
            val isConfident = !homography.empty() && inlierCount >= minGoodMatches && inlierRatio >= minInlierRatio

            if (isConfident && inlierCount > bestInlierCount) {
                bestInlierCount = inlierCount
                best = MarkerResult(id = target.id, corners = transformCorners(homography, target.widthPx, target.heightPx))
            }
            homography.release()
        }

        frameKeypoints.release()
        frameDescriptors.release()

        return listOfNotNull(best)
    }

    override fun physicalSizeMeters(id: Int): Pair<Double, Double> {
        val target = targets.firstOrNull { it.id == id } ?: return ASSUMED_LONG_SIDE_METERS to ASSUMED_LONG_SIDE_METERS
        return target.widthMeters to target.heightMeters
    }

    private fun transformCorners(homography: Mat, width: Int, height: Int): List<Point> {
        val srcCorners = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width.toDouble(), 0.0),
            Point(width.toDouble(), height.toDouble()),
            Point(0.0, height.toDouble())
        )
        val dstCorners = MatOfPoint2f()
        Core.perspectiveTransform(srcCorners, dstCorners, homography)
        val result = dstCorners.toList()
        srcCorners.release()
        dstCorners.release()
        return result
    }

    companion object {
        /** Assumed real-world length of a reference image's longer side, in meters (~a folded cloth/print). */
        const val ASSUMED_LONG_SIDE_METERS = 0.20
    }
}
