package com.prayag.omr_scan_aar.presentation.scanner

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class DocumentScannerActivity : ComponentActivity() {

    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var resultProcessed = false
    private val viewModel: DocumentScannerViewModel by viewModels()

    companion object {
        var scannerCallback: DocumentScannerCallback? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(2)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            resultProcessed = true
            if (result.resultCode == RESULT_OK) {
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                scanningResult?.pages?.forEach { page ->
                    val imageUri: Uri = page.getImageUri()
                    val filePath = viewModel.saveImageToFile(imageUri, contentResolver, filesDir)
                    if (filePath != null) {
                        scannerCallback?.onDocumentScanned(true, filePath)
                    } else {
                        scannerCallback?.onDocumentScanned(false)
                    }
                } ?: run {
                    scannerCallback?.onDocumentScanned(false)
                }

                scanningResult?.pdf?.let {
                    val pdfUri = it.uri
                    val pageCount = it.pageCount
                    // Optional: handle PDF display or save
                }
            } else {
                scannerCallback?.onDocumentScanned(false)
            }
            scannerCallback = null
            finish()
        }

        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                resultProcessed = true
                scannerCallback?.onDocumentScanned(false)
                scannerCallback = null
                finish()
            }
    }

    override fun onBackPressed() {
        if (!resultProcessed) {
            resultProcessed = true
            scannerCallback?.onDocumentScanned(false)
            scannerCallback = null
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        if (!resultProcessed) {
            resultProcessed = true
            scannerCallback?.onDocumentScanned(false)
            scannerCallback = null
        }
        super.onDestroy()
    }
}
