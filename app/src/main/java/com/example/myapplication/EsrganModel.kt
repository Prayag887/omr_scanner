package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ESRGANModel(private val context: Context) {
    private var interpreter: Interpreter

    init {
        val modelFile = loadModelFile(context, "esrgan.tflite")
        interpreter = Interpreter(modelFile)
    }

    fun enhanceImage(inputBitmap: Bitmap): Bitmap {
        val resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, 512, 512, true)

        val input = preprocessBitmap(resizedBitmap)

        val output = Array(1) { Array(512) { FloatArray(512 * 3) } } // Adjust for model output (512x512x3)

        // Run the model inference
        interpreter.run(input, output)

        // Postprocess the output to create a bitmap
        val postProcessedBitmap = postprocessBitmap(output)

        if (postProcessedBitmap.width == 0 || postProcessedBitmap.height == 0) {
            Log.e("DEBUG", "postProcessedBitmap is invalid!")
            return resizedBitmap
        }

        // Save Bitmap as PNG
        val enhancedFile = File(context.filesDir, "a_enhanced.png")
        try {
            val outputStream = enhancedFile.outputStream()
            postProcessedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Log.d("DEBUG", "Image saved successfully at: ${enhancedFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("DEBUG", "Failed to save image!", e)
        }

        return postProcessedBitmap
    }



    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        try {
            val assetFileDescriptor = context.assets.openFd(modelName)
            val inputStream = assetFileDescriptor.createInputStream()
            val byteArray = inputStream.readBytes()

            Log.d("TFLITE", "Model file $modelName loaded successfully. Size: ${byteArray.size} bytes")

            return ByteBuffer.allocateDirect(byteArray.size).apply {
                order(ByteOrder.nativeOrder())
                put(byteArray)
            }
        } catch (e: Exception) {
            Log.e("TFLITE", "Failed to load model file: $modelName", e)
            throw RuntimeException("Model file not found: $modelName")
        }
    }


    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(4 * 512 * 512 * 3) // 3 channels (RGB)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(512 * 512)
        bitmap.getPixels(pixels, 0, 512, 0, 0, 512, 512)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        return inputBuffer
    }

    private fun postprocessBitmap(output: Array<Array<FloatArray>>): Bitmap {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)

        for (y in 0 until 512) {
            for (x in 0 until 512) {
                val r = (output[0][y][x] * 255).toInt().coerceIn(0, 255)
                val g = (output[0][y][x] * 255).toInt().coerceIn(0, 255)
                val b = (output[0][y][x] * 255).toInt().coerceIn(0, 255)
                bitmap.setPixel(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }

        return bitmap
    }
}
