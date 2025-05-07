package com.example.myapplication

import android.app.Activity
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DocumentScannerActivity : ComponentActivity() {

    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var resultProcessed = false  // Flag to track if result was already processed


    // Add a companion object to hold the callback instance
    companion object {
        var scannerCallback: DocumentScannerCallback? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure scanner options
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false) // Disable gallery import
            .setPageLimit(2) // Limit pages to 2 (can be adjusted)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF) // Acceptable result formats
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL) // Full mode scanner
            .build()

        // Create scanner client
        val scanner = GmsDocumentScanning.getClient(options)

        // Register an ActivityResultCallback to handle the scan results
        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            resultProcessed = true // Mark that we've processed a result
            if (result.resultCode == RESULT_OK) {
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                scanningResult?.pages?.let { pages ->
                    for (page in pages) {
                        val imageUri = page.getImageUri()
                        val filePath = saveImageToFile(imageUri)
                        // Notify success through callback
                        if (filePath != null) {
                            scannerCallback?.onDocumentScanned(true, filePath.toString())
                        } else {
                            scannerCallback?.onDocumentScanned(false)
                        }
                    }
                }?: run {
                    // No pages scanned
                    scannerCallback?.onDocumentScanned(false)
                }

                scanningResult?.pdf?.let { pdf ->
                    val pdfUri = pdf.getUri()
                    val pageCount = pdf.getPageCount()
                    // Handle PDF saving or displaying
                }
            } else {
                // Handle cancel/failure case - notify through callback
                scannerCallback?.onDocumentScanned(false)

            }
            scannerCallback = null
            finish()
        }

        // Launch the document scanner
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                // Handle failure in scanning initialization
                resultProcessed = true
                scannerCallback?.onDocumentScanned(false)
                scannerCallback = null //clear callback reference
                finish()
            }
    }

    // Save image to file and return the Bitmap
    private fun saveImageToFile(imageUri: Uri):String? {
        try {
            val contentResolver: ContentResolver = contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(imageUri)!!

            // Convert InputStream to Bitmap
            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

            // Create the file where the image will be saved
            val file = File(filesDir, "paper.png")
            val fileOutputStream = FileOutputStream(file)

            // Compress the Bitmap and save it as PNG
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)

            // Close the file output stream
            fileOutputStream.flush()
            fileOutputStream.close()

            // Optionally, display a message or log the successful save
            println("Image saved to ${file.absolutePath}")
            goBack()
            // Return the absolute path
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Go back to the previous page after scanning is complete
    private fun goBack() {
        // Close the current activity and return to the previous one
        onBackPressed()
    }


    override fun onBackPressed() {
        // Only notify if no result was processed yet
        if (!resultProcessed) {
            resultProcessed = true
            scannerCallback?.onDocumentScanned(false)
            scannerCallback = null  // Clear callback reference
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        // Only notify if no result was processed yet
        if (!resultProcessed) {
            resultProcessed = true
            scannerCallback?.onDocumentScanned(false)
            scannerCallback = null  // Clear callback reference
        }
        super.onDestroy()
    }
}

// Add a callback interface for document scanning results
interface DocumentScannerCallback {
    fun onDocumentScanned(success: Boolean, filePath: String? = null)
}