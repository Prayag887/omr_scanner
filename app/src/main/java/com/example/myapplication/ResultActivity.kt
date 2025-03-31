package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val txtResults = findViewById<TextView>(R.id.txtResults)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val answers = intent.getIntArrayExtra("omr_results")
        val resultText = answers?.mapIndexed { index, answer ->
            "Q${index + 1}: ${if (answer == -1) "Unmarked" else "Option $answer"}"
        }?.joinToString("\n")

        txtResults.text = resultText ?: "No results found."
        btnBack.setOnClickListener { finish() }
    }
}
