package com.prayag.omr_scan_aar.presentation.omrresult

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.prayag.omr_scan_aar.R
import org.koin.androidx.viewmodel.ext.android.viewModel

class ResultActivity : AppCompatActivity() {

    private val viewModel: ResultViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Make root container white
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        rootLayout.setBackgroundColor(Color.WHITE)

        val ocrTextContainer = findViewById<LinearLayout>(R.id.ocrTextContainer)
        ocrTextContainer.setBackgroundColor(Color.WHITE) // white scroll area
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Observe structured OCR data
        viewModel.ocrTableData.observe(this) { tableRows ->
            ocrTextContainer.removeAllViews()

            if (tableRows.isNullOrEmpty()) {
                showEmptyState(ocrTextContainer)
                return@observe
            }

            tableRows.forEachIndexed { index, row ->
                if (row.isTableRow && row.cells.size > 1) {
                    addTableRow(ocrTextContainer, row.cells, index == 0)
                } else {
                    addRegularText(ocrTextContainer, row.cells.firstOrNull() ?: "")
                }
            }
        }

        // Observe errors
        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                ocrTextContainer.removeAllViews()
                val tv = TextView(this).apply {
                    text = errorMsg
                    textSize = 16f
                    setTextColor(Color.RED)
                    setPadding(16, 8, 16, 8)
                    setBackgroundColor(Color.WHITE) // error background white
                }
                ocrTextContainer.addView(tv)
            }
        }

        val imagePath = intent.getStringExtra("image_path")
        viewModel.processOCR(imagePath)

        btnBack.setOnClickListener { finish() }
    }

    private fun addTableRow(container: LinearLayout, cells: List<String>, isHeader: Boolean) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 4, 0, 4)
            setBackgroundColor(Color.WHITE) // row white
        }

        cells.forEach { cellText ->
            val cellView = TextView(this).apply {
                text = cellText
                textSize = if (isHeader) 14f else 13f
                setTextColor(Color.BLACK)
                setPadding(12, 8, 12, 8)

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )

                if (isHeader) setTypeface(null, Typeface.BOLD)

                // White inside, black border
                setBackgroundResource(R.drawable.cell_border)
            }
            rowLayout.addView(cellView)
        }

        container.addView(rowLayout)
        addDivider(container)
    }

    private fun addRegularText(container: LinearLayout, text: String) {
        if (text.isBlank()) return

        val tv = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.WHITE)
        }
        container.addView(tv)
    }

    private fun addDivider(container: LinearLayout) {
        val divider = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
            setBackgroundColor(Color.WHITE) // fully white divider
        }
        container.addView(divider)
    }

    private fun showEmptyState(container: LinearLayout) {
        val tv = TextView(this).apply {
            text = "No text detected in the image"
            textSize = 16f
            setTextColor(Color.BLACK) // text stays black
            gravity = Gravity.CENTER
            setPadding(16, 32, 16, 32)
            setBackgroundColor(Color.WHITE)
        }
        container.addView(tv)
    }
}
