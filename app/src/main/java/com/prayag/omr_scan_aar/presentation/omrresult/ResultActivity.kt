package com.prayag.omr_scan_aar.presentation.omrresult

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.facebook.shimmer.ShimmerFrameLayout
import com.prayag.omr_scan_aar.R
import com.prayag.omr_scan_aar.data.omrresult.repository.OMRRepositoryImpl
import com.prayag.omr_scan_aar.utils.SessionManager

class ResultActivity : AppCompatActivity() {

    private lateinit var viewModel: ResultViewModel
    // UPDATED: Store multiple answers per question
    private val questionAnswers = mutableMapOf<Int, List<Int>>() // Store question number and list of selected answer indices
    private var rollNumber: String? = null

    companion object {
        private const val TAG = "ResultActivity"

        fun getMultipleAnswersFromSharedPreferences(context: Context, questionNumber: Int): List<Int> {
            val sharedPreferences = context.getSharedPreferences("omr_result", Context.MODE_PRIVATE)
            val count = sharedPreferences.getInt("question_${questionNumber}_count", 0)

            return if (count > 0) {
                (0 until count).map { index ->
                    sharedPreferences.getInt("question_${questionNumber}_answer_$index", -1)
                }
            } else {
                // Fallback to old format
                listOf(sharedPreferences.getInt("question_$questionNumber", -1))
            }
        }

        fun getAllMultipleAnswersFromSharedPreferences(context: Context): Map<Int, List<Int>> {
            val sharedPreferences = context.getSharedPreferences("omr_result", Context.MODE_PRIVATE)
            val totalQuestions = sharedPreferences.getInt("total_questions", 0)
            val results = mutableMapOf<Int, List<Int>>()

            for (questionNumber in 1..totalQuestions) {
                results[questionNumber] = getMultipleAnswersFromSharedPreferences(context, questionNumber)
            }

            return results
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val shimmerLayout = findViewById<ShimmerFrameLayout>(R.id.shimmerLayout)
        val resultsContainer = findViewById<LinearLayout>(R.id.resultsContainer)
        val btnBack = findViewById<Button>(R.id.btnBack)

        shimmerLayout.startShimmer()

        // Get roll number from multiple sources
        rollNumber = intent.getStringExtra("ROLL_NUMBER") ?: SessionManager.getRollNumber(this)

        rollNumber?.let { rollNum ->
            Log.d(TAG, "Roll number received: $rollNum")
        } ?: run {
            Log.w(TAG, "No roll number available")
        }

        // Inject dependencies manually
        val repository = OMRRepositoryImpl()
        viewModel = ResultViewModel(repository)

        viewModel.results.observe(this) { results ->
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            resultsContainer.visibility = View.VISIBLE

            Log.d(TAG, "Received ${results.size} results from ViewModel")

            // Clear previous answers
            questionAnswers.clear()

            results.forEach { result ->
                Log.d(TAG, "Q${result.questionNumber}: ${result.allSelections.joinToString(", ")} ${if (result.hasMultipleSelections) "(MULTIPLE)" else ""}")

                // UPDATED: Store all answers for this question
                questionAnswers[result.questionNumber] = result.allSelections

                val itemView = layoutInflater.inflate(
                    R.layout.item_question,
                    resultsContainer,
                    false
                ) as CardView

                val llQuestionBackground = itemView.findViewById<LinearLayout>(R.id.llQuestionBackground)
                val tvQuestionNumber = itemView.findViewById<TextView>(R.id.tvQuestionNumber)
                val bubbleA = itemView.findViewById<TextView>(R.id.bubbleA)
                val bubbleB = itemView.findViewById<TextView>(R.id.bubbleB)
                val bubbleC = itemView.findViewById<TextView>(R.id.bubbleC)
                val bubbleD = itemView.findViewById<TextView>(R.id.bubbleD)

                tvQuestionNumber.text = "Q${result.questionNumber}"

                // UPDATED: Set background color based on selection type
                when {
                    result.hasMultipleSelections -> {
                        // Multiple selections - use warning color (light yellow)
                        llQuestionBackground.setBackgroundColor(Color.parseColor("#FFF9C4"))
                    }
                    result.hasNoSelection -> {
                        // No selection - use light red
                        llQuestionBackground.setBackgroundColor(Color.parseColor("#FFEBEE"))
                    }
                    else -> {
                        // Single selection - normal white
                        llQuestionBackground.setBackgroundColor(Color.WHITE)
                    }
                }

                // Reset all bubbles to default
                val bubbles = listOf(bubbleA, bubbleB, bubbleC, bubbleD)
                bubbles.forEach { bubble ->
                    bubble.background = getDrawable(R.drawable.bubble_default)
                }

                // UPDATED: Highlight ALL selected answers
                result.allSelections.forEach { answerIndex ->
                    when (answerIndex) {
                        0 -> bubbleA.background = getDrawable(R.drawable.bubble_selected)
                        1 -> bubbleB.background = getDrawable(R.drawable.bubble_selected)
                        2 -> bubbleC.background = getDrawable(R.drawable.bubble_selected)
                        3 -> bubbleD.background = getDrawable(R.drawable.bubble_selected)
                        -1 -> {
                            // No selection - could add special styling here if needed
                            // For example: add a "?" indicator or different background
                        }
                    }
                }

                // UPDATED: Add indicator text for multiple selections
                if (result.hasMultipleSelections) {
                    // Create a small text indicator
                    val multipleIndicator = TextView(this).apply {
                        text = "Multiple (${result.selectionCount})"
                        textSize = 10f
                        setTextColor(Color.parseColor("#FF9800")) // Orange color
                        setPadding(8, 2, 8, 2)
                        gravity = android.view.Gravity.CENTER
                    }

                    // Add the indicator to the question layout
                    llQuestionBackground.addView(multipleIndicator)
                }

                // UPDATED: Add selection summary text
                val selectionSummary = TextView(this).apply {
                    text = result.selectionText
                    textSize = 11f
                    setTextColor(when {
                        result.hasMultipleSelections -> Color.parseColor("#FF9800") // Orange
                        result.hasNoSelection -> Color.parseColor("#F44336") // Red
                        else -> Color.parseColor("#4CAF50") // Green
                    })
                    setPadding(8, 2, 8, 2)
                    gravity = android.view.Gravity.CENTER
                }

                llQuestionBackground.addView(selectionSummary)

                resultsContainer.addView(itemView)
            }

            // Log summary
            val totalQuestions = results.size
            val answeredQuestions = results.count { !it.hasNoSelection }
            val multipleSelections = results.count { it.hasMultipleSelections }

            Log.d(TAG, "=== UI DISPLAY SUMMARY ===")
            Log.d(TAG, "Total questions displayed: $totalQuestions")
            Log.d(TAG, "Answered questions: $answeredQuestions")
            Log.d(TAG, "Questions with multiple selections: $multipleSelections")
            Log.d(TAG, "Unanswered questions: ${totalQuestions - answeredQuestions}")
        }

        viewModel.error.observe(this) {
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            resultsContainer.visibility = View.VISIBLE

            val errorView = TextView(this).apply {
                text = it ?: "Unknown error"
                textSize = 18f
                setTextColor(Color.RED)
                setPadding(16, 16, 16, 16)
            }
            resultsContainer.addView(errorView)
        }

        viewModel.processOMR(intent.getStringExtra("image_path"))

        btnBack.setOnClickListener {
            saveResultsToSharedPreferences()
            finishAffinity()
        }
    }

    private fun saveResultsToSharedPreferences() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("omr_result", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Clear existing data and save new results
        editor.clear()

        // Save roll number
        rollNumber?.let { rollNum ->
            editor.putString("roll_number", rollNum)
            Log.d(TAG, "Saving roll number to SharedPreferences: $rollNum")
        }

        // UPDATED: Save multiple answers per question
        questionAnswers.forEach { (questionNumber, answerIndices) ->
            // Save the number of answers for this question
            editor.putInt("question_${questionNumber}_count", answerIndices.size)

            // Save each answer index
            answerIndices.forEachIndexed { index, answerIndex ->
                val key = "question_${questionNumber}_answer_$index"
                editor.putInt(key, answerIndex)
            }

            // BACKWARD COMPATIBILITY: Also save in old format (first answer only)
            val key = "question_$questionNumber"
            editor.putInt(key, answerIndices.firstOrNull() ?: -1)

            // Log what we're saving
            if (answerIndices.size > 1) {
                Log.d(TAG, "Saving Q$questionNumber: Multiple answers [${answerIndices.joinToString(", ")}]")
            }
        }

        // Save metadata
        editor.putInt("total_questions", questionAnswers.size)

        // Save additional metadata for multiple selections
        val multipleSelectionCount = questionAnswers.values.count { it.size > 1 }
        editor.putInt("multiple_selection_count", multipleSelectionCount)

        // Save timestamp for when results were saved
        editor.putLong("timestamp", System.currentTimeMillis())

        editor.apply()
        Log.d(TAG, "OMR results saved to SharedPreferences:")
        Log.d(TAG, "  Total questions: ${questionAnswers.size}")
        Log.d(TAG, "  Questions with multiple selections: $multipleSelectionCount")

        // Clear roll number from SessionManager after saving to OMR results
        SessionManager.clearRollNumber(this)
        Log.d(TAG, "Roll number cleared from SessionManager")
    }
}