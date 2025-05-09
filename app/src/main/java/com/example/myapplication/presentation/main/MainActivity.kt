package com.example.myapplication.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.presentation.omrresult.ResultActivity
import com.example.myapplication.presentation.scanner.DocumentScannerActivity
import com.example.myapplication.presentation.scanner.DocumentScannerCallback
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val viewModel: MainViewModel by viewModel()

    private lateinit var imageView: ImageView
    private lateinit var cameraView: JavaCameraView
    private lateinit var btnCapture: Button
    private lateinit var btnProcess: Button
//    private lateinit var btnToggleCamera: FloatingActionButton

    private var currentFrame: Mat? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 300
    }

    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    viewModel.setOpenCVLoaded(true)
                    btnProcess.isEnabled = true
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
                    viewModel.setOpenCVLoaded(false)
                    btnProcess.isEnabled = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupListeners()
        loadNativeLibrary()
        observeViewModel()
        loadLatestImage()
    }

    private fun initializeViews() {
        imageView = findViewById(R.id.previewOmrImage)
        cameraView = findViewById(R.id.camera_view)
        btnCapture = findViewById(R.id.btnCapture)
        btnProcess = findViewById(R.id.btnProcess)
//        btnToggleCamera = findViewById(R.id.btnToggleCamera)

        btnProcess.isEnabled = true
        cameraView.visibility = View.GONE
    }

    private fun setupListeners() {
        btnCapture.setOnClickListener { handleCaptureClick() }
        btnProcess.setOnClickListener {
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("image_path", "${filesDir}/paper.png")
            startActivity(intent)
        }
//        btnToggleCamera.setOnClickListener { handleToggleCameraClick() }
    }

    private fun loadNativeLibrary() {
        try {
            System.loadLibrary("myapplication")
            Log.d(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library", e)
            Toast.makeText(this, "Failed to load native library: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun observeViewModel() {
        viewModel.isLiveMode.observe(this) { isLiveMode ->
            updateUIForLiveMode(isLiveMode)
        }

        viewModel.documentScanResult.observe(this) { result ->
            if (result.success && result.bitmap != null) {
                imageView.setImageBitmap(result.bitmap)
                imageView.visibility = View.VISIBLE
                btnProcess.visibility = View.VISIBLE
                btnProcess.isEnabled = viewModel.isOpenCVLoaded.value ?: false
            } else {
                result.errorMessage?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadLatestImage() {
        viewModel.loadLatestScannedDocument()
    }

    private fun handleCaptureClick() {
        if (viewModel.isLiveMode.value == true) {
            captureLiveFrame()
        } else {
            launchDocumentScanner()
        }
    }

    private fun captureLiveFrame() {
        currentFrame?.let { mat ->
            val bitmap = convertMatToBitmap(mat)
            viewModel.saveBitmapAsDocument(bitmap)
            viewModel.toggleLiveMode()
        }
    }

    private fun launchDocumentScanner() {
        // Set up the callback before launching scanner activity
        DocumentScannerActivity.scannerCallback = object : DocumentScannerCallback {
            override fun onDocumentScanned(success: Boolean, filePath: String?) {
                runOnUiThread {
                    if (success && filePath != null) {
                        // Show success toast
                        Toast.makeText(
                            this@MainActivity,
                            "Image captured successfully! You can process it now.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Refresh the image preview
                        val capturedBitmap = BitmapFactory.decodeFile(filePath)
                        imageView.setImageBitmap(capturedBitmap)
                        imageView.visibility = View.VISIBLE
                        btnProcess.visibility = View.VISIBLE
                        btnProcess.isEnabled = true
                    } else {
                        // Only show cancellation message if scan failed or was canceled
                        Toast.makeText(
                            this@MainActivity,
                            "Document scanning canceled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // Launch the document scanner activity
        val intent = Intent(this, DocumentScannerActivity::class.java)
        startActivity(intent)
    }

    private fun handleToggleCameraClick() {
        if (viewModel.isLiveMode.value == true) {
            viewModel.toggleLiveMode()
        } else {
            checkCameraPermissionAndToggleLiveMode()
        }
    }

    private fun checkCameraPermissionAndToggleLiveMode() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            viewModel.toggleLiveMode()
        }
    }

    private fun updateUIForLiveMode(isLiveMode: Boolean) {
        if (isLiveMode) {
            imageView.visibility = View.GONE
            cameraView.visibility = View.VISIBLE
            btnProcess.visibility = View.GONE
            btnCapture.text = "Capture Frame"
            if (viewModel.isOpenCVLoaded.value == true) {
                cameraView.enableView()
            } else {
                Toast.makeText(this, "OpenCV not loaded yet", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraView.disableView()
            cameraView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            btnProcess.visibility = if (viewModel.documentScanResult.value?.bitmap != null) View.VISIBLE else View.GONE
            btnCapture.text = "Capture"
        }
    }

    private fun convertMatToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.toggleLiveMode()
                } else {
                    Toast.makeText(
                        this,
                        "Camera permission required for live mode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback)
        } else {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }

        // Enable camera if in live mode
        if (viewModel.isLiveMode.value == true && viewModel.isOpenCVLoaded.value == true) {
            cameraView.enableView()
        }

        // Refresh the latest scanned document
        viewModel.loadLatestScannedDocument()
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
        return viewModel.processFrame(rgba)
    }
}