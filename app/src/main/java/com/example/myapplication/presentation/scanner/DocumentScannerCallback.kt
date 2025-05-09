package com.example.myapplication.presentation.scanner

interface DocumentScannerCallback {
    fun onDocumentScanned(success: Boolean, filePath: String? = null)
}
