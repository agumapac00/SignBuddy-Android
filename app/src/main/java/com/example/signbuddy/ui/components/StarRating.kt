package com.example.signbuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ⭐ Star Rating System for Gamification
@Composable
fun StarRating(
    rating: Int,
    maxRating: Int = 5,
    size: Int = 24,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxRating) { index ->
            val isFilled = index < rating
            val starColor = when {
                isFilled -> Color(0xFFFFD700) // Gold
                else -> Color(0xFFE0E0E0) // Light gray
            }
            
            if (animated && isFilled) {
                AnimatedStar(
                    color = starColor,
                    size = size.dp,
                    delay = index * 100L
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star ${index + 1}",
                    tint = starColor,
                    modifier = Modifier.size(size.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedStar(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    delay: Long = 0L
) {
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "starScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "starAlpha"
    )

    LaunchedEffect(Unit) {
        delay(delay)
        isVisible = true
    }

    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = "Animated Star",
        tint = color,
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
    )
}

// 🏆 Progress Indicator with Stars
@Composable
fun ProgressWithStars(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
    val starsEarned = (progress * 5).toInt().coerceIn(0, 5)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Progress: $current/$total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        StarRating(
            rating = starsEarned,
            maxRating = 5,
            size = 20,
            animated = true
        )
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline
        )
    }
}





