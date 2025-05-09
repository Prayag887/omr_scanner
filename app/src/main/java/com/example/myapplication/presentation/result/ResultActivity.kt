package com.example.myapplication.presentation.result

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.myapplication.R
import com.facebook.shimmer.ShimmerFrameLayout
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class ResultActivity : AppCompatActivity() {
    private external fun processOMR(matAddrInput: Long): IntArray
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val shimmerLayout = findViewById<ShimmerFrameLayout>(R.id.shimmerLayout)
        val resultsContainer = findViewById<LinearLayout>(R.id.resultsContainer)
        val btnBack = findViewById<Button>(R.id.btnBack)

        shimmerLayout.startShimmer()
        val imagePath = intent.getStringExtra("image_path")
        if (imagePath != null) {
            Thread {
                val paperBitmap = BitmapFactory.decodeFile(imagePath)

                if (paperBitmap != null) {
                    val mat = Mat()
                    Utils.bitmapToMat(paperBitmap, mat)

                    val processedMat = if (mat.channels() == 4) {
                        Mat().also { Imgproc.cvtColor(mat, it, Imgproc.COLOR_RGBA2RGB) }
                    } else {
                        mat.clone()
                    }

                    val resultArray = processOMR(processedMat.nativeObjAddr)

                    runOnUiThread {
                        shimmerLayout.stopShimmer()
                        shimmerLayout.visibility = View.GONE
                        resultsContainer.visibility = View.VISIBLE

                        resultArray.forEachIndexed { index, answer ->
                            val resultText = "Q${index + 1}: ${if (answer == -1) "Unmarked" else "Option $answer"}"

                            val card = CardView(this).apply {
                                radius = 16f
                                setCardBackgroundColor(Color.WHITE)
                                useCompatPadding = true
                                val textView = TextView(context).apply {
                                    text = resultText
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

                        // Cleanup
                        mat.release()
                        processedMat.release()
                    }
                } else {
                    runOnUiThread {
                        shimmerLayout.stopShimmer()
                        shimmerLayout.visibility = View.GONE
                        resultsContainer.visibility = View.VISIBLE

                        val errorView = TextView(this).apply {
                            text = "Failed to load image."
                            textSize = 18f
                            setTextColor(Color.RED)
                            setPadding(16, 16, 16, 16)
                        }
                        resultsContainer.addView(errorView)
                    }
                }
            }.start()
        } else {
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            Toast.makeText(this, "Image path not found", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener { finish() }
    }
}