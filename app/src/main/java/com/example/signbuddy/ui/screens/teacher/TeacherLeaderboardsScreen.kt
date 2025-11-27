package com.example.signbuddy.ui.screens.teacher

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.navigation.NavController
import com.example.signbuddy.services.TeacherService
import com.example.signbuddy.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLeaderboardsScreen(navController: NavController? = null, authViewModel: AuthViewModel? = null) {
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fun")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(500, easing = EaseInOutQuad), RepeatMode.Reverse), label = "bounce"
    )
    val wiggleAngle by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "wiggle"
    )
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "sparkle"
    )

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.3f), Color(0xFFFFF8E1), Color(0xFFFFF5F5))
    )

    var showConfetti by remember { mutableStateOf(false) }
    var leaderboardEntries by remember { mutableStateOf<List<TeacherService.LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val teacherService = remember { TeacherService() }
    
    LaunchedEffect(Unit) {
        authViewModel?.getCurrentTeacherInfo()?.let { info ->
            val teacherId = info["uid"] as? String ?: ""
            if (teacherId.isNotEmpty()) {
                try { leaderboardEntries = teacherService.getClassLeaderboard(teacherId, 10) } catch (_: Exception) {}
                isLoading = false
            }
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 28.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leaderboard", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.popBackStack()
                    }) { Text("⬅️", fontSize = 24.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFB300),
                    titleContentColor = Color.White
                )
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(inner)
        ) {
            // Decorations
            Text("⭐", fontSize = 28.sp, modifier = Modifier.offset(x = 30.dp, y = 50.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 26.sp, modifier = Modifier.offset(x = 320.dp, y = 80.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🎉", fontSize = 30.sp, modifier = Modifier.offset(x = 320.dp, y = 450.dp).rotate(wiggleAngle))
            Text("🏅", fontSize = 26.sp, modifier = Modifier.offset(x = 20.dp, y = 400.dp).rotate(-wiggleAngle))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 50 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Card(
                        modifier = Modifier.fillMaxWidth().offset(y = bounceOffset.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(12.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆", fontSize = 48.sp, modifier = Modifier.rotate(wiggleAngle))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Class Champions!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF8F00))
                            Text("See who's leading the class! 🌟", fontSize = 14.sp, color = Color.Gray)
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFFB300))
                        }
                    } else if (leaderboardEntries.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏆", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Students Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
                                Text("Add students to see the leaderboard!", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        leaderboardEntries.forEachIndexed { index, entry ->
                            LeaderboardCard(
                                rank = entry.rank,
                                name = entry.studentName,
                                score = entry.score
                            )
                        }
                    }

                    // Celebrate Button
                    Button(
                        onClick = {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                            showConfetti = true
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                    ) {
                        Text("🎉", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Celebrate Winners!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            com.example.signbuddy.ui.screens.teacher.components.ConfettiOverlay(visible = showConfetti) { showConfetti = false }
        }
    }
}

@Composable
fun LeaderboardCard(rank: Int, name: String, score: Int) {
    val (bgColor, emoji) = when (rank) {
        1 -> Color(0xFFFFD700).copy(alpha = 0.2f) to "🥇"
        2 -> Color(0xFFC0C0C0).copy(alpha = 0.2f) to "🥈"
        3 -> Color(0xFFCD7F32).copy(alpha = 0.2f) to "🥉"
        else -> Color.White to "⭐"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (rank <= 3) 8.dp else 4.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("#$rank", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF8F00))
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$score pts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
        }
    }
}
