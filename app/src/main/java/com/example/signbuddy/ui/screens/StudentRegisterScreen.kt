package com.example.signbuddy.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.data.FirestoreService
import com.example.signbuddy.data.StudentProfile
import com.example.signbuddy.data.User
import com.example.signbuddy.data.UserType
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun StudentRegisterScreen(
    navController: NavController
) {
    var studentUsername by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val firestoreService = remember { FirestoreService() }
    val scope = rememberCoroutineScope()

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Enhanced animated student logo area
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4ECDC4).copy(alpha = 0.3f),
                                Color(0xFF44A08D).copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(70.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.8f),
                                    Color.White.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(50.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "Student Registration Icon",
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Student Registration 🎓",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your account to start learning ASL! 🌟",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Username Input
            OutlinedTextField(
                value = studentUsername,
                onValueChange = { if (it.length <= 20) studentUsername = it },
                label = { Text("Choose Your Username 🆔") },
                placeholder = { Text("Enter a unique username...") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )


            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (showError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Success message
            if (showSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Registration successful! Logging you in...",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register Button
            run {
                val isrc = MutableInteractionSource()
                val pressed by isrc.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.96f else 1f,
                    animationSpec = tween(100),
                    label = "registerScale"
                )
                Button(
                    onClick = { 
                        if (studentUsername.isBlank()) {
                            showError = true
                            errorMessage = "Please enter a username"
                        } else if (studentUsername.length < 3) {
                            showError = true
                            errorMessage = "Username must be at least 3 characters long"
                        } else {
                            scope.launch {
                                isLoading = true
                                showError = false
                                showSuccess = false
                                
                                // Check if username is available
                                firestoreService.isUsernameAvailable(studentUsername)
                                    .onSuccess { isAvailable ->
                                        if (!isAvailable) {
                                            showError = true
                                            errorMessage = "Username is already taken. Please choose another one."
                                            isLoading = false
                                        } else {
                                            // Create student account
                                            createStudentAccount(studentUsername, firestoreService) { success, message ->
                                                if (success) {
                                                    showSuccess = true
                                                    showError = false
                                                    
                                                    // Store username before clearing the form
                                                    val registeredUsername = studentUsername
                                                    
                                                    // Clear the form
                                                    studentUsername = ""
                                                    
                                                    // Show success message briefly, then navigate
                                                    scope.launch {
                                                        delay(1500) // Show success message for 1.5 seconds
                                                        navController.navigate("studentDashboard/$registeredUsername?tab=0") {
                                                            popUpTo(0) { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    showError = true
                                                    errorMessage = message
                                                }
                                                isLoading = false
                                            }
                                        }
                                    }
                                    .onFailure { exception ->
                                        showError = true
                                        errorMessage = "Error checking username: ${exception.message}"
                                        isLoading = false
                                    }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    interactionSource = isrc,
                    enabled = !isLoading
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Create Account 🚀",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Back to Login
            val backInteractionSource = remember { MutableInteractionSource() }
            val backPressed by backInteractionSource.collectIsPressedAsState()
            val backScale by animateFloatAsState(
                targetValue = if (backPressed) 0.95f else 1f,
                animationSpec = tween(100),
                label = "backButton"
            )
            TextButton(
                onClick = { navController.popBackStack() },
                interactionSource = backInteractionSource,
                modifier = Modifier.graphicsLayer(scaleX = backScale, scaleY = backScale)
            ) {
                Text(
                    text = "Already have an account? Sign In",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private suspend fun createStudentAccount(
    username: String,
    firestoreService: FirestoreService,
    callback: (Boolean, String) -> Unit
) {
    // Generate a unique UID for the student
    val studentUid = "student_${System.currentTimeMillis()}_${username}"
    
    // Create user account
    val user = User(
        uid = studentUid,
        email = "", // No email required for students
        username = username,
        displayName = username,
        userType = UserType.STUDENT,
        createdAt = Date()
    )
    
    // Create student profile with initial values
    val studentProfile = StudentProfile(
        uid = studentUid,
        username = username,
        displayName = username,
        totalScore = 0,
        totalXp = 0,
        level = 1,
        achievements = emptyList(),
        practiceSessions = 0,
        averageAccuracy = 0f,
        lettersLearned = 0,
        enrolledAt = Date(),
        teacherId = null, // Student will be enrolled later by teacher
        grade = null,
        emoji = null,
        email = null,
        lastActive = null,
        streakDays = 0,
        lastStreakDate = null
    )
    
    // Save to Firestore
    firestoreService.createUser(user)
        .onSuccess {
            firestoreService.createStudentProfile(studentProfile)
                .onSuccess {
                    callback(true, "Account created successfully!")
                }
                .onFailure { exception ->
                    callback(false, "Failed to create profile: ${exception.message}")
                }
        }
        .onFailure { exception ->
            callback(false, "Failed to create account: ${exception.message}")
        }
}