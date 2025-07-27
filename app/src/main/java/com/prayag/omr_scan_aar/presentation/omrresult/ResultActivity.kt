package com.prayag.omr_scan_aar.presentation.omrresult

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.facebook.shimmer.ShimmerFrameLayout
import com.prayag.omr_scan_aar.R
import com.prayag.omr_scan_aar.data.omrresult.repository.OMRRepositoryImpl

class ResultActivity : AppCompatActivity() {

    private lateinit var viewModel: ResultViewModel
    private val questionAnswers = mutableMapOf<Int, Int>() // Store question number and selected answer index

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val shimmerLayout = findViewById<ShimmerFrameLayout>(R.id.shimmerLayout)
        val resultsContainer = findViewById<LinearLayout>(R.id.resultsContainer)
        val btnBack = findViewById<Button>(R.id.btnBack)

        shimmerLayout.startShimmer()

        // Inject dependencies manually
        val repository = OMRRepositoryImpl()
        viewModel = ResultViewModel(repository)

        viewModel.results.observe(this) { results ->
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            resultsContainer.visibility = View.VISIBLE

            // Clear previous answers
            questionAnswers.clear()

            results.forEach { result ->
                // Store the question number and answer for SharedPreferences
                questionAnswers[result.questionNumber] = result.answer
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
                llQuestionBackground.setBackgroundColor(Color.WHITE)

                // Reset all bubbles
                listOf(bubbleA, bubbleB, bubbleC, bubbleD).forEach { bubble ->
                    bubble.background = getDrawable(R.drawable.bubble_default)
                }

                // Highlight selected answer
                when (result.answer) {
                    0 -> bubbleA.background = getDrawable(R.drawable.bubble_selected)
                    1 -> bubbleB.background = getDrawable(R.drawable.bubble_selected)
                    2 -> bubbleC.background = getDrawable(R.drawable.bubble_selected)
                    3 -> bubbleD.background = getDrawable(R.drawable.bubble_selected)
                    -1 -> {
                        // Optional: Handle unmarked questions
                        // Example: bubbleA.background = getDrawable(R.drawable.bubble_error)
                    }
                }

                resultsContainer.addView(itemView)
            }
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

        // Save each question's selected bubble index
        questionAnswers.forEach { (questionNumber, answerIndex) ->
            val key = "question_$questionNumber"
            editor.putInt(key, answerIndex)
        }

        // Save metadata
        editor.putInt("total_questions", questionAnswers.size)

        editor.apply()
    }
}