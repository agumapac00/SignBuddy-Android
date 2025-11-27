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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.services.TeacherService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherClassPerformanceScreen(navController: NavController? = null, teacherId: String = "") {
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
        colors = listOf(Color(0xFF81D4FA), Color(0xFFE1F5FE), Color(0xFFFFF5F5))
    )

    var classStats by remember { mutableStateOf<TeacherService.ClassPerformanceStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showConfetti by remember { mutableStateOf(false) }
    val teacherService = remember { TeacherService() }
    
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try { classStats = teacherService.getClassPerformanceStats(teacherId) } catch (_: Exception) {}
        }
        isLoading = false
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 28.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Performance", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.popBackStack()
                    }) { Text("⬅️", fontSize = 24.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF29B6F6),
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
            Text("⭐", fontSize = 24.sp, modifier = Modifier.offset(x = 30.dp, y = 50.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("📈", fontSize = 28.sp, modifier = Modifier.offset(x = 320.dp, y = 80.dp).rotate(wiggleAngle))
            Text("🎯", fontSize = 26.sp, modifier = Modifier.offset(x = 320.dp, y = 450.dp).rotate(-wiggleAngle))

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
                            Text("📊", fontSize = 48.sp, modifier = Modifier.rotate(wiggleAngle))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Class Insights", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0288D1))
                            Text("See how your class is doing! 🌟", fontSize = 14.sp, color = Color.Gray)
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF29B6F6))
                        }
                    } else if (classStats == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Data Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                                Text("Data will appear once students start learning!", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        // Stats Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PerformanceStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Accuracy",
                                value = "${(classStats!!.averageAccuracy * 100).toInt()}%",
                                icon = Icons.Filled.CheckCircle,
                                color = Color(0xFF4CAF50)
                            )
                            PerformanceStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Students",
                                value = "${classStats!!.totalStudents}",
                                icon = Icons.Filled.BarChart,
                                color = Color(0xFF2196F3)
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PerformanceStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Avg Speed",
                                value = String.format("%.1f", classStats!!.averageSpeed),
                                icon = Icons.Filled.Speed,
                                color = Color(0xFFFF9800)
                            )
                            PerformanceStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Sessions",
                                value = "${classStats!!.totalSessions}",
                                icon = Icons.Filled.TrendingUp,
                                color = Color(0xFF9C27B0)
                            )
                        }

                        // Accuracy Distribution
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("📊 Accuracy Distribution", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                                Spacer(modifier = Modifier.height(12.dp))
                                classStats!!.accuracyDistribution.forEach { (range, count) ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(range, modifier = Modifier.width(80.dp), fontSize = 14.sp)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(20.dp)
                                                .background(Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        ) {
                                            val fraction = if (classStats!!.totalStudents > 0) count.toFloat() / classStats!!.totalStudents else 0f
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction)
                                                    .background(Color(0xFF4CAF50), RoundedCornerShape(10.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("$count", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // Common Mistakes
                        if (classStats!!.commonMistakes.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(8.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("🔥 Common Mistakes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5722))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    classStats!!.commonMistakes.entries.take(5).forEach { (letter, count) ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Letter '$letter'", fontSize = 14.sp)
                                            Text("$count mistakes", color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
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
                        Text("Celebrate Progress!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            com.example.signbuddy.ui.screens.teacher.components.ConfettiOverlay(visible = showConfetti) { showConfetti = false }
        }
    }
}

@Composable
fun PerformanceStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
