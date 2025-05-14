package com.prayag.omr_scan_aar.data.omrresult.repository

import android.graphics.BitmapFactory
import com.prayag.omr_scan_aar.domain.omrresult.model.OMRResult
import com.prayag.omr_scan_aar.domain.omrresult.repository.OMRRepository
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
