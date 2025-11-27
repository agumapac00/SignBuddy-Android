package com.example.signbuddy.ui.screens.teacher

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.example.signbuddy.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentsScreen(navController: NavController? = null, authViewModel: AuthViewModel? = null) {
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fun")
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

    var students by remember { mutableStateOf<List<TeacherService.StudentPerformance>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<TeacherService.StudentPerformance?>(null) }
    val teacherService = remember { TeacherService() }
    val scope = rememberCoroutineScope()
    val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try { students = teacherService.getStudents(teacherId) } catch (_: Exception) {}
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👥", fontSize = 26.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Students", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.popBackStack()
                    }) { Text("⬅️", fontSize = 24.sp) }
                },
                actions = {
                    IconButton(onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.navigate("teacher/students/add")
                    }) { Text("➕", fontSize = 24.sp) }
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
            Text("⭐", fontSize = 24.sp, modifier = Modifier.offset(x = 30.dp, y = 50.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 22.sp, modifier = Modifier.offset(x = 330.dp, y = 100.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🎓", fontSize = 28.sp, modifier = Modifier.offset(x = 320.dp, y = 450.dp).rotate(wiggleAngle))

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥 Class Overview", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MiniStatCard("Total", "${students.size}", "👥", Color(0xFF4ECDC4))
                            MiniStatCard("Active", "${students.count { it.isActive }}", "✅", Color(0xFF4CAF50))
                            MiniStatCard("Avg", if (students.isNotEmpty()) "${students.map { it.progress }.average().toInt()}%" else "0%", "📈", Color(0xFFFF9800))
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFE57373))
                    }
                } else if (students.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👥", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No Students Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            Text("Add your first student!", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController?.navigate("teacher/students/add") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("➕ Add Student", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(students) { _, student ->
                            StudentListCard(student) {
                                studentToDelete = student
                                showDeleteDialog = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Row { Text("🗑️", fontSize = 28.sp); Spacer(Modifier.width(8.dp)); Text("Remove Student?") } },
            text = { Text("Remove ${studentToDelete?.studentName} from your class?") },
            confirmButton = {
                TextButton(onClick = {
                    studentToDelete?.let { student ->
                        scope.launch {
                            try {
                                teacherService.removeStudentFromClass(student.studentId)
                                students = students.filter { it.studentId != student.studentId }
                            } catch (_: Exception) {}
                        }
                    }
                    showDeleteDialog = false
                    studentToDelete = null
                }) { Text("Remove", color = Color(0xFFF44336), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MiniStatCard(label: String, value: String, emoji: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StudentListCard(student: TeacherService.StudentPerformance, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👤", fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(student.studentName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Level ${student.level} • ${student.progress}%", fontSize = 13.sp, color = Color.Gray)
                Text("Score: ${student.totalScore}", fontSize = 12.sp, color = Color(0xFFFF9800))
            }
            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFF5722).copy(alpha = 0.1f))
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color(0xFFFF5722))
            }
        }
    }
}
