package com.prayag.omr_scan_aar.data.omrresult.repository

import android.graphics.BitmapFactory
import android.util.Log
import com.prayag.omr_scan_aar.domain.omrresult.model.OMRResult
import com.prayag.omr_scan_aar.domain.omrresult.repository.OMRRepository
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File

class OMRRepositoryImpl : OMRRepository {
    external fun processOMR(matAddrInput: Long, packageName: String): IntArray

    private val TAG = "OMRRepositoryImpl"

    override fun processOMR(imagePath: String): List<OMRResult> {
        val paperBitmap = BitmapFactory.decodeFile(imagePath) ?: return emptyList()
        val mat = Mat().also { Utils.bitmapToMat(paperBitmap, it) }

        val processedMat = if (mat.channels() == 4) {
            Mat().also { Imgproc.cvtColor(mat, it, Imgproc.COLOR_RGBA2RGB) }
        } else mat.clone()

        // Extract only the package name from the path
        val originalDirPath = File(imagePath).parent ?: ""
        val packageName = extractPackageName(originalDirPath)

        Log.d(TAG, "Original path: $originalDirPath")
        Log.d(TAG, "Package name: $packageName")

        // Pass both the mat address and the package name to JNI
        val resultArray = processOMR(processedMat.nativeObjAddr, packageName)

        mat.release()
        processedMat.release()

        return resultArray.mapIndexed { index, answer ->
            OMRResult(index + 1, answer)
        }
    }

    private fun extractPackageName(path: String): String {
        // Pattern 1: /data/user/0/com.package.name/...
        val userDataPattern = "/data/user/\\d+/([\\w.]+).*".toRegex()
        userDataPattern.find(path)?.let {
            return it.groupValues[1]
        }

        // Pattern 2: /data/data/com.package.name/...
        val dataDataPattern = "/data/data/([\\w.]+).*".toRegex()
        dataDataPattern.find(path)?.let {
            return it.groupValues[1]
        }

        // Pattern 4: .../com.package.name/files/...
        val generalPattern = ".*/([\\w.]+\\.\\w+)/files.*".toRegex()
        generalPattern.find(path)?.let {
            return it.groupValues[1]
        }

        // If we can't determine the package name, log warning and return empty string
        Log.w(TAG, "Could not extract package name from path: $path")
        return ""
    }
}