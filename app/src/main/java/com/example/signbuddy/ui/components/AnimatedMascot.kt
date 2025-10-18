package com.example.signbuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// 🐾 Animated Mascot for Kindergarteners
@Composable
fun AnimatedMascot(
    isHappy: Boolean = true,
    isCelebrating: Boolean = false,
    size: Int = 80,
    modifier: Modifier = Modifier
) {
    val bounce by animateFloatAsState(
        targetValue = if (isCelebrating) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mascotBounce"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isCelebrating) 10f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotRotation"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .scale(bounce),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isHappy) 
                    Color(0xFFFFD54F) // Yellow
                else 
                    Color(0xFFFFAB91) // Light orange
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isHappy) Icons.Default.EmojiEmotions else Icons.Default.Face,
                    contentDescription = "Mascot",
                    tint = if (isHappy) Color(0xFFFF6F00) else Color(0xFF757575),
                    modifier = Modifier.size((size * 0.6).dp)
                )
            }
        }
    }
}

// 🎉 Celebrating Mascot with Particle Effects
@Composable
fun CelebratingMascot(
    onAnimationComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "celebratingScale"
    )

    LaunchedEffect(Unit) {
        delay(3000) // Celebrate for 3 seconds
        isVisible = false
        delay(500)
        onAnimationComplete()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedMascot(
            isHappy = true,
            isCelebrating = true,
            size = 100,
            modifier = Modifier.scale(scale)
        )
    }
}

// 🎯 Mascot with Mood States
@Composable
fun MoodMascot(
    mood: MascotMood = MascotMood.HAPPY,
    size: Int = 60,
    modifier: Modifier = Modifier
) {
    val (isHappy, color, icon) = when (mood) {
        MascotMood.HAPPY -> Triple(true, Color(0xFFFFD54F), Icons.Default.EmojiEmotions)
        MascotMood.SAD -> Triple(false, Color(0xFFFFAB91), Icons.Default.Face)
        MascotMood.CELEBRATING -> Triple(true, Color(0xFF4CAF50), Icons.Default.EmojiEmotions)
        MascotMood.THINKING -> Triple(false, Color(0xFF81C784), Icons.Default.Face)
    }

    Card(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Mood Mascot",
                tint = Color.White,
                modifier = Modifier.size((size * 0.7).dp)
            )
        }
    }
}

enum class MascotMood {
    HAPPY, SAD, CELEBRATING, THINKING
}





