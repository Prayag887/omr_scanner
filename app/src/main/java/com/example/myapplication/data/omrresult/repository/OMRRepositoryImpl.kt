package com.example.myapplication.data.omrresult.repository

import android.graphics.BitmapFactory
import com.example.myapplication.domain.omrresult.model.OMRResult
import com.example.myapplication.domain.omrresult.repository.OMRRepository
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class OMRRepositoryImpl : OMRRepository {
    external fun processOMR(matAddrInput: Long): IntArray

    override fun processOMR(imagePath: String): List<OMRResult> {
        val paperBitmap = BitmapFactory.decodeFile(imagePath) ?: return emptyList()
        val mat = Mat().also { Utils.bitmapToMat(paperBitmap, it) }

        val processedMat = if (mat.channels() == 4) {
            Mat().also { Imgproc.cvtColor(mat, it, Imgproc.COLOR_RGBA2RGB) }
        } else mat.clone()

        val resultArray = processOMR(processedMat.nativeObjAddr)

        mat.release()
        processedMat.release()

        return resultArray.mapIndexed { index, answer ->
            OMRResult(index + 1, answer)
        }
    }
}
