package com.example.signbuddy.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import com.example.signbuddy.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun TeacherLoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel? = null
) {
    var teacherEmail by remember { mutableStateOf("") }
    var teacherPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    val isLoading by authViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val authError by authViewModel?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLoginSuccessful by authViewModel?.isLoginSuccessful?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(authError) {
        if (authError != null) {
            showError = true
            errorMessage = authError!!
        }
    }

    LaunchedEffect(Unit) {
        authViewModel?.clearUserState()
    }

    LaunchedEffect(isLoginSuccessful) {
        if (isLoginSuccessful) {
            navController.navigate("teacher/dashboard") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Fun animations
    val infiniteTransition = rememberInfiniteTransition(label = "fun")
    
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
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

    // Coral/red theme for teacher
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFB6B6),
            Color(0xFFFFD5D5),
            Color(0xFFFFF5F5)
        )
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        // Decorations
        Box(modifier = Modifier.fillMaxSize()) {
            Text("📚", fontSize = 40.sp, modifier = Modifier.offset(x = 300.dp, y = 30.dp).rotate(wiggleAngle))
            Text("☁️", fontSize = 35.sp, modifier = Modifier.offset(x = 20.dp, y = 40.dp).graphicsLayer { alpha = 0.6f })
            Text("⭐", fontSize = 22.sp, modifier = Modifier.offset(x = 50.dp, y = 180.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 20.sp, modifier = Modifier.offset(x = 340.dp, y = 300.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🍎", fontSize = 28.sp, modifier = Modifier.offset(x = 30.dp, y = 550.dp).rotate(-wiggleAngle))
            Text("✏️", fontSize = 26.sp, modifier = Modifier.offset(x = 320.dp, y = 500.dp).rotate(wiggleAngle))
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 80 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Bouncing mascot
                Box(
                    modifier = Modifier
                        .offset(y = bounceOffset.dp)
                        .size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFB6B6).copy(alpha = 0.5f), Color.Transparent)
                                ),
                                CircleShape
                            )
                    )
                    Text("👩‍🏫", fontSize = 60.sp, modifier = Modifier.rotate(wiggleAngle / 2))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Teacher Login",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD32F2F)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📖", fontSize = 18.sp, modifier = Modifier.rotate(-wiggleAngle))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Welcome back!", fontSize = 16.sp, color = Color(0xFFE57373))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📖", fontSize = 18.sp, modifier = Modifier.rotate(wiggleAngle))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Input card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = teacherEmail,
                            onValueChange = { teacherEmail = it },
                            label = { Text("📧 Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE57373),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                            shape = RoundedCornerShape(16.dp),
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

                Spacer(modifier = Modifier.height(20.dp))

                // Login button
                val loginInteraction = remember { MutableInteractionSource() }
                val loginPressed by loginInteraction.collectIsPressedAsState()
                val loginScale by animateFloatAsState(
                    targetValue = if (loginPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f),
                    label = "login"
                )

                Button(
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        if (teacherEmail.isBlank() || teacherPassword.isBlank()) {
                            showError = true
                            errorMessage = "Please fill in all fields!"
                        } else {
                            authViewModel?.signIn(teacherEmail, teacherPassword)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .scale(loginScale),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    interactionSource = loginInteraction,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    } else {
                        Text("🚀", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Let's Teach!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        navController.navigate("teacherRegister")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .scale(registerScale),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB6B6)),
                    interactionSource = registerInteraction
                ) {
                    Text("📝", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Student button
                TextButton(
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("login")
                    }
                ) {
                    Text("👶 I'm a Student", fontSize = 14.sp, color = Color(0xFFD32F2F))
                }
            }
        }
    }
}
