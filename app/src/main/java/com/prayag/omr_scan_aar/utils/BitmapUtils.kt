package com.prayag.omr_scan_aar.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import org.opencv.android.Utils
import org.opencv.core.Mat

class BitmapUtils {

    fun rotateBitmap(bitmap: Bitmap?, degrees: Float): Bitmap? {
        if (bitmap == null) return null

        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        val bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bmp, mat)
        return mat
    }

    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
}