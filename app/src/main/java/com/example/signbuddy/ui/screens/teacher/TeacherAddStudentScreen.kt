package com.example.signbuddy.ui.screens.teacher

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.services.TeacherService
import com.example.signbuddy.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAddStudentScreen(navController: NavController? = null, authViewModel: AuthViewModel? = null) {
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "fun")
    val wiggleAngle by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "wiggle"
    )
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "sparkle"
    )

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFB6B6), Color(0xFFFFD5D5), Color(0xFFFFF5F5))
    )

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var studentName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🌟") }
    var selectedGrade by remember { mutableStateOf("Kindergarten") }
    
    val teacherService = remember { TeacherService() }
    val scope = rememberCoroutineScope()
    var teacherId by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        authViewModel?.getCurrentTeacherInfo()?.let { info ->
            teacherId = info["uid"] as? String ?: ""
        }
    }

    val emojis = listOf("🌟", "⭐", "🎯", "🔥", "💪", "🎨", "🚀", "🏆", "🎪", "🎭", "🎵", "🦋")
    val grades = listOf("Kindergarten", "1st Grade", "2nd Grade", "3rd Grade")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("➕", fontSize = 24.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Student", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.popBackStack()
                    }) { Text("⬅️", fontSize = 24.sp) }
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
            Text("🍎", fontSize = 28.sp, modifier = Modifier.offset(x = 320.dp, y = 400.dp).rotate(wiggleAngle))

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add New Student", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F))
                        Text("Fill in the details below", fontSize = 14.sp, color = Color.Gray)
                    }
                }

                // Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = studentName,
                            onValueChange = { if (it.all { c -> c.isLetterOrDigit() || c.isWhitespace() || c == '_' }) studentName = it },
                            label = { Text("👤 Student Username") },
                            placeholder = { Text("e.g., john_smith") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE57373),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )

                        Text("📚 Grade Level", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            grades.forEach { grade ->
                                val isSelected = grade == selectedGrade
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFE57373) else Color(0xFFF5F5F5)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    onClick = {
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                                        selectedGrade = grade
                                    }
                                ) {
                                    Text(
                                        grade.replace(" Grade", ""),
                                        modifier = Modifier.padding(10.dp),
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Text("😊 Avatar Emoji", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(emojis.size) { index ->
                                val emoji = emojis[index]
                                val isSelected = emoji == selectedEmoji
                                Card(
                                    modifier = Modifier.size(50.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFE57373) else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 4.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    onClick = {
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                                        selectedEmoji = emoji
                                    }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Button
                val addInteraction = remember { MutableInteractionSource() }
                val addPressed by addInteraction.collectIsPressedAsState()
                val addScale by animateFloatAsState(
                    targetValue = if (addPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f), label = "add"
                )

                Button(
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        if (studentName.isBlank()) {
                            errorMessage = "Please enter a username!"
                            showErrorDialog = true
                        } else if (teacherId.isEmpty()) {
                            errorMessage = "Teacher not found!"
                            showErrorDialog = true
                        } else {
                            scope.launch {
                                isLoading = true
                                teacherService.addStudentToClass(teacherId, studentName, selectedGrade, selectedEmoji)
                                    .onSuccess {
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                                        showSuccessDialog = true
                                        studentName = ""
                                        selectedEmoji = "🌟"
                                        selectedGrade = "Kindergarten"
                                    }
                                    .onFailure { e ->
                                        errorMessage = "Failed: ${e.message}"
                                        showErrorDialog = true
                                    }
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp).scale(addScale),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    interactionSource = addInteraction,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    } else {
                        Text("➕", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Student", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Row { Text("✅", fontSize = 28.sp); Spacer(Modifier.width(8.dp)); Text("Success!") } },
            text = { Text("Student added to your class! 🎉") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false; navController?.popBackStack() }) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
    
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Row { Text("❌", fontSize = 28.sp); Spacer(Modifier.width(8.dp)); Text("Error") } },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) { Text("OK") }
            }
        )
    }
}
