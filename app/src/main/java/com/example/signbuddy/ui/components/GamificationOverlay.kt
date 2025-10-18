package com.example.signbuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// 🎮 Comprehensive Gamification Overlay
@Composable
fun GamificationOverlay(
    isVisible: Boolean,
    type: GamificationType,
    message: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var animationPhase by remember { mutableStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = when (animationPhase) {
            0 -> 0.5f
            1 -> 1.1f
            2 -> 1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "overlayScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (animationPhase >= 0) 1f else 0f,
        animationSpec = tween(500),
        label = "overlayAlpha"
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            animationPhase = 1
            delay(200)
            animationPhase = 2
            delay(2500)
            onComplete()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        
        // Main content
        Card(
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .padding(32.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon based on type
                when (type) {
                    GamificationType.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    GamificationType.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    GamificationType.ACHIEVEMENT -> {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Achievement",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    GamificationType.LEVEL_UP -> {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Level Up",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    GamificationType.STREAK -> {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle based on type
                val subtitle = when (type) {
                    GamificationType.SUCCESS -> "Great job! Keep it up!"
                    GamificationType.ERROR -> "Don't give up! Try again!"
                    GamificationType.ACHIEVEMENT -> "You earned a new badge!"
                    GamificationType.LEVEL_UP -> "You're getting better!"
                    GamificationType.STREAK -> "You're on fire!"
                }
                
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

enum class GamificationType {
    SUCCESS, ERROR, ACHIEVEMENT, LEVEL_UP, STREAK
}

// 🎊 Particle Effect Component
@Composable
fun ParticleEffect(
    isActive: Boolean,
    particleCount: Int = 20,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    Box(modifier = modifier.fillMaxSize()) {
        repeat(particleCount) { index ->
            var isVisible by remember { mutableStateOf(false) }
            val offsetX by animateFloatAsState(
                targetValue = if (isVisible) (0..1000).random().toFloat() else 0f,
                animationSpec = tween(2000),
                label = "particleX$index"
            )
            val offsetY by animateFloatAsState(
                targetValue = if (isVisible) (0..1000).random().toFloat() else 0f,
                animationSpec = tween(2000),
                label = "particleY$index"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isVisible) 0f else 1f,
                animationSpec = tween(2000),
                label = "particleAlpha$index"
            )

            LaunchedEffect(Unit) {
                delay(index * 50L)
                isVisible = true
            }

            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (index % 4) {
                            0 -> Color(0xFFFFD700)
                            1 -> Color(0xFFFF6B35)
                            2 -> Color(0xFF4CAF50)
                            else -> Color(0xFF2196F3)
                        }.copy(alpha = alpha)
                    )
            )
        }
    }
}

// 🎯 Floating Action Button with Gamification
@Composable
fun GamifiedFAB(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100),
        label = "fabScale"
    )

    FloatingActionButton(
        onClick = {
            if (isEnabled) {
                onClick()
            }
        },
        modifier = modifier.scale(scale),
        containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isEnabled) Color.White else Color(0xFF757575)
        )
    }
}

// 🎨 Themed Progress Indicator
@Composable
fun ThemedProgressIndicator(
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress"
    )

    LinearProgressIndicator(
        progress = animatedProgress,
        modifier = modifier.fillMaxWidth(),
        color = color,
        trackColor = color.copy(alpha = 0.2f)
    )
}

// 🏅 Badge Component
@Composable
fun Badge(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}





