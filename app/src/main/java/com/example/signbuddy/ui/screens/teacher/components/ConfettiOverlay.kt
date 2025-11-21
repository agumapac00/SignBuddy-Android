package com.example.signbuddy.ui.screens.teacher.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ConfettiOverlay(visible: Boolean, onFinished: () -> Unit = {}) {
    AnimatedVisibility(visible = visible) {
        var particles by remember { mutableStateOf(generateParticles()) }
        LaunchedEffect(visible) {
            if (visible) {
                val durationMs = 1500
                val start = System.currentTimeMillis()
                while (System.currentTimeMillis() - start < durationMs) {
                    particles = particles.map { it.step() }
                    delay(16)
                }
                onFinished()
            }
        }
        Box(Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas {
                    particles.forEach { p ->
                        drawCircle(color = p.color.copy(alpha = p.alpha), radius = p.size, center = Offset(p.x, p.y))
                    }
                }
            }
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val alpha: Float
) {
    fun step(): Particle {
        val nx = x + vx
        val ny = y + vy
        val na = (alpha - 0.01f).coerceAtLeast(0f)
        return copy(x = nx, y = ny, vy = vy + 0.4f, alpha = na)
    }
}

private fun generateParticles(count: Int = 80): List<Particle> {
    val colors = listOf(
        Color(0xFFFFCDD2), // pastel red
        Color(0xFFF8BBD0), // pastel pink
        Color(0xFFE1BEE7), // pastel purple
        Color(0xFFC5CAE9), // pastel indigo
        Color(0xFFB3E5FC), // pastel blue
        Color(0xFFC8E6C9), // pastel green
        Color(0xFFFFF9C4), // pastel yellow
        Color(0xFFFFE0B2)  // pastel orange
    )
    return List(count) {
        val r = Random(it * 37)
        Particle(
            x = r.nextFloat() * 1080f,
            y = -r.nextFloat() * 400f,
            vx = (r.nextFloat() - 0.5f) * 6f,
            vy = r.nextFloat() * 6f + 2f,
            size = r.nextFloat() * 10f + 6f,
            color = colors[r.nextInt(colors.size)],
            alpha = 1f
        )
    }
}







