package com.example.signbuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// 🏆 Enhanced Achievement System
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val maxProgress: Int = 100,
    val points: Int = 0
)

object AchievementManager {
    val achievements = listOf(
        Achievement(
            id = "first_sign",
            title = "First Steps",
            description = "Complete your first sign",
            icon = Icons.Default.Star,
            color = Color(0xFFFFD700),
            points = 10
        ),
        Achievement(
            id = "streak_5",
            title = "Hot Streak",
            description = "Get 5 correct signs in a row",
            icon = Icons.Default.LocalFireDepartment,
            color = Color(0xFFFF6B35),
            points = 25
        ),
        Achievement(
            id = "streak_10",
            title = "On Fire",
            description = "Get 10 correct signs in a row",
            icon = Icons.Default.Whatshot,
            color = Color(0xFFFF0000),
            points = 50
        ),
        Achievement(
            id = "perfect_score",
            title = "Perfect Score",
            description = "Get 100% accuracy in a session",
            icon = Icons.Default.EmojiEvents,
            color = Color(0xFF4CAF50),
            points = 100
        ),
        Achievement(
            id = "speed_demon",
            title = "Speed Demon",
            description = "Complete 10 signs in under 30 seconds",
            icon = Icons.Default.Speed,
            color = Color(0xFF9C27B0),
            points = 75
        ),
        Achievement(
            id = "dedicated_learner",
            title = "Dedicated Learner",
            description = "Practice for 7 days in a row",
            icon = Icons.Default.CalendarToday,
            color = Color(0xFF2196F3),
            points = 150
        ),
        Achievement(
            id = "alphabet_master",
            title = "Alphabet Master",
            description = "Learn all 26 letters",
            icon = Icons.Default.School,
            color = Color(0xFF607D8B),
            points = 200
        ),
        Achievement(
            id = "helpful_friend",
            title = "Helpful Friend",
            description = "Help 5 classmates",
            icon = Icons.Default.People,
            color = Color(0xFFFF9800),
            points = 100
        )
    )
    
    fun checkAchievements(score: Int, streak: Int, accuracy: Float, sessionTime: Long): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()
        
        achievements.forEach { achievement ->
            when (achievement.id) {
                "first_sign" -> if (score >= 1) unlocked.add(achievement.copy(isUnlocked = true))
                "streak_5" -> if (streak >= 5) unlocked.add(achievement.copy(isUnlocked = true))
                "streak_10" -> if (streak >= 10) unlocked.add(achievement.copy(isUnlocked = true))
                "perfect_score" -> if (accuracy >= 1.0f) unlocked.add(achievement.copy(isUnlocked = true))
                "speed_demon" -> if (sessionTime <= 30000) unlocked.add(achievement.copy(isUnlocked = true))
            }
        }
        
        return unlocked
    }
}

// 🎖️ Achievement Badge Component
@Composable
fun AchievementBadge(
    achievement: Achievement,
    isNew: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "achievementScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "achievementAlpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Card(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) 
                achievement.color.copy(alpha = 0.1f)
            else 
                Color(0xFFE0E0E0).copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(if (isNew) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) 
                            achievement.color.copy(alpha = 0.2f)
                        else 
                            Color(0xFFBDBDBD).copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = achievement.title,
                    tint = if (achievement.isUnlocked) achievement.color else Color(0xFF757575),
                    modifier = Modifier.size(30.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (achievement.isUnlocked) Color(0xFF2C3E50) else Color(0xFF757575)
            )
            
            Text(
                text = achievement.description,
                fontSize = 12.sp,
                color = if (achievement.isUnlocked) Color(0xFF666666) else Color(0xFFBDBDBD),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            if (achievement.isUnlocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+${achievement.points} pts",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = achievement.color
                )
            }
            
            if (isNew) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NEW!",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35)
                )
            }
        }
    }
}

// 🏆 Achievement Unlock Animation
@Composable
fun AchievementUnlockAnimation(
    achievement: Achievement,
    onComplete: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "unlockScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(1000),
        label = "unlockAlpha"
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
                    text = "🏆 Achievement Unlocked!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = achievement.title,
                    tint = achievement.color,
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = achievement.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = achievement.color
                )
                
                Text(
                    text = achievement.description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "+${achievement.points} points earned!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

// 📊 Achievement Progress Bar
@Composable
fun AchievementProgress(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val progress = if (achievement.maxProgress > 0) 
        achievement.progress.toFloat() / achievement.maxProgress.toFloat() 
    else 0f

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (achievement.isUnlocked) Color(0xFF2C3E50) else Color(0xFF757575)
            )
            Text(
                text = "${achievement.progress}/${achievement.maxProgress}",
                fontSize = 12.sp,
                color = if (achievement.isUnlocked) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = if (achievement.isUnlocked) achievement.color else Color(0xFFE0E0E0),
            trackColor = Color(0xFFF5F5F5)
        )
    }
}





