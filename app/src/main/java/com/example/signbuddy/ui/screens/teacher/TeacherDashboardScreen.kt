package com.example.signbuddy.ui.screens.teacher

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.services.TeacherService
import com.example.signbuddy.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(navController: NavController? = null, authViewModel: AuthViewModel? = null) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // Animations
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
        colors = listOf(Color(0xFFFFB6B6), Color(0xFFFFD5D5), Color(0xFFFFF5F5))
    )

    var showLogoutDialog by remember { mutableStateOf(false) }
    var teacherName by remember { mutableStateOf("Teacher") }
    var teacherId by remember { mutableStateOf("") }
    val teacherService = remember { TeacherService() }
    var classStats by remember { mutableStateOf(TeacherService.ClassStatistics(0, 0, 0f, 0)) }
    var isLoadingStats by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null && authViewModel != null) {
            navController?.navigate("teacherLogin") { popUpTo(0) { inclusive = true } }
            return@LaunchedEffect
        }
        
        authViewModel?.getCurrentTeacherInfo()?.let { info ->
            teacherName = info["displayName"] as? String ?: "Teacher"
            teacherId = info["uid"] as? String ?: ""
            
            if (teacherId.isNotEmpty()) {
                try { classStats = teacherService.getClassStatistics(teacherId) } catch (_: Exception) {}
                isLoadingStats = false
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
                        Text("👩‍🏫", fontSize = 26.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Teacher Hub", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE57373),
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
            Text("📚", fontSize = 35.sp, modifier = Modifier.offset(x = 300.dp, y = 30.dp).rotate(wiggleAngle))
            Text("⭐", fontSize = 22.sp, modifier = Modifier.offset(x = 30.dp, y = 100.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🍎", fontSize = 28.sp, modifier = Modifier.offset(x = 320.dp, y = 450.dp).rotate(-wiggleAngle))
            Text("✏️", fontSize = 24.sp, modifier = Modifier.offset(x = 20.dp, y = 500.dp).rotate(wiggleAngle))

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
                    // Welcome Card
                    Card(
                        modifier = Modifier.fillMaxWidth().offset(y = bounceOffset.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(12.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👋", fontSize = 40.sp, modifier = Modifier.rotate(wiggleAngle))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Welcome, $teacherName!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F))
                            Text("Manage your classroom! 🎓", fontSize = 14.sp, color = Color.Gray)
                        }
                    }

                    // Stats Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TeacherStatCard(
                            modifier = Modifier.weight(1f),
                            emoji = "👥",
                            value = if (isLoadingStats) "..." else "${classStats.totalStudents}",
                            label = "Students",
                            color = Color(0xFF4ECDC4)
                        )
                        TeacherStatCard(
                            modifier = Modifier.weight(1f),
                            emoji = "🌟",
                            value = if (isLoadingStats) "..." else "${classStats.activeStudents}",
                            label = "Active",
                            color = Color(0xFF4CAF50)
                        )
                        TeacherStatCard(
                            modifier = Modifier.weight(1f),
                            emoji = "📈",
                            value = if (isLoadingStats) "..." else "${(classStats.averageProgress * 100).toInt()}%",
                            label = "Progress",
                            color = Color(0xFFFF9800)
                        )
                    }

                    // Action Cards
                    TeacherActionCard("View Students", "👥", Color(0xFFFF6B6B)) {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.navigate("teacher/students")
                    }
                    
                    TeacherActionCard("Add Student", "➕", Color(0xFF6C63FF)) {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.navigate("teacher/students/add")
                    }
                    
                    TeacherActionCard("Class Performance", "📊", Color(0xFFFFB74D)) {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.navigate("teacher/class/performance/$teacherId")
                    }
                    
                    TeacherActionCard("Leaderboards", "🏆", Color(0xFFF48FB1)) {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.navigate("teacher/class/leaderboard")
                    }
                    
                    TeacherActionCard("Reports", "📈", Color(0xFF4ECDC4)) {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.navigate("teacher/reports/$teacherId")
                    }

                    // Logout Button
                    val logoutInteraction = remember { MutableInteractionSource() }
                    val logoutPressed by logoutInteraction.collectIsPressedAsState()
                    val logoutScale by animateFloatAsState(
                        targetValue = if (logoutPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f), label = "logout"
                    )

                    Button(
                        onClick = {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                            showLogoutDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).scale(logoutScale),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        interactionSource = logoutInteraction
                    ) {
                        Text("🚪", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Row { Text("🚪", fontSize = 28.sp); Spacer(Modifier.width(8.dp)); Text("Logout?") } },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel?.signOut()
                    navController?.navigate("teacherLogin") { popUpTo(0) { inclusive = true } }
                }) { Text("Logout", color = Color(0xFFF44336), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Stay") }
            }
        )
    }
}

@Composable
fun TeacherStatCard(modifier: Modifier, emoji: String, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 26.sp)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun TeacherActionCard(title: String, emoji: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f), label = "action"
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(70.dp).scale(scale),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = onClick,
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
