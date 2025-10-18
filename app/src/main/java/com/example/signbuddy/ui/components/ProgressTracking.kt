package com.example.signbuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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

// 📈 Progress Tracking System
data class ProgressData(
    val totalSigns: Int = 0,
    val correctSigns: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val accuracy: Float = 0f,
    val timeSpent: Long = 0L, // in milliseconds
    val level: String = "Beginner",
    val xp: Int = 0,
    val nextLevelXp: Int = 100
)

object ProgressManager {
    fun calculateLevel(xp: Int): String {
        return when {
            xp < 100 -> "Beginner"
            xp < 300 -> "Explorer"
            xp < 600 -> "Adventurer"
            xp < 1000 -> "Expert"
            xp < 1500 -> "Master"
            else -> "Legend"
        }
    }
    
    fun getNextLevelXp(currentXp: Int): Int {
        return when {
            currentXp < 100 -> 100
            currentXp < 300 -> 300
            currentXp < 600 -> 600
            currentXp < 1000 -> 1000
            currentXp < 1500 -> 1500
            else -> currentXp + 500
        }
    }
    
    fun getLevelColor(level: String): Color {
        return when (level) {
            "Beginner" -> Color(0xFF4CAF50)
            "Explorer" -> Color(0xFF2196F3)
            "Adventurer" -> Color(0xFF9C27B0)
            "Expert" -> Color(0xFFFF9800)
            "Master" -> Color(0xFFF44336)
            "Legend" -> Color(0xFFFFD700)
            else -> Color(0xFF757575)
        }
    }
}

// 🎯 Level Progress Card
@Composable
fun LevelProgressCard(
    progressData: ProgressData,
    modifier: Modifier = Modifier
) {
    val levelColor = ProgressManager.getLevelColor(progressData.level)
    val levelProgress = (progressData.xp.toFloat() / progressData.nextLevelXp).coerceIn(0f, 1f)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = levelColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Level ${progressData.level}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                    Text(
                        text = "${progressData.xp} XP",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                // Level icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(levelColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Level",
                        tint = levelColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // XP Progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress to next level",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "${progressData.xp}/${progressData.nextLevelXp}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = levelProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = levelColor,
                    trackColor = Color(0xFFE0E0E0)
                )
            }
        }
    }
}

// 📊 Stats Overview Card
@Composable
fun StatsOverviewCard(
    progressData: ProgressData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Your Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "${progressData.correctSigns}",
                    label = "Correct",
                    color = Color(0xFF4CAF50)
                )
                
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    value = "${progressData.bestStreak}",
                    label = "Best Streak",
                    color = Color(0xFFFF9800)
                )
                
                StatItem(
                    icon = Icons.Default.Timer,
                    value = "${(progressData.timeSpent / 60000)}m",
                    label = "Time Spent",
                    color = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accuracy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = "${(progressData.accuracy * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progressData.accuracy,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

// 🎉 Level Up Animation
@Composable
fun LevelUpAnimation(
    newLevel: String,
    onComplete: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "levelUpScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(1000),
        label = "levelUpAlpha"
    )

    LaunchedEffect(Unit) {
        delay(3000) // Show for 3 seconds
        isVisible = false
        delay(500)
        onComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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
                Text(
                    text = "🎉 Level Up!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Level Up",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "You are now a",
                    fontSize = 16.sp,
                    color = Color(0xFF666666)
                )
                
                Text(
                    text = newLevel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProgressManager.getLevelColor(newLevel)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Keep up the great work!",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}





