package com.prayag.omr_scan_aar.data.omrresult.repository

import android.graphics.BitmapFactory
import android.util.Log
import com.prayag.omr_scan_aar.domain.omrresult.model.OMRResult
import com.prayag.omr_scan_aar.domain.omrresult.repository.OMRRepository
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File

class OMRRepositoryImpl : OMRRepository {
    external fun processOMR(matAddrInput: Long, packageName: String): IntArray

    private val TAG = "OMRRepositoryImpl"

    override fun processOMR(imagePath: String): List<OMRResult> {
        val paperBitmap = BitmapFactory.decodeFile(imagePath) ?: return emptyList()
        val mat = Mat().also { Utils.bitmapToMat(paperBitmap, it) }

        val processedMat = if (mat.channels() == 4) {
            Mat().also { Imgproc.cvtColor(mat, it, Imgproc.COLOR_RGBA2RGB) }
        } else mat.clone()

        // Extract only the package name from the path
        val originalDirPath = File(imagePath).parent ?: ""
        val packageName = extractPackageName(originalDirPath)

        Log.d(TAG, "Original path: $originalDirPath")
        Log.d(TAG, "Package name: $packageName")

        // Pass both the mat address and the package name to JNI
        val resultArray = processOMR(processedMat.nativeObjAddr, packageName)

        mat.release()
        processedMat.release()

        // UPDATED: Parse results to handle multiple selections
        return parseOMRResults(resultArray)
    }

    // UPDATED: Enhanced parsing to handle multiple selections
    private fun parseOMRResults(resultArray: IntArray): List<OMRResult> {
        Log.d(TAG, "=== PARSING DEBUG ===")
        Log.d(TAG, "Raw result array size: ${resultArray.size}")

        // Log first 50 values in chunks for better readability
        for (i in resultArray.indices step 10) {
            val chunk = resultArray.sliceArray(i until minOf(i + 10, resultArray.size))
            Log.d(TAG, "Results [${i}-${minOf(i + 9, resultArray.size - 1)}]: ${chunk.joinToString(", ")}")
        }

        // Analyze the data
        val separatorCount = resultArray.count { it == -2 }
        val negativeOnes = resultArray.count { it == -1 }
        val validAnswers = resultArray.count { it in 0..3 }

        Log.d(TAG, "Data analysis:")
        Log.d(TAG, "  Separators (-2): $separatorCount")
        Log.d(TAG, "  No answers (-1): $negativeOnes")
        Log.d(TAG, "  Valid answers (0-3): $validAnswers")

        return if (containsSeparators(resultArray)) {
            // New format with separators for multiple selections
            parseResultsWithSeparators(resultArray)
        } else {
            // Original format - simple one-to-one mapping
            parseResultsOriginalFormat(resultArray)
        }
    }

    // Check if the result array contains separator values (-2)
    private fun containsSeparators(resultArray: IntArray): Boolean {
        return resultArray.contains(-2)
    }

    // Parse results with separator-based encoding (-2 separates questions)
    private fun parseResultsWithSeparators(resultArray: IntArray): List<OMRResult> {
        val omrResults = mutableListOf<OMRResult>()
        var questionNumber = 1
        var i = 0

        Log.d(TAG, "=== PARSING WITH SEPARATORS ===")

        while (i < resultArray.size) {
            val selections = mutableListOf<Int>()

            // Read selections until we hit a separator (-2) or end of array
            while (i < resultArray.size && resultArray[i] != -2) {
                selections.add(resultArray[i])
                i++
            }

            // Skip the separator
            if (i < resultArray.size && resultArray[i] == -2) {
                i++
            }

            // IMPROVED: Filter out invalid selections and handle edge cases
            val validSelections = selections.filter { it in -1..3 } // Allow -1 (no answer) and 0-3 (valid options)
            val finalAnswers = when {
                validSelections.isEmpty() -> listOf(-1) // No valid selections found
                validSelections.contains(-1) && validSelections.size == 1 -> listOf(-1) // Only -1 (no answer)
                validSelections.contains(-1) && validSelections.size > 1 -> {
                    // Mixed case: has -1 and other selections, keep only valid answers
                    val onlyValidAnswers = validSelections.filter { it in 0..3 }
                    if (onlyValidAnswers.isNotEmpty()) onlyValidAnswers else listOf(-1)
                }
                else -> validSelections // All selections are valid (0-3)
            }

            omrResults.add(
                OMRResult(
                    questionNumber = questionNumber,
                    answer = finalAnswers.first(), // First answer for backward compatibility
                    answers = finalAnswers // All answers
                )
            )

            // Enhanced logging
            val selectionText = when {
                finalAnswers.size == 1 && finalAnswers.first() == -1 -> "No Answer"
                finalAnswers.size == 1 -> {
                    val option = when(finalAnswers.first()) {
                        0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"
                        else -> "Invalid"
                    }
                    "Single: $option"
                }
                else -> {
                    val options = finalAnswers.map {
                        when(it) {
                            0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"
                            else -> "?"
                        }
                    }.joinToString(", ")
                    "Multiple: $options"
                }
            }

            Log.d(TAG, "Q$questionNumber: [${finalAnswers.joinToString(", ")}] -> $selectionText")
            questionNumber++

            // Safety check to prevent infinite loops
            if (questionNumber > 250) { // Allow up to 250 questions as safety
                Log.w(TAG, "Reached safety limit of 250 questions, stopping parse")
                break
            }
        }

        Log.d(TAG, "=== SEPARATOR PARSING COMPLETE ===")
        Log.d(TAG, "Parsed ${omrResults.size} questions")

        // Summary statistics
        val answeredQuestions = omrResults.count { !it.hasNoSelection }
        val multipleSelections = omrResults.count { it.hasMultipleSelections }

        Log.d(TAG, "Summary:")
        Log.d(TAG, "  Total questions: ${omrResults.size}")
        Log.d(TAG, "  Answered questions: $answeredQuestions")
        Log.d(TAG, "  Questions with multiple selections: $multipleSelections")
        Log.d(TAG, "  Unanswered questions: ${omrResults.size - answeredQuestions}")

        return omrResults
    }

    // Parse results in original format (one result per question) - SIMPLIFIED
    private fun parseResultsOriginalFormat(resultArray: IntArray): List<OMRResult> {
        Log.d(TAG, "=== PARSING ORIGINAL FORMAT ===")
        Log.d(TAG, "Using simple one-to-one mapping")

        return resultArray.mapIndexed { index, answer ->
            val questionNumber = index + 1
            OMRResult(
                questionNumber = questionNumber,
                answer = answer,
                answers = if (answer == -1) listOf(-1) else listOf(answer)
            )
        }
    }

    private fun extractPackageName(path: String): String {
        // Pattern 1: /data/user/0/com.package.name/...
        val userDataPattern = "/data/user/\\d+/([\\w.]+).*".toRegex()
        userDataPattern.find(path)?.let {
            return it.groupValues[1]
        }

        // Pattern 2: /data/data/com.package.name/...
        val dataDataPattern = "/data/data/([\\w.]+).*".toRegex()
        dataDataPattern.find(path)?.let {
            return it.groupValues[1]
        }

        // Pattern 4: .../com.package.name/files/...
        val generalPattern = ".*/([\\w.]+\\.\\w+)/files.*".toRegex()
        generalPattern.find(path)?.let {
            return it.groupValues[1]
        }

        // If we can't determine the package name, log warning and return empty string
        Log.w(TAG, "Could not extract package name from path: $path")
        return ""
    }
}