package com.example.myapplication.domain.omrresult.repository

import com.example.myapplication.domain.omrresult.model.OMRResult

interface OMRRepository {
    fun processOMR(imagePath: String): List<OMRResult>
}
