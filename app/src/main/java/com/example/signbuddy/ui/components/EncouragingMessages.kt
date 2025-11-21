package com.example.signbuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// 🌟 Encouraging Messages for Kindergarteners
object EncouragingMessages {
    val correctMessages = listOf(
        "🌟 Amazing! You're a star!",
        "🎉 Fantastic work!",
        "✨ You're doing great!",
        "🏆 Super job!",
        "🎈 Wonderful! Keep it up!",
        "⭐ Excellent! You're learning fast!",
        "🎊 Brilliant! You're so smart!",
        "🌈 Outstanding! You're awesome!",
        "🎯 Perfect! You nailed it!",
        "🚀 Incredible! You're flying high!"
    )
    
    val wrongMessages = listOf(
        "💪 Don't give up! Try again!",
        "🌟 You're learning! Keep trying!",
        "🎈 That's okay! Practice makes perfect!",
        "✨ You're getting better! Try once more!",
        "🏆 Almost there! You can do it!",
        "⭐ Great effort! Let's try again!",
        "🎉 You're doing great! One more time!",
        "🌈 Keep practicing! You're improving!",
        "🎯 Nice try! You're getting closer!",
        "🚀 Don't worry! Learning takes time!"
    )
    
    val achievementMessages = listOf(
        "🎊 Congratulations! New achievement unlocked!",
        "🏆 Wow! You earned a new badge!",
        "⭐ Fantastic! You're leveling up!",
        "🌟 Amazing progress! Keep going!",
        "🎈 You're becoming a sign language expert!",
        "✨ Incredible! You're so talented!",
        "🌈 Outstanding! You're a superstar!",
        "🎯 Perfect! You're mastering this!",
        "🚀 Awesome! You're flying through levels!",
        "💫 Brilliant! You're unstoppable!"
    )
    
    fun getRandomCorrectMessage(): String = correctMessages.random()
    fun getRandomWrongMessage(): String = wrongMessages.random()
    fun getRandomAchievementMessage(): String = achievementMessages.random()
}

// 💬 Animated Message Display
@Composable
fun EncouragingMessage(
    message: String,
    isSuccess: Boolean,
    onAnimationComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "messageScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "messageAlpha"
    )

    LaunchedEffect(Unit) {
        isVisible = true
        delay(2000) // Show for 2 seconds
        onAnimationComplete()
    }

    Card(
        modifier = modifier
            .scale(scale)
            .alpha(alpha),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) 
                Color(0xFF4CAF50).copy(alpha = 0.9f) 
            else 
                Color(0xFFFF9800).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// 🎯 Floating Message Overlay
@Composable
fun FloatingMessage(
    message: String,
    isVisible: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var animationProgress by remember { mutableStateOf(0f) }
    val offsetY by animateFloatAsState(
        targetValue = -100f,
        animationSpec = tween(2000),
        label = "floatOffset"
    )
    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(2000),
        label = "floatAlpha"
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            animationProgress = 1f
            delay(2000)
            onComplete()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .offset(y = offsetY.dp)
                .alpha(alpha),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2196F3).copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(20.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}





