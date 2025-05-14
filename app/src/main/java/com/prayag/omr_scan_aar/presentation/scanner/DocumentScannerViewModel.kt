package com.prayag.omr_scan_aar.presentation.scanner

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DocumentScannerViewModel : ViewModel() {

    fun saveImageToFile(imageUri: Uri, contentResolver: ContentResolver, filesDir: File): String? {
        return try {
            val inputStream: InputStream = contentResolver.openInputStream(imageUri)!!
            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

            val file = File(filesDir, "paper.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
