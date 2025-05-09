// domain/repository/DocumentRepositoryImpl.kt
package com.example.myapplication.domain.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.example.myapplication.domain.model.DocumentScanResult
import org.koin.core.component.KoinComponent
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

class DocumentRepositoryImpl(
    private val context: Context
) : DocumentRepository, KoinComponent {

    private val TAG = "DocumentRepositoryImpl"

    override fun saveScannedDocument(mat: Mat, fileName: String): String? {
        val filePath = "${context.filesDir}/$fileName"
        try {
            return if (Imgcodecs.imwrite(filePath, mat)) filePath else null
        } catch (e: Exception) {
            Log.e(TAG, "Error saving document: ${e.message}", e)
            return null
        }
    }

    override fun loadLatestScannedDocument(): DocumentScanResult {
        val file = File(context.filesDir, "paper.png")
        return if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    DocumentScanResult(true, bitmap, file.absolutePath)
                } else {
                    DocumentScanResult(false, errorMessage = "Failed to decode image")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading document: ${e.message}", e)
                DocumentScanResult(false, errorMessage = "Error loading document: ${e.message}")
            }
        } else {
            DocumentScanResult(false, errorMessage = "No scanned document found")
        }
    }

    override fun processFrame(frame: Mat): Mat {
        val rgba = frame.clone()
        try {
            // Rotate frame by 90 degrees
            val rotatedFrame = Mat()
            Core.rotate(rgba, rotatedFrame, Core.ROTATE_90_CLOCKWISE)

            // Convert to grayscale
            val gray = Mat()
            Imgproc.cvtColor(rotatedFrame, gray, Imgproc.COLOR_RGBA2GRAY)

            // Apply Gaussian blur to reduce noise
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

            // Detect edges using Canny
            val edges = Mat()
            Imgproc.Canny(gray, edges, 50.0, 150.0)

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

            var largestContour: MatOfPoint? = null
            var maxArea = 0.0

            // Find the largest quadrilateral contour (paper)
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

            if (largestContour != null) {
                // Draw the detected paper contour in blue
                Imgproc.drawContours(
                    rotatedFrame,
                    listOf(largestContour),
                    -1,
                    Scalar(255.0, 0.0, 0.0, 255.0),
                    3
                )

                // Get the 4 points of the detected paper
                val points = largestContour.toArray().sortedBy { it.y } // Sort by Y-coordinates

                if (points.size == 4) {
                    // Ensure correct point ordering: TL, TR, BR, BL
                    val sortedPoints = listOf(
                        if (points[0].x < points[1].x) points[0] else points[1], // Top-left
                        if (points[0].x > points[1].x) points[0] else points[1], // Top-right
                        if (points[2].x > points[3].x) points[2] else points[3], // Bottom-right
                        if (points[2].x < points[3].x) points[2] else points[3]  // Bottom-left
                    )

                    // Define destination points (standard paper size)
                    val width = 800
                    val height = 1100
                    val srcMat = MatOfPoint2f(*sortedPoints.toTypedArray())
                    val dstMat = MatOfPoint2f(
                        Point(0.0, 0.0), Point(width.toDouble(), 0.0),
                        Point(width.toDouble(), height.toDouble()), Point(0.0, height.toDouble())
                    )

                    // Apply perspective transform
                    val perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
                    val paperWarped = Mat()
                    Imgproc.warpPerspective(
                        rotatedFrame,
                        paperWarped,
                        perspectiveTransform,
                        Size(width.toDouble(), height.toDouble())
                    )

                    // Save the isolated paper image when detected
                    val filePath = "${context.filesDir}/paper.png"
                    Imgcodecs.imwrite(filePath, paperWarped)

                    // Clean up
                    gray.release()
                    edges.release()
                    hierarchy.release()

                    return rotatedFrame
                }
            }

            // Release temporary Mats
            gray.release()
            edges.release()
            hierarchy.release()

            return rotatedFrame
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}", e)
            return rgba
        }
    }
}