package com.example.myapplication.utils

import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class OpenCVUtils {
    private val TAG = "OpenCVUtils"

    fun findDocumentContours(grayMat: Mat): List<MatOfPoint> {
        try {
            // Apply Gaussian blur
            Imgproc.GaussianBlur(grayMat, grayMat, Size(5.0, 5.0), 0.0)

            // Edge detection
            val edges = Mat()
            Imgproc.Canny(grayMat, edges, 50.0, 150.0)

            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                edges,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            edges.release()
            hierarchy.release()

            return contours
        } catch (e: Exception) {
            Log.e(TAG, "Error finding contours: ${e.message}")
            return emptyList()
        }
    }

    fun findLargestRectangularContour(contours: List<MatOfPoint>): MatOfPoint? {
        var largestContour: MatOfPoint? = null
        var maxArea = 0.0

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            val approx = MatOfPoint2f()
            val contour2f = MatOfPoint2f(*contour.toArray())

            // Approximate contour to a polygon
            Imgproc.approxPolyDP(
                contour2f,
                approx,
                Imgproc.arcLength(contour2f, true) * 0.02,
                true
            )

            if (area > maxArea && approx.total() == 4L) { // Ensure 4 points (rectangular)
                maxArea = area
                largestContour = MatOfPoint(*approx.toArray())
            }
        }

        return largestContour
    }
}