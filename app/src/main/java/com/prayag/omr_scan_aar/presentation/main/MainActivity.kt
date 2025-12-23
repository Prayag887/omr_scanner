package com.prayag.omr_scan_aar.presentation.main

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.prayag.omr_scan_aar.R
import com.prayag.omr_scan_aar.presentation.omrresult.ResultActivity
import com.prayag.omr_scan_aar.presentation.scanner.DocumentScannerActivity
import com.prayag.omr_scan_aar.presentation.scanner.DocumentScannerCallback
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.opencv.android.*
import org.opencv.core.Mat

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val viewModel: MainViewModel by viewModel()

    private lateinit var imageView: ImageView
    private lateinit var cameraView: JavaCameraView
    private lateinit var btnCapture: Button
    private lateinit var btnGallery: Button
    private lateinit var btnProcess: Button

    private var currentFrame: Mat? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 300
    }

//    -------------------- Gallery Picker --------------------

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            viewModel.saveBitmapAsDocument(bitmap)

                            imageView.setImageBitmap(bitmap)
                            imageView.visibility = View.VISIBLE
                            cameraView.visibility = View.GONE

                            btnProcess.visibility = View.VISIBLE
                            btnProcess.isEnabled = true

                            Toast.makeText(this, "Image selected from gallery", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load gallery image", e)
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

//     -------------------- OpenCV Loader --------------------

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

//    -------------------- Lifecycle --------------------

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
        btnGallery = findViewById(R.id.btnGallery)
        btnProcess = findViewById(R.id.btnProcess)

        btnProcess.isEnabled = true
        cameraView.visibility = View.GONE
    }

    private fun setupListeners() {
        btnCapture.setOnClickListener { handleCaptureClick() }

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnProcess.setOnClickListener {
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("image_path", "${filesDir}/paper.png")
            startActivity(intent)
        }
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
        viewModel.isLiveMode.observe(this) { updateUIForLiveMode(it) }

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

//    -------------------- Capture / Scanner --------------------

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
        DocumentScannerActivity.scannerCallback = object : DocumentScannerCallback {
            override fun onDocumentScanned(success: Boolean, filePath: String?) {
                runOnUiThread {
                    if (success && filePath != null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Image captured successfully! You can process it now.",
                            Toast.LENGTH_LONG
                        ).show()

                        val bitmap = BitmapFactory.decodeFile(filePath)
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        btnProcess.visibility = View.VISIBLE
                        btnProcess.isEnabled = true
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Document scanning canceled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        startActivity(Intent(this, DocumentScannerActivity::class.java))
    }

//    -------------------- Live Mode --------------------

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
            btnProcess.visibility =
                if (viewModel.documentScanResult.value?.bitmap != null) View.VISIBLE else View.GONE
            btnCapture.text = "Capture"
        }
    }

//    -------------------- Camera Permission -------------------/

    private fun checkCameraPermissionAndToggleLiveMode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.toggleLiveMode()
        }
    }

//    -------------------- OpenCV Lifecycle --------------------

    override fun onResume() {
        super.onResume()

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback)
        } else {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }

        if (viewModel.isLiveMode.value == true && viewModel.isOpenCVLoaded.value == true) {
            cameraView.enableView()
        }

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

//     -------------------- Camera Callbacks --------------------

    override fun onCameraViewStarted(width: Int, height: Int) {}
    override fun onCameraViewStopped() {}

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val rgba = inputFrame?.rgba() ?: return Mat()
        currentFrame = rgba.clone()
        return viewModel.processFrame(rgba)
    }

    private fun convertMatToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
}
