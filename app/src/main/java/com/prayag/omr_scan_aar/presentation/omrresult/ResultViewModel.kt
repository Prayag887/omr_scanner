package com.prayag.omr_scan_aar.presentation.omrresult

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.prayag.omr_scan_aar.domain.omrresult.model.OMRResult
import com.prayag.omr_scan_aar.domain.omrresult.repository.OMRRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import java.io.File

data class TableCell(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class TableRow(
    val cells: List<String>,
    val isTableRow: Boolean = false
)

class ResultViewModel(
    private val repository: OMRRepository
) : ViewModel() {

    private val _results = MutableLiveData<List<OMRResult>>()
    val results: LiveData<List<OMRResult>> = _results

    private val _ocrTableData = MutableLiveData<List<TableRow>>()
    val ocrTableData: LiveData<List<TableRow>> = _ocrTableData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun processOCR(imagePath: String?) {
        if (imagePath == null) {
            _error.value = "Image path not found"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resultList = repository.processOMR(imagePath)
                _results.postValue(resultList)

                runMlKitOcr()

            } catch (e: Exception) {
                _error.postValue("Failed to process image: ${e.message}")
            }
        }
    }

    private suspend fun runMlKitOcr() = withContext(Dispatchers.IO) {
        try {
            val binaryFile = File(
                "/data/data/com.prayag.omr_scan_aar/files/6_final_binary.png"
            )

            if (!binaryFile.exists()) {
                _error.postValue("OCR image not found")
                return@withContext
            }

            val bitmap = BitmapFactory.decodeFile(
                binaryFile.absolutePath,
                BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
            )

            val image = InputImage.fromBitmap(bitmap, 0)

            val recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            )

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val structuredData = reconstructTableStructure(visionText)
                    _ocrTableData.postValue(structuredData)

                    // Also log for debugging
                    println("OCR Structured Data:")
                    structuredData.forEach { row ->
                        println(row.cells.joinToString(" | "))
                    }
                }
                .addOnFailureListener { e ->
                    _error.postValue("OCR failed: ${e.message}")
                }

        } catch (e: Exception) {
            _error.postValue("OCR exception: ${e.message}")
        }
    }

    /**
     * Reconstructs table structure from ML Kit vision text
     * Groups text blocks by rows and sorts them by columns
     */
    private fun reconstructTableStructure(visionText: Text): List<TableRow> {
        val cells = mutableListOf<TableCell>()

        // Extract all text elements with bounding boxes
        visionText.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    val box = element.boundingBox
                    if (box != null) {
                        cells.add(
                            TableCell(
                                text = element.text.trim(),
                                left = box.left,
                                top = box.top,
                                right = box.right,
                                bottom = box.bottom
                            )
                        )
                    }
                }
            }
        }

        if (cells.isEmpty()) {
            return listOf(TableRow(listOf("No text detected"), false))
        }

        // Sort by vertical position
        val sortedCells = cells.sortedWith(compareBy({ it.top }, { it.left }))

        // Estimate average line height
        val lineHeight = calculateAverageLineHeight(cells)
        val rowTolerance = (lineHeight * 0.7).coerceAtLeast(15.0)

        val tableRows = mutableListOf<TableRow>()
        val currentRow = mutableListOf<TableCell>()

        var lastTop = sortedCells.first().top

        // Group cells into rows
        for (cell in sortedCells) {
            if (cell.top - lastTop > rowTolerance && currentRow.isNotEmpty()) {
                // Process current row
                tableRows.add(rowFromCells(currentRow))
                currentRow.clear()
            }
            currentRow.add(cell)
            lastTop = cell.top
        }

        // Add last row
        if (currentRow.isNotEmpty()) {
            tableRows.add(rowFromCells(currentRow))
        }

        return tableRows
    }

    /**
     * Converts a list of TableCell into a TableRow
     * Merges horizontally close elements into columns
     */
    private fun rowFromCells(cells: List<TableCell>): TableRow {
        if (cells.isEmpty()) return TableRow(emptyList(), false)

        val sorted = cells.sortedBy { it.left }
        val columns = mutableListOf<MutableList<TableCell>>()
        val horizontalGapThreshold = 40 // adjust based on invoice spacing

        for (cell in sorted) {
            var added = false
            for (col in columns) {
                // If cell is horizontally close to last element in column, append
                val last = col.last()
                if (cell.left - last.right < horizontalGapThreshold) {
                    col.add(cell)
                    added = true
                    break
                }
            }
            if (!added) {
                columns.add(mutableListOf(cell))
            }
        }

        // Convert each column list to a single string (multi-line cell)
        val columnTexts = columns.map { col ->
            col.joinToString(" ") { it.text }
        }

        // Determine if it's a table row (more than 1 column)
        val isTableRow = columnTexts.size > 1

        return TableRow(columnTexts, isTableRow)
    }


    /**
     * Calculates average line height for better row grouping
     */
    private fun calculateAverageLineHeight(cells: List<TableCell>): Int {
        if (cells.size < 2) return 40 // Default value

        val heights = cells.map { it.bottom - it.top }
        return heights.average().toInt().coerceAtLeast(20)
    }

    /**
     * Checks if cells have consistent spacing (indicating a table structure)
     */
    private fun hasConsistentSpacing(cells: List<TableCell>): Boolean {
        if (cells.size < 2) return false

        val gaps = mutableListOf<Int>()
        for (i in 0 until cells.size - 1) {
            gaps.add(cells[i + 1].left - cells[i].right)
        }

        if (gaps.isEmpty()) return false

        val avgGap = gaps.average()
        val variance = gaps.map { (it - avgGap) * (it - avgGap) }.average()

        // If variance is low, spacing is consistent
        return variance < 2000
    }

    /**
     * Groups cells into columns based on horizontal position
     */
    private fun groupIntoColumns(cells: List<TableCell>): List<String> {
        if (cells.isEmpty()) return emptyList()

        val columns = mutableListOf<String>()
        val columnThreshold = 30 // Minimum horizontal gap to consider new column

        var currentColumn = mutableListOf<String>()
        var lastRight = cells.first().left

        cells.forEach { cell ->
            val gap = cell.left - lastRight

            if (gap > columnThreshold && currentColumn.isNotEmpty()) {
                // Start new column
                columns.add(currentColumn.joinToString(" "))
                currentColumn = mutableListOf()
            }

            currentColumn.add(cell.text)
            lastRight = cell.right
        }

        // Add the last column
        if (currentColumn.isNotEmpty()) {
            columns.add(currentColumn.joinToString(" "))
        }

        return columns
    }
}