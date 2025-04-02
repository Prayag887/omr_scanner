package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.opencv.android.*
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
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private lateinit var imageView: ImageView
    private lateinit var cameraView: JavaCameraView
    private lateinit var btnCapture: Button
    private lateinit var btnProcess: Button
    private lateinit var btnToggleCamera: Button

    private var selectedBitmap: Bitmap? = null
    private var isOpenCVLoaded = false
    private var currentPhotoPath: String? = null
    private var isLiveMode = false
    private var currentFrame: Mat? = null
    var circularContourCount = 0
    var detected = false

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_REQUEST_CODE = 100
        private const val GALLERY_REQUEST_CODE = 200
        private const val CAMERA_PERMISSION_REQUEST_CODE = 300
        private const val MARKER_NAME = "omr_marker.jpg"
    }

    private external fun processOMR(matAddrInput: Long): IntArray

    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    isOpenCVLoaded = true
                    btnProcess.isEnabled = selectedBitmap != null
                    cameraView.setCameraPermissionGranted()
                    cameraView.setCvCameraViewListener(this@MainActivity)
                }
                else -> {
                    super.onManagerConnected(status)
                    Toast.makeText(
                        this@MainActivity,
                        "OpenCV failed to load. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    isOpenCVLoaded = false
                    btnProcess.isEnabled = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        cameraView = findViewById(R.id.camera_view)
        btnCapture = findViewById(R.id.btnCapture)
        btnProcess = findViewById(R.id.btnProcess)
        btnToggleCamera = findViewById(R.id.btnToggleCamera)

        btnProcess.isEnabled = false
        cameraView.visibility = View.GONE

        btnCapture.setOnClickListener { handleCaptureClick() }
        btnProcess.setOnClickListener { processImage() }
        btnToggleCamera.setOnClickListener { toggleLiveMode() }

        try {
            System.loadLibrary("myapplication")
            Log.d(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library", e)
            Toast.makeText(this, "Failed to load native library: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleCaptureClick() {
        if (isLiveMode) {
            currentFrame?.let { mat ->
                selectedBitmap = convertMatToBitmap(mat)
                selectedBitmap = rotateBitmap(selectedBitmap, 90f)
                checkMarkerAndDisplay(selectedBitmap)
                toggleLiveMode()
            }
        } else {
            openCamera()
        }
    }

    private fun rotateBitmap(bitmap: Bitmap?, degrees: Float): Bitmap? {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    private fun convertMatToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    private fun toggleLiveMode() {
        if (isLiveMode) {
            // Switch to static mode
            isLiveMode = false
            cameraView.disableView()
            cameraView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            btnProcess.visibility = if (selectedBitmap != null) View.VISIBLE else View.GONE
            btnCapture.text = "Capture"
        } else {
            // Check camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
                return
            }
            // Switch to live mode
            isLiveMode = true
            imageView.visibility = View.GONE
            cameraView.visibility = View.VISIBLE
            btnProcess.visibility = View.GONE
            btnCapture.text = "Capture Frame"
            if (isOpenCVLoaded) {
                cameraView.enableView()
            } else {
                Toast.makeText(this, "OpenCV not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toggleLiveMode()
                } else {
                    Toast.makeText(this, "Camera permission required for live mode", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback)
        } else {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
        if (isLiveMode && isOpenCVLoaded) {
            cameraView.enableView()
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }

    // Camera callbacks
    override fun onCameraViewStarted(width: Int, height: Int) {}
    override fun onCameraViewStopped() {}

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val rgba = inputFrame?.rgba() ?: return Mat()
        currentFrame = rgba.clone()

        try {
            // Rotate frame by 90 degrees
            val rotatedFrame = Mat()
            Core.rotate(rgba, rotatedFrame, Core.ROTATE_90_CLOCKWISE)

            // Convert to grayscale
            val gray = Mat()
            Imgproc.cvtColor(rotatedFrame, gray, Imgproc.COLOR_RGBA2GRAY)

            // Apply Gaussian blur to reduce noise
            Imgproc.GaussianBlur(gray, gray, org.opencv.core.Size(5.0, 5.0), 0.0)

            // Detect edges using Canny
            val edges = Mat()
            Imgproc.Canny(gray, edges, 50.0, 150.0)

            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            var largestContour: MatOfPoint? = null
            var maxArea = 0.0

            // Find the largest quadrilateral contour (paper)
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                val approx = MatOfPoint2f()
                val contour2f = MatOfPoint2f(*contour.toArray())

                // Approximate contour to a polygon
                Imgproc.approxPolyDP(contour2f, approx, Imgproc.arcLength(contour2f, true) * 0.02, true)

                if (area > maxArea && approx.total() == 4L) { // Ensure 4 points (rectangular)
                    maxArea = area
                    largestContour = MatOfPoint(*approx.toArray())
                }
            }

            if (largestContour != null) {
                // Draw the detected paper contour in blue
                Imgproc.drawContours(rotatedFrame, listOf(largestContour), -1, Scalar(255.0, 0.0, 0.0, 255.0), 3)

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
                    Imgproc.warpPerspective(rotatedFrame, paperWarped, perspectiveTransform, Size(width.toDouble(), height.toDouble()))

                    // Save the isolated paper image when detected
                    val filePath = "/data/data/com.example.myapplication/files/paper.png"
                    Imgcodecs.imwrite(filePath, paperWarped)
                }
            }

            // Release temporary Mats
            gray.release()
            edges.release()
            hierarchy.release()

            return rotatedFrame
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
        }

        return rgba
    }


    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile = createImageFile()
                val photoURI = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
                currentPhotoPath = photoFile.absolutePath
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            .apply { currentPhotoPath = absolutePath }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        selectedBitmap = loadBitmapFromUri(uri)
                        checkMarkerAndDisplay(selectedBitmap)
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    currentPhotoPath?.let { path ->
                        val bitmap = BitmapFactory.decodeFile(path)
                        selectedBitmap = bitmap
                        checkMarkerAndDisplay(bitmap)
                    }
                }
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    private fun checkMarkerAndDisplay(bitmap: Bitmap?) {
        bitmap?.let {
            val mat = Mat()
            Utils.bitmapToMat(it.copy(Bitmap.Config.ARGB_8888, true), mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR)

            // Fetch marker file from DCIM
            val markerFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MARKER_NAME)

            Log.d(TAG, "Looking for marker file at: ${markerFile.absolutePath}")

            if (!markerFile.exists()) {
                Log.e(TAG, "Marker file not found at ${markerFile.absolutePath}")
                Toast.makeText(this, "Marker file not found at ${markerFile.absolutePath}", Toast.LENGTH_LONG).show()
                return
            }

            Log.d(TAG, "Marker file found at: ${markerFile.absolutePath}")

            // Detect marker and update image
            if (true) {
                // Convert modified Mat back to Bitmap
                val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(mat, resultBitmap)

                // Display processed image
                imageView.setImageBitmap(resultBitmap)
                btnProcess.visibility = View.VISIBLE
                btnProcess.isEnabled = isOpenCVLoaded
            } else {
                Log.e(TAG, "Marker not detected")
                Toast.makeText(this, "Marker not found. Cannot use image.", Toast.LENGTH_LONG).show()
                selectedBitmap = null
                imageView.setImageBitmap(null)
                btnProcess.visibility = View.GONE
            }
        }
    }

    private fun processImage() {
        // Save the isolated paper image when detected
        val filePath = "/data/data/com.example.myapplication/files/paper.png"
        val paperBitmap = BitmapFactory.decodeFile(filePath)

        // Ensure the image is not null
        if (paperBitmap != null) {
            // Get the original width and height
            val width = paperBitmap.width
            val height = paperBitmap.height

            // Set the target size for ESRGAN (512x512 as per your model)
            val targetWidth = 512
            val targetHeight = 512

            // Resize the bitmap to the target size
            val resizedBitmap = if (width != targetWidth || height != targetHeight) {
                Bitmap.createScaledBitmap(paperBitmap, targetWidth, targetHeight, true)
            } else {
                paperBitmap
            }

            // ESRGAN Enhancement for Blur Image Correction
            try {
                val esrganModel = ESRGANModel(this)

                // Check if the resized bitmap is valid
                if (resizedBitmap.width <= 0 || resizedBitmap.height <= 0) {
                    Log.e(TAG, "Resized bitmap has invalid dimensions: ${resizedBitmap.width}x${resizedBitmap.height}")
                    return
                }

                // Enhance the image with ESRGAN
                val enhancedBitmap = esrganModel.enhanceImage(resizedBitmap)

                // Save the enhanced image back to file
                val enhancedMat = Mat()
                Utils.bitmapToMat(enhancedBitmap, enhancedMat)

                // Save the enhanced image as paper_enhanced.png
                val enhancedFilePath = "/data/data/com.example.myapplication/files/paper_enhanced.png"
                Imgcodecs.imwrite(enhancedFilePath, enhancedMat)

                // Clean up Mat objects to avoid memory leaks
                enhancedMat.release()

                // Log success
                Log.d(TAG, "Enhanced image saved successfully: $enhancedFilePath")
            } catch (e: Exception) {
                Log.e(TAG, "Error enhancing image: ${e.message}")
            }
        } else {
            Log.e(TAG, "Failed to load paper image from file")
        }

        // Process other selectedBitmap (if needed)
        selectedBitmap?.let {
            val mat = Mat()
            Utils.bitmapToMat(it, mat)

            // Ensure correct number of channels before sending to native code
            var processedMat = Mat()
            if (mat.channels() == 4) {
                Imgproc.cvtColor(mat, processedMat, Imgproc.COLOR_RGBA2RGB) // Remove Alpha channel
            } else {
                processedMat = mat.clone()
            }

            // Send Mat address to native function
            val resultArray = processOMR(processedMat.nativeObjAddr)

            // Convert resultArray to an Intent Extra
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("omr_results", resultArray)
            }

            // Start the ResultActivity
            startActivity(intent)

            Log.d(TAG, "OMR Result: ${resultArray.joinToString(", ")}")

            // The C++ code now draws contours on the processedMat directly
            // Display the processed image with contours
            val resultBitmap = Bitmap.createBitmap(processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(processedMat, resultBitmap)

            runOnUiThread {
                imageView.setImageBitmap(resultBitmap)
                Toast.makeText(this, "OMR processing completed", Toast.LENGTH_SHORT).show()
            }

            // Cleanup
            mat.release()
            processedMat.release()
        }
    }
}