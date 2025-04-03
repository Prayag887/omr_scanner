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

    // Define a callback interface to return the Bitmap
    interface ImageScanCallback {
        fun onImageScanned(bitmap: Bitmap)
    }

    var imageScanCallback: ImageScanCallback? = null

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
            if (result.resultCode == RESULT_OK) {
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                scanningResult?.pages?.let { pages ->
                    for (page in pages) {
                        val imageUri = page.getImageUri()
                        // Save the scanned image to a file
                        val bitmap = saveImageToFile(imageUri)

                    }
                }

                scanningResult?.pdf?.let { pdf ->
                    val pdfUri = pdf.getUri()
                    val pageCount = pdf.getPageCount()
                    // Handle PDF saving or displaying
                }
            } else {
                // Handle failure case
            }
        }

        // Launch the document scanner
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                // Handle failure in scanning initialization
            }
    }

    // Save image to file and return the Bitmap
    private fun saveImageToFile(imageUri: Uri) {
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
            // Return the bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Go back to the previous page after scanning is complete
    private fun goBack() {
        // Close the current activity and return to the previous one
        onBackPressed()
    }

    override fun onBackPressed() {
        // Optionally, call the callback again here before exiting the activity
        imageScanCallback?.let {
            val dummyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Return a dummy bitmap if necessary
            it.onImageScanned(dummyBitmap)
        }

        super.onBackPressed()
    }
}
