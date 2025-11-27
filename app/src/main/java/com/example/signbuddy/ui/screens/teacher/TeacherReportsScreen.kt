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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
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
fun TeacherReportsScreen(navController: NavController? = null, teacherId: String = "") {
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
        colors = listOf(Color(0xFFCE93D8), Color(0xFFF3E5F5), Color(0xFFFFF5F5))
    )

    var classStats by remember { mutableStateOf<TeacherService.ClassPerformanceStats?>(null) }
    var studentReports by remember { mutableStateOf<List<TeacherService.StudentReport>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReportType by remember { mutableStateOf("overview") }
    val teacherService = remember { TeacherService() }
    
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                classStats = teacherService.getClassPerformanceStats(teacherId)
                studentReports = teacherService.getStudentReports(teacherId)
            } catch (_: Exception) {}
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
                        Text("📈", fontSize = 28.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reports", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.popBackStack()
                    }) { Text("⬅️", fontSize = 24.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF9C27B0),
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
            Text("🤖", fontSize = 28.sp, modifier = Modifier.offset(x = 320.dp, y = 80.dp).rotate(wiggleAngle))
            Text("📊", fontSize = 26.sp, modifier = Modifier.offset(x = 320.dp, y = 450.dp).rotate(-wiggleAngle))

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
                            Text("🤖", fontSize = 48.sp, modifier = Modifier.rotate(wiggleAngle))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AI Reports & Insights", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7B1FA2))
                            Text("Smart analytics for your class! 🌟", fontSize = 14.sp, color = Color.Gray)
                        }
                    }

                    // Report Type Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📊 Report Type", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    onClick = { toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50); selectedReportType = "overview" },
                                    label = { Text("Overview") },
                                    selected = selectedReportType == "overview",
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    onClick = { toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50); selectedReportType = "individual" },
                                    label = { Text("Individual") },
                                    selected = selectedReportType == "individual",
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    onClick = { toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50); selectedReportType = "class" },
                                    label = { Text("Class") },
                                    selected = selectedReportType == "class",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF9C27B0))
                        }
                    } else if (classStats == null || studentReports == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📈", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Reports Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                                Text("Reports will appear once students start learning!", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        when (selectedReportType) {
                            "overview" -> OverviewReportSection(classStats!!)
                            "individual" -> IndividualReportsSection(studentReports!!)
                            "class" -> ClassReportsSection(classStats!!)
                        }
                        
                        // AI Insights
                        AIInsightsCard(classStats!!, studentReports!!)
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewReportSection(classStats: TeacherService.ClassPerformanceStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("📈 Overview Report", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ReportMetricCard("Students", "${classStats.totalStudents}", "👥", Color(0xFF2196F3))
                ReportMetricCard("Accuracy", "${(classStats.averageAccuracy * 100).toInt()}%", "✅", Color(0xFF4CAF50))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ReportMetricCard("Sessions", "${classStats.totalSessions}", "📊", Color(0xFF9C27B0))
                ReportMetricCard("Speed", "${classStats.averageSpeed.toInt()}s", "⚡", Color(0xFFFF9800))
            }
        }
    }
}

@Composable
fun IndividualReportsSection(studentReports: List<TeacherService.StudentReport>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("👥 Individual Reports", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
            Spacer(modifier = Modifier.height(16.dp))
            studentReports.take(5).forEach { report ->
                StudentReportItem(report)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun ClassReportsSection(classStats: TeacherService.ClassPerformanceStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("🏫 Class-Level Reports", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Accuracy Distribution", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            classStats.accuracyDistribution.forEach { (range, count) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(range, fontSize = 13.sp)
                    Text("$count students", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            if (classStats.commonMistakes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Common Mistakes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                classStats.commonMistakes.entries.take(5).forEach { (letter, count) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Letter '$letter'", fontSize = 13.sp)
                        Text("$count mistakes", color = Color(0xFFFF5722), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun AIInsightsCard(classStats: TeacherService.ClassPerformanceStats, studentReports: List<TeacherService.StudentReport>) {
    val suggestions = generateAISuggestions(classStats, studentReports)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🤖", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
            }
            Spacer(modifier = Modifier.height(16.dp))
            suggestions.forEach { suggestion ->
                AssistChip(
                    onClick = { },
                    label = { Text(suggestion, fontSize = 12.sp) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ReportMetricCard(label: String, value: String, emoji: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StudentReportItem(report: TeacherService.StudentReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(report.studentName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Accuracy: ${(report.accuracy * 100).toInt()}%", fontSize = 13.sp)
                Text("Level: ${report.level}", fontSize = 13.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sessions: ${report.totalSessions}", fontSize = 13.sp)
                Text("Score: ${report.totalScore}", fontSize = 13.sp, color = Color(0xFFFF9800))
            }
        }
    }
}

fun generateAISuggestions(classStats: TeacherService.ClassPerformanceStats, studentReports: List<TeacherService.StudentReport>): List<String> {
    val suggestions = mutableListOf<String>()
    
    if (classStats.averageAccuracy < 0.7f) {
        suggestions.add("Focus on accuracy - class average is ${(classStats.averageAccuracy * 100).toInt()}%")
    }
    
    classStats.commonMistakes.maxByOrNull { it.value }?.let { (letter, count) ->
        suggestions.add("Focus on letter '$letter' - $count students struggling")
    }
    
    if (classStats.totalSessions < 50) {
        suggestions.add("Encourage more practice - only ${classStats.totalSessions} sessions")
    }
    
    val strugglingStudents = studentReports.count { it.accuracy < 0.5f }
    if (strugglingStudents > 0) {
        suggestions.add("$strugglingStudents students need extra attention")
    }
    
    if (suggestions.isEmpty()) {
        suggestions.add("Great job! Class is performing well! 🌟")
    }
    
    return suggestions
}
