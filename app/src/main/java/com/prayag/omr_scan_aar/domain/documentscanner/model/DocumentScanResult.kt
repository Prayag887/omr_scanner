package com.prayag.omr_scan_aar.domain.documentscanner.model

import android.graphics.Bitmap

data class DocumentScanResult(
    val success: Boolean,
    val bitmap: Bitmap? = null,
    val filePath: String? = null,
    val errorMessage: String? = null
)