package com.example.signbuddy.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.signbuddy.services.TeacherService
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavController, username: String = "") {
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

    // Blue theme
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF87CEEB),
            Color(0xFFB0E0E6),
            Color(0xFFF0F8FF)
        )
    )

    // Real data
    var classLeaderboard by remember { mutableStateOf<List<TeacherService.LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isEnrolled by remember { mutableStateOf(false) }
    var teacherId by remember { mutableStateOf<String?>(null) }
    val teacherService = remember { TeacherService() }

    LaunchedEffect(username) {
        try {
            if (username.isNotEmpty()) {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                var studentSnapshot = firestore.collection("studentProfiles")
                    .whereEqualTo("username", username).limit(1).get().await()

                if (studentSnapshot.isEmpty) {
                    val normalized = username.replace(" ", "_")
                    studentSnapshot = firestore.collection("studentProfiles")
                        .whereEqualTo("username", normalized).limit(1).get().await()
                }

                if (!studentSnapshot.isEmpty) {
                    val doc = studentSnapshot.documents.first()
                    val tid = doc.getString("teacherId")
                    teacherId = tid
                    isEnrolled = !tid.isNullOrEmpty()

                    if (isEnrolled && teacherId != null) {
                        try {
                            classLeaderboard = teacherService.getClassLeaderboard(teacherId!!, 20)
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏅", fontSize = 26.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Top Stars!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
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
                    containerColor = Color(0xFF2196F3),
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
                Text("🎈", fontSize = 28.sp, modifier = Modifier.offset(x = 320.dp, y = 500.dp).rotate(wiggleAngle))
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
                            Text("🌟 Top Learners! 🌟", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2196F3))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("See who's doing great!", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF2196F3))
                        }
                    }
                } else if (classLeaderboard.isEmpty() && !isEnrolled) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👨‍🏫", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Not Enrolled Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ask your teacher to add you!", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else if (classLeaderboard.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🏆", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Classmates Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Be the first star learner!", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Leaderboard entries
                itemsIndexed(classLeaderboard) { index, entry ->
                    val isCurrentUser = entry.studentName == username
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD700) // Gold
                        1 -> Color(0xFFC0C0C0) // Silver
                        2 -> Color(0xFFCD7F32) // Bronze
                        else -> Color(0xFF2196F3)
                    }
                    val rankEmoji = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "#${index + 1}"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(if (isCurrentUser) 1.02f else 1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentUser) Color(0xFFE3F2FD) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(if (isCurrentUser) 12.dp else 6.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(rankColor.copy(alpha = 0.2f), RoundedCornerShape(25.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    rankEmoji,
                                    fontSize = if (index < 3) 24.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rankColor
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Level emoji
                            Text(
                                when {
                                    entry.level >= 10 -> "🌟"
                                    entry.level >= 7 -> "⭐"
                                    entry.level >= 4 -> "🎯"
                                    else -> "🔥"
                                },
                                fontSize = 24.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name and score
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.studentName,
                                    fontSize = 18.sp,
                                    fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isCurrentUser) Color(0xFF1565C0) else Color(0xFF333333)
                                )
                                Text(
                                    "${entry.score} pts • Level ${entry.level}",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }

                            // Score badge
                            Box(
                                modifier = Modifier
                                    .background(rankColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "${entry.score}",
                                    fontWeight = FontWeight.Bold,
                                    color = rankColor,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
