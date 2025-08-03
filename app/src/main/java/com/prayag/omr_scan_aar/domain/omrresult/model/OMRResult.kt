package com.prayag.omr_scan_aar.domain.omrresult.model

data class OMRResult(
    val questionNumber: Int,
    val answer: Int, // Keep for backward compatibility - represents the first/primary answer
    val answers: List<Int> = emptyList() // NEW: List of all selected answers
) {
    // Constructor for backward compatibility (single answer)
    constructor(questionNumber: Int, answer: Int) : this(
        questionNumber = questionNumber,
        answer = answer,
        answers = if (answer == -1) listOf(-1) else listOf(answer)
    )

    // Helper property to get all selections (prioritize new format)
    val allSelections: List<Int>
        get() = if (answers.isNotEmpty()) answers else listOf(answer)

    // Helper property to check if multiple selections exist
    val hasMultipleSelections: Boolean
        get() = allSelections.size > 1 && !allSelections.contains(-1)

    // Helper property to check if no selection exists
    val hasNoSelection: Boolean
        get() = allSelections.all { it == -1 }

    // Helper property to get readable selection text
    val selectionText: String
        get() {
            val validSelections = allSelections.filter { it in 0..3 }
            return when {
                validSelections.isEmpty() -> "No Answer"
                validSelections.size == 1 -> {
                    when (validSelections.first()) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> "No Answer"
                    }
                }
                else -> {
                    validSelections.joinToString(", ") { index ->
                        when (index) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            3 -> "D"
                            else -> "?"
                        }
                    } + " (Multiple)"
                }
            }
        }

    // Helper property to get selection count
    val selectionCount: Int
        get() = allSelections.filter { it in 0..3 }.size
}
