package com.example.myapplication.presentation.main

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.domain.documentscanner.model.DocumentScanResult
import com.example.myapplication.domain.documentscanner.repository.DocumentRepository
import com.example.myapplication.utils.BitmapUtils
import org.opencv.core.Mat

class MainViewModel(
    private val documentRepository: DocumentRepository,
    private val bitmapUtils: BitmapUtils
) : ViewModel() {

    private val _documentScanResult = MutableLiveData<DocumentScanResult>()
    val documentScanResult: LiveData<DocumentScanResult> = _documentScanResult

    private val _isOpenCVLoaded = MutableLiveData(false)
    val isOpenCVLoaded: LiveData<Boolean> = _isOpenCVLoaded

    private val _isLiveMode = MutableLiveData(false)
    val isLiveMode: LiveData<Boolean> = _isLiveMode

    fun setOpenCVLoaded(loaded: Boolean) {
        _isOpenCVLoaded.value = loaded
    }

    fun toggleLiveMode() {
        _isLiveMode.value = !(_isLiveMode.value ?: false)
    }

    fun loadLatestScannedDocument() {
        _documentScanResult.value = documentRepository.loadLatestScannedDocument()
    }

    fun processFrame(frame: Mat): Mat {
        return documentRepository.processFrame(frame)
    }

    fun saveBitmapAsDocument(bitmap: Bitmap) {
        val mat = bitmapUtils.bitmapToMat(bitmap)
        val filePath = documentRepository.saveScannedDocument(mat, "paper.png")
        _documentScanResult.value = DocumentScanResult(
            success = filePath != null,
            bitmap = bitmap,
            filePath = filePath,
            errorMessage = if (filePath == null) "Failed to save document" else null
        )
    }
}