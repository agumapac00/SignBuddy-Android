package com.example.signbuddy.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.data.UserType
import com.example.signbuddy.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun TeacherRegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var teacherEmail by remember { mutableStateOf("") }
    var teacherPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var teacherUsername by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    val isLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.errorMessage.collectAsState()
    val isRegistrationSuccessful by authViewModel.isRegistrationSuccessful.collectAsState()

    LaunchedEffect(authError) {
        if (authError != null) {
            showError = true
            errorMessage = authError!!
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.clearUserState()
    }

    LaunchedEffect(isRegistrationSuccessful) {
        if (isRegistrationSuccessful) {
            navController.navigate("teacher/dashboard") {
                popUpTo(0) { inclusive = true }
            }
        }
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

    // Coral theme
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFB6B6),
            Color(0xFFFFD5D5),
            Color(0xFFFFF5F5)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        // Decorations
        Box(modifier = Modifier.fillMaxSize()) {
            Text("🎓", fontSize = 40.sp, modifier = Modifier.offset(x = 300.dp, y = 30.dp).rotate(wiggleAngle))
            Text("⭐", fontSize = 22.sp, modifier = Modifier.offset(x = 30.dp, y = 100.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 20.sp, modifier = Modifier.offset(x = 340.dp, y = 200.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🍎", fontSize = 28.sp, modifier = Modifier.offset(x = 20.dp, y = 650.dp).rotate(-wiggleAngle))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Mascot
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFFB6B6).copy(alpha = 0.5f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                Text("🏫", fontSize = 50.sp, modifier = Modifier.rotate(wiggleAngle / 2))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Join Our Team!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFD32F2F)
            )
            Text("Create your teacher account 👩‍🏫", fontSize = 14.sp, color = Color(0xFFE57373))

            Spacer(modifier = Modifier.height(16.dp))

            // Input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { if (it.length <= 30) teacherName = it },
                        label = { Text("👩‍🏫 Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE57373),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = teacherUsername,
                        onValueChange = { if (it.length <= 20) teacherUsername = it },
                        label = { Text("🆔 Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE57373),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { if (it.length <= 50) schoolName = it },
                        label = { Text("🏫 School Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE57373),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = teacherEmail,
                        onValueChange = { teacherEmail = it },
                        label = { Text("📧 Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE57373),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = teacherPassword,
                        onValueChange = { teacherPassword = it },
                        label = { Text("🔐 Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE57373),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("🔐 Confirm Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE57373),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    // Error
                    AnimatedVisibility(visible = showError) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(Color(0xFFFFE5E5), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorMessage, color = Color(0xFFD32F2F), fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register button
            val registerInteraction = remember { MutableInteractionSource() }
            val registerPressed by registerInteraction.collectIsPressedAsState()
            val registerScale by animateFloatAsState(
                targetValue = if (registerPressed) 0.95f else 1f,
                animationSpec = spring(dampingRatio = 0.5f),
                label = "register"
            )

            Button(
                onClick = {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    if (validateInput(teacherName, teacherUsername, schoolName, teacherEmail, teacherPassword, confirmPassword)) {
                        authViewModel.register(
                            email = teacherEmail,
                            password = teacherPassword,
                            displayName = teacherName,
                            username = teacherUsername,
                            userType = UserType.TEACHER
                        )
                    } else {
                        showError = true
                        errorMessage = "Please fill all fields correctly!"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(registerScale),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                interactionSource = registerInteraction,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("🚀", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Account", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    navController.popBackStack()
                }
            ) {
                Text("⬅️ Already have an account? Sign In", fontSize = 14.sp, color = Color(0xFFD32F2F))
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun validateInput(
    name: String,
    username: String,
    school: String,
    email: String,
    password: String,
    confirmPassword: String
): Boolean {
    return name.isNotBlank() && 
           username.isNotBlank() && 
           school.isNotBlank() && 
           email.isNotBlank() && 
           password.isNotBlank() && 
           confirmPassword.isNotBlank() && 
           password == confirmPassword &&
           password.length >= 6
}
