package com.example.signbuddy.ml

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage

class HandSignAnalyzer(
    private val modelInterpreter: Interpreter?,
    private val imageProcessor: ImageProcessor,
    private val inputSize: Int,
    private val useFrontCamera: Boolean,
    private val context: Context,
    private val onPrediction: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Confidence threshold (adjust as needed)
    private val confidenceThreshold = 0.75f

    // Use a single coroutine scope per analyzer to avoid spawning too many
    private val analysisScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun analyze(image: ImageProxy) {
        if (modelInterpreter == null) {
            Log.w("ASL", "❌ Model not loaded yet.")
            image.close()
            return
        }

        val rotationDegrees = image.imageInfo.rotationDegrees

        analysisScope.launch {
            try {
                val bitmap = ASLModelHelper.imageProxyToBitmap(image, rotationDegrees, useFrontCamera)
                val tensorImage = TensorImage.fromBitmap(bitmap)
                val processedImage = imageProcessor.process(tensorImage)

                val inputBuffer = processedImage.buffer
                val output = Array(1) { FloatArray(26) } // 26 possible letters (A–Z)

                modelInterpreter.run(inputBuffer, output)

                val confidences = output[0]
                val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: -1
                val maxConfidence = if (maxIndex != -1) confidences[maxIndex] else 0f

                val prediction = if (maxConfidence >= confidenceThreshold && maxIndex in 0..25) {
                    val letter = ('A' + maxIndex).toString()
                    Log.d("ASL", "✅ Prediction: $letter (${(maxConfidence * 100).toInt()}%)")
                    letter
                } else {
                    Log.d("ASL", "⚠️ Low confidence (${(maxConfidence * 100).toInt()}%), skipping")
                    ""
                }

                withContext(Dispatchers.Main) {
                    onPrediction(prediction)
                }

            } catch (e: Exception) {
                Log.e("ASL", "🔥 Error during analysis: ${e.message}", e)
            } finally {
                image.close()
            }
        }
    }
}
