package com.example.signbuddy.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.data.AchievementsData
import com.example.signbuddy.services.StudentService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavController, username: String = "") {
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // Fun animations
    val infiniteTransition = rememberInfiniteTransition(label = "fun")
    
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

    // Gold theme
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE066),
            Color(0xFFFFF5CC),
            Color(0xFFFFFFF0)
        )
    )

    // Real data
    var studentStats by remember { mutableStateOf<StudentService.StudentStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val studentService = remember { StudentService() }

    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            try {
                studentStats = studentService.getStudentStats(username)
                if (studentStats == null) {
                    studentStats = StudentService.StudentStats(0, 0, 1, 0, 0f, 0, 0, 1, emptyList())
                }
            } catch (e: Exception) {
                studentStats = StudentService.StudentStats(0, 0, 1, 0, 0f, 0, 0, 1, emptyList())
            }
        } else {
            studentStats = StudentService.StudentStats(0, 0, 1, 0, 0f, 0, 0, 1, emptyList())
        }
        isLoading = false
    }

    // Refresh periodically
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            if (username.isNotEmpty()) {
                try {
                    studentStats = studentService.getStudentStats(username)
                } catch (_: Exception) {}
            }
        }
    }

    val achievements = AchievementsData.allAchievements.map { achievement ->
        val isUnlocked = studentStats?.achievements?.contains(achievement.id) ?: false
        achievement.copy(unlocked = isUnlocked)
    }
    val unlockedCount = achievements.count { it.unlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 26.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Badges!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.popBackStack()
                    }) {
                        Text("⬅️", fontSize = 26.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFB300),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding)
        ) {
            // Decorations
            Box(modifier = Modifier.fillMaxSize()) {
                Text("⭐", fontSize = 24.sp, modifier = Modifier.offset(x = 30.dp, y = 50.dp).graphicsLayer { alpha = sparkleAlpha })
                Text("🌟", fontSize = 22.sp, modifier = Modifier.offset(x = 330.dp, y = 100.dp).graphicsLayer { alpha = sparkleAlpha })
                Text("✨", fontSize = 20.sp, modifier = Modifier.offset(x = 40.dp, y = 400.dp).graphicsLayer { alpha = sparkleAlpha })
                Text("💫", fontSize = 22.sp, modifier = Modifier.offset(x = 320.dp, y = 500.dp).graphicsLayer { alpha = sparkleAlpha })
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(12.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎉 You're Amazing! 🎉", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Keep learning to unlock more!", fontSize = 14.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$unlockedCount", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
                                    Text("Unlocked", fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${achievements.size}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2196F3))
                                    Text("Total", fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${(unlockedCount * 100 / achievements.size.coerceAtLeast(1))}%", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9800))
                                    Text("Done", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Achievement cards
                items(achievements) { achievement ->
                    KidsAchievementCard(achievement = achievement)
                }
            }
        }
    }
}

@Composable
fun KidsAchievementCard(achievement: com.example.signbuddy.data.Achievement) {
    val scale by animateFloatAsState(
        targetValue = if (achievement.unlocked) 1f else 0.98f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.unlocked) 8.dp else 2.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        if (achievement.unlocked) Color(0xFFFFF3E0) else Color(0xFFEEEEEE),
                        RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(achievement.emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (achievement.unlocked) Color(0xFF333333) else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    achievement.description,
                    fontSize = 13.sp,
                    color = if (achievement.unlocked) Color(0xFF666666) else Color.LightGray
                )
            }

            Text(
                if (achievement.unlocked) "✅" else "🔒",
                fontSize = 28.sp
            )
        }
    }
}
