package com.example.signbuddy.ui.screens.tabs

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
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
import com.example.signbuddy.data.AchievementsData
import com.example.signbuddy.services.StudentService
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(username: String, navController: androidx.navigation.NavController? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("signbuddy_prefs", android.content.Context.MODE_PRIVATE) }
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

    // Lavender theme
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE8D5FF), // Lavender
            Color(0xFFF5EEFF),
            Color(0xFFFFF5FF)
        )
    )

    // Real data
    var studentStats by remember { mutableStateOf<StudentService.StudentStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val studentService = remember { StudentService() }
    var showLogoutDialog by remember { mutableStateOf(false) }

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

    val achievements = AchievementsData.allAchievements.map { achievement ->
        val isUnlocked = studentStats?.achievements?.contains(achievement.id) ?: false
        achievement.copy(unlocked = isUnlocked)
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
            Text("👤", fontSize = 40.sp, modifier = Modifier.offset(x = 300.dp, y = 25.dp).rotate(wiggleAngle))
            Text("☁️", fontSize = 35.sp, modifier = Modifier.offset(x = 20.dp, y = 30.dp).graphicsLayer { alpha = 0.6f })
            Text("⭐", fontSize = 22.sp, modifier = Modifier.offset(x = 50.dp, y = 150.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 20.sp, modifier = Modifier.offset(x = 340.dp, y = 300.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🎈", fontSize = 28.sp, modifier = Modifier.offset(x = 30.dp, y = 550.dp).rotate(-wiggleAngle))
            Text("🌈", fontSize = 35.sp, modifier = Modifier.offset(x = 280.dp, y = 580.dp))
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 50 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(10.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFD0B0FF), Color(0xFFE8D5FF), Color(0xFFF5EEFF))
                                )
                            )
                    ) {
                        // Decorative elements
                        Text("✨", fontSize = 14.sp, modifier = Modifier.offset(x = 15.dp, y = 10.dp).graphicsLayer { alpha = sparkleAlpha })
                        Text("⭐", fontSize = 12.sp, modifier = Modifier.offset(x = 320.dp, y = 15.dp).graphicsLayer { alpha = sparkleAlpha })
                        Text("💫", fontSize = 12.sp, modifier = Modifier.offset(x = 300.dp, y = 70.dp).rotate(wiggleAngle))
                        
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar with ring
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(85.dp)
                                        .background(
                                            Brush.sweepGradient(
                                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF6B6B), Color(0xFF9B7FCF), Color(0xFF4ECDC4), Color(0xFFFFD700))
                                            ),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier.size(75.dp).background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("😊", fontSize = 42.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("👋", fontSize = 22.sp, modifier = Modifier.rotate(wiggleAngle))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(username, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6B4E9B))
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏅", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Level ${studentStats?.level ?: 1} ⭐", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Letters
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4ECDC4)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📚", fontSize = 26.sp)
                        val letters = if (isLoading) 0 else (studentStats?.lettersLearned ?: 0).coerceAtMost(26)
                        Text("$letters/26", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Letters", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    }

                    // Badges
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B6B)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆", fontSize = 26.sp)
                        val badgesUnlocked = achievements.count { it.unlocked }
                        Text("$badgesUnlocked/${achievements.size}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Badges", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    }
                }

                // Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val letters = if (isLoading) 0 else (studentStats?.lettersLearned ?: 0).coerceAtMost(26)
                        val lettersProgress = letters.toFloat() / 26f
                        val badgesUnlocked = achievements.count { it.unlocked }
                        val badgesProgress = if (achievements.isNotEmpty()) badgesUnlocked.toFloat() / achievements.size else 0f
                        val overallProgress = ((lettersProgress + badgesProgress) / 2f).coerceAtMost(1f)
                        
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { overallProgress },
                                modifier = Modifier.size(70.dp),
                                strokeWidth = 9.dp,
                                color = Color(0xFF9B7FCF),
                                trackColor = Color(0xFFE8D5FF)
                            )
                            Text("${(overallProgress * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B4E9B))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("📊 My Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B4E9B))
                            Text("Keep it up! 🎉", fontSize = 14.sp, color = Color(0xFF9B7FCF))
                        }
                    }
                }

                // Logout Button
                val logoutInteraction = remember { MutableInteractionSource() }
                val logoutPressed by logoutInteraction.collectIsPressedAsState()
                val logoutScale by animateFloatAsState(
                    targetValue = if (logoutPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f),
                    label = "logout"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(logoutScale),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B6B)),
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        showLogoutDialog = true
                    },
                    interactionSource = logoutInteraction,
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚪", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Logout", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚪", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bye Bye?", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("Are you sure you want to leave? We'll miss you! 😢") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        prefs.edit().remove("logged_in_username").apply()
                        navController?.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Yes, Bye!", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Stay! 🎉", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
