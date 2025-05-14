package com.prayag.omr_scan_aar.presentation.omrresult

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.prayag.omr_scan_aar.R
import com.prayag.omr_scan_aar.data.omrresult.repository.OMRRepositoryImpl
import com.facebook.shimmer.ShimmerFrameLayout

class ResultActivity : AppCompatActivity() {

    private lateinit var viewModel: ResultViewModel

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

            results.forEach { result ->
                val card = CardView(this).apply {
                    radius = 16f
                    setCardBackgroundColor(Color.WHITE)
                    useCompatPadding = true
                    val textView = TextView(context).apply {
                        text = "Q${result.questionNumber}: ${if (result.answer == -1) "Unmarked" else "Option ${result.answer}"}"
                        textSize = 16f
                        setPadding(20, 20, 20, 20)
                        setTextColor(Color.BLACK)
                    }
                    addView(textView)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 16
                    }
                }
                resultsContainer.addView(card)
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
        btnBack.setOnClickListener { finish() }
    }
}
