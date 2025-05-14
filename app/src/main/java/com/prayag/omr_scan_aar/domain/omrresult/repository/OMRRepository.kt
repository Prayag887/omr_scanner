package com.prayag.omr_scan_aar.domain.omrresult.repository

import com.prayag.omr_scan_aar.domain.omrresult.model.OMRResult

interface OMRRepository {
    fun processOMR(imagePath: String): List<OMRResult>
}
