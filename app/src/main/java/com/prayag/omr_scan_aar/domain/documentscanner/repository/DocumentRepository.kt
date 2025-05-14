package com.prayag.omr_scan_aar.domain.documentscanner.repository

import com.prayag.omr_scan_aar.domain.documentscanner.model.DocumentScanResult
import org.opencv.core.Mat

interface DocumentRepository {
    fun saveScannedDocument(mat: Mat, fileName: String): String?
    fun loadLatestScannedDocument(): DocumentScanResult
    fun processFrame(frame: Mat): Mat
}