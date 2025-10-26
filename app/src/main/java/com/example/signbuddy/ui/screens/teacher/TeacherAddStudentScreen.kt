package com.example.signbuddy.ui.screens.teacher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyRow
import com.example.signbuddy.ui.components.*
import com.example.signbuddy.services.TeacherService
import com.example.signbuddy.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAddStudentScreen(navController: NavController? = null, authViewModel: AuthViewModel? = null) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )
    
    val accentGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFF6B6B).copy(alpha = 0.1f),
            Color(0xFF4ECDC4).copy(alpha = 0.1f),
            Color.Transparent
        ),
        radius = 800f
    )

    val soundEffects = rememberSoundEffects()
    val hapticFeedback = rememberHapticFeedback()
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Form state
    var studentName by remember { mutableStateOf("") }
    var studentEmail by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🌟") }
    var selectedGrade by remember { mutableStateOf("Kindergarten") }
    
    // Services
    val teacherService = remember { TeacherService() }
    val scope = rememberCoroutineScope()
    var teacherId by remember { mutableStateOf("") }
    
    // Get teacher ID
    LaunchedEffect(Unit) {
        if (authViewModel != null) {
            val teacherInfo = authViewModel.getCurrentTeacherInfo()
            teacherInfo?.let { info ->
                teacherId = info["uid"] as? String ?: ""
            }
        }
    }

    val emojis = listOf("🌟", "⭐", "🎯", "🔥", "💪", "🎨", "🚀", "🏆", "🎪", "🎭", "🎨", "🎵")
    val grades = listOf("Kindergarten", "1st Grade", "2nd Grade", "3rd Grade")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("➕ Add Student", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            // Enhanced background with layered gradients
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentGradient)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👥 Add New Student",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fill in the details to add a new student to your class",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Student Name
                        OutlinedTextField(
                            value = studentName,
                            onValueChange = { studentName = it },
                            label = { Text("👤 Student Name") },
                            placeholder = { Text("Enter student's full name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Student Email
                        OutlinedTextField(
                            value = studentEmail,
                            onValueChange = { studentEmail = it },
                            label = { Text("📧 Email Address") },
                            placeholder = { Text("Enter student's email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Grade Selection
                        Text(
                            text = "📚 Grade Level",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grades.forEach { grade ->
                                val isSelected = grade == selectedGrade
                                val gradeIs = MutableInteractionSource()
                                val gradePressed by gradeIs.collectIsPressedAsState()
                                val gradeScale by animateFloatAsState(
                                    targetValue = if (gradePressed) 0.95f else 1f,
                                    label = "gradeButton"
                                )
                                
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = gradeScale, scaleY = gradeScale),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5)
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isSelected) 6.dp else 2.dp
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    onClick = {
                                        soundEffects.playButtonClick()
                                        hapticFeedback.lightTap()
                                        selectedGrade = grade
                                    },
                                    interactionSource = gradeIs
                                ) {
                                    Text(
                                        text = grade,
                                        modifier = Modifier.padding(12.dp),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Emoji Selection
                        Text(
                            text = "😊 Choose Avatar Emoji",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(emojis.size) { index ->
                                val emoji = emojis[index]
                                val isSelected = emoji == selectedEmoji
                                val emojiIs = MutableInteractionSource()
                                val emojiPressed by emojiIs.collectIsPressedAsState()
                                val emojiScale by animateFloatAsState(
                                    targetValue = if (emojiPressed) 0.9f else 1f,
                                    label = "emojiButton"
                                )
                                
                                Card(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .graphicsLayer(scaleX = emojiScale, scaleY = emojiScale),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isSelected) 8.dp else 4.dp
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    onClick = {
                                        soundEffects.playButtonClick()
                                        hapticFeedback.lightTap()
                                        selectedEmoji = emoji
                                    },
                                    interactionSource = emojiIs
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = emoji,
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Student Button
                val addIs = MutableInteractionSource()
                val addPressed by addIs.collectIsPressedAsState()
                val addScale by animateFloatAsState(
                    targetValue = if (addPressed) 0.96f else 1f,
                    label = "addButton"
                )
                
                Button(
                    onClick = {
                        if (studentName.isBlank() || studentEmail.isBlank()) {
                            errorMessage = "Please fill in all required fields"
                            showErrorDialog = true
                        } else if (teacherId.isEmpty()) {
                            errorMessage = "Teacher not found. Please try again."
                            showErrorDialog = true
                        } else {
                            scope.launch {
                                isLoading = true
                                try {
                                    val result = teacherService.addStudentToClass(
                                        teacherId = teacherId,
                                        studentName = studentName,
                                        studentEmail = studentEmail,
                                        grade = selectedGrade,
                                        emoji = selectedEmoji
                                    )
                                    
                                    result.onSuccess {
                                        soundEffects.playButtonClick()
                                        hapticFeedback.lightTap()
                                        showSuccessDialog = true
                                        // Clear form
                                        studentName = ""
                                        studentEmail = ""
                                        selectedEmoji = "🌟"
                                        selectedGrade = "Kindergarten"
                                    }.onFailure { exception ->
                                        errorMessage = "Failed to add student: ${exception.message}"
                                        showErrorDialog = true
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "An error occurred: ${e.message}"
                                    showErrorDialog = true
                                }
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer(scaleX = addScale, scaleY = addScale),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(16.dp),
                    interactionSource = addIs,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "➕ Add Student to Class",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("✅ Student Added!") },
            text = { Text("$studentName has been successfully added to your class!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        showSuccessDialog = false
                        navController?.popBackStack()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
    
    // Error dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("❌ Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        showErrorDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}





