package com.example.signbuddy.ml

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ThrottledAnalyzer(
    private val targetFps: Int,
    private val analyzer: ImageAnalysis.Analyzer
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTime = 0L
    private val frameIntervalMs = 1000L / targetFps

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalyzedTime >= frameIntervalMs) {
            lastAnalyzedTime = currentTime
            analyzer.analyze(image)
        } else {
            image.close()
        }
    }
}
