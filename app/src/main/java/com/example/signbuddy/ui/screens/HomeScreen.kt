package com.example.signbuddy.ui.screens.tabs

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.signbuddy.services.StudentService
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(navController: NavHostController, username: String) {
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // Fun animations
    val infiniteTransition = rememberInfiniteTransition(label = "fun")
    
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    val wiggleAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )
    
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )

    // Kindergarten colors
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE066), // Sunshine yellow
            Color(0xFFFFF5CC),
            Color(0xFFFFFFF0)
        )
    )

    // Real data state
    var studentStats by remember { mutableStateOf<StudentService.StudentStats?>(null) }
    var dayStreak by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val studentService = remember { StudentService() }

    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            try {
                studentStats = studentService.getStudentStats(username)
                studentStats?.let { stats ->
                    dayStreak = if (stats.streakDays > 0) stats.streakDays else (stats.practiceSessions / 7).coerceAtLeast(1)
                } ?: run {
                    studentStats = StudentService.StudentStats(0, 0, 1, 0, 0f, 0, 0, 1, emptyList())
                    dayStreak = 1
                }
            } catch (e: Exception) {
                studentStats = StudentService.StudentStats(0, 0, 1, 0, 0f, 0, 0, 1, emptyList())
                dayStreak = 1
            }
            isLoading = false
        } else {
            studentStats = StudentService.StudentStats(0, 0, 1, 0, 0f, 0, 0, 1, emptyList())
            dayStreak = 1
            isLoading = false
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        // Decorations
        Box(modifier = Modifier.fillMaxSize()) {
            Text("🌞", fontSize = 45.sp, modifier = Modifier.offset(x = 300.dp, y = 20.dp).rotate(wiggleAngle))
            Text("☁️", fontSize = 35.sp, modifier = Modifier.offset(x = 20.dp, y = 30.dp).graphicsLayer { alpha = 0.6f })
            Text("⭐", fontSize = 22.sp, modifier = Modifier.offset(x = 50.dp, y = 150.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 20.sp, modifier = Modifier.offset(x = 340.dp, y = 200.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🦋", fontSize = 24.sp, modifier = Modifier.offset(x = 30.dp, y = 550.dp).rotate(wiggleAngle))
            Text("🌈", fontSize = 35.sp, modifier = Modifier.offset(x = 280.dp, y = 580.dp))
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 50 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = bounceOffset.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👋", fontSize = 36.sp, modifier = Modifier.rotate(wiggleAngle))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Hi, $username!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5D4E37)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ready to learn sign language? 🤟",
                            fontSize = 16.sp,
                            color = Color(0xFF8B7355)
                        )
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Streak Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B6B)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔥", fontSize = 32.sp)
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("$dayStreak", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            Text("Day Streak!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Letters Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4ECDC4)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📚", fontSize = 32.sp)
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                val letters = (studentStats?.lettersLearned ?: 0).coerceAtMost(26)
                                Text("$letters", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            Text("Letters!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Action Cards
                KidsDashboardCard(
                    title = "My Progress",
                    emoji = "📈",
                    color = Color(0xFF4ECDC4),
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("progress/$username")
                    }
                )

                KidsDashboardCard(
                    title = "My Badges",
                    emoji = "🏆",
                    color = Color(0xFFFF6B6B),
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("achievements/$username")
                    }
                )

                KidsDashboardCard(
                    title = "Leaderboard",
                    emoji = "🏅",
                    color = Color(0xFF6C63FF),
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("leaderboard/$username")
                    }
                )
            }
        }
    }
}

@Composable
fun KidsDashboardCard(
    title: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = onClick,
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
