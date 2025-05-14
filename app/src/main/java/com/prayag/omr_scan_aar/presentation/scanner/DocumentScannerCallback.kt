package com.prayag.omr_scan_aar.presentation.scanner

interface DocumentScannerCallback {
    fun onDocumentScanned(success: Boolean, filePath: String? = null)
}
