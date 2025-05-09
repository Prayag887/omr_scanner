package com.example.myapplication.domain.repository

import android.graphics.Bitmap
import com.example.myapplication.domain.model.DocumentScanResult
import org.opencv.core.Mat

interface DocumentRepository {
    fun saveScannedDocument(mat: Mat, fileName: String): String?
    fun loadLatestScannedDocument(): DocumentScanResult
    fun processFrame(frame: Mat): Mat
}