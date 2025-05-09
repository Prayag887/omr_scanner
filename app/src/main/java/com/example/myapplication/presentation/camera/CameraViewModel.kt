package com.example.myapplication.presentation.camera

import androidx.lifecycle.ViewModel
import com.example.myapplication.domain.repository.DocumentRepository
import com.example.myapplication.utils.BitmapUtils
import org.opencv.core.Mat

class CameraViewModel(
    private val documentRepository: DocumentRepository,
    private val bitmapUtils: BitmapUtils
) : ViewModel() {

    fun processFrame(inputFrame: Mat): Mat {
        return documentRepository.processFrame(inputFrame)
    }

    fun saveProcessedFrame(processedFrame: Mat): String? {
        return documentRepository.saveScannedDocument(processedFrame, "paper.png")
    }
}