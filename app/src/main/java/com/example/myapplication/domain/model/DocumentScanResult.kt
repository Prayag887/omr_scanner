package com.example.myapplication.domain.model

import android.graphics.Bitmap

data class DocumentScanResult(
    val success: Boolean,
    val bitmap: Bitmap? = null,
    val filePath: String? = null,
    val errorMessage: String? = null
)