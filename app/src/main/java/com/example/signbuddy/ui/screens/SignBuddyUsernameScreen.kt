package com.example.signbuddy.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.data.FirestoreService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignBuddyUsernameScreen(navController: NavController) {
    var studentUsername by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val firestoreService = remember { FirestoreService() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { context.getSharedPreferences("signbuddy_prefs", android.content.Context.MODE_PRIVATE) }

    // Sound effects
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // Fun animations for kids
    val infiniteTransition = rememberInfiniteTransition(label = "fun")
    
    // Bouncing mascot
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    // Wiggling stars
    val wiggleAngle by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )
    
    // Rainbow color shift
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbow"
    )
    
    // Sparkle alpha
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )

    // Entry animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    // Kindergarten-friendly bright colors
    val skyBlue = Color(0xFF87CEEB)
    val sunYellow = Color(0xFFFFD93D)
    val grassGreen = Color(0xFF6BCB77)
    val candyPink = Color(0xFFFF6B9D)
    val cloudWhite = Color(0xFFFFFBF0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        skyBlue,
                        Color(0xFFB4E4FF),
                        cloudWhite
                    )
                )
            )
    ) {
        // Floating clouds decoration
        Box(modifier = Modifier.fillMaxSize()) {
            // Cloud 1
            Text(
                "☁️",
                fontSize = 60.sp,
                modifier = Modifier
                    .offset(x = 20.dp, y = 50.dp)
                    .graphicsLayer { alpha = 0.7f }
            )
            // Cloud 2
            Text(
                "☁️",
                fontSize = 45.sp,
                modifier = Modifier
                    .offset(x = 280.dp, y = 30.dp)
                    .graphicsLayer { alpha = 0.6f }
            )
            // Sun
            Text(
                "🌞",
                fontSize = 50.sp,
                modifier = Modifier
                    .offset(x = 300.dp, y = 80.dp)
                    .rotate(wiggleAngle)
            )
            // Floating stars
            listOf(
                Triple(50.dp, 150.dp, "⭐"),
                Triple(320.dp, 200.dp, "🌟"),
                Triple(30.dp, 400.dp, "✨"),
                Triple(340.dp, 450.dp, "💫")
            ).forEach { (x, y, emoji) ->
                Text(
                    emoji,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .rotate(wiggleAngle)
                        .graphicsLayer { alpha = sparkleAlpha }
                )
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = tween(800, easing = EaseOutBounce)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Bouncing mascot hand
                Box(
                    modifier = Modifier
                        .offset(y = bounceOffset.dp)
                        .size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow circle
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        sunYellow.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    // Main emoji
                    Text(
                        "🤟",
                        fontSize = 80.sp,
                        modifier = Modifier.rotate(wiggleAngle / 2)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fun title with rainbow effect
                Text(
                    text = "SignBuddy",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2D5A7B),
                    textAlign = TextAlign.Center
                )
                
                // Subtitle with emojis
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🎮", fontSize = 20.sp, modifier = Modifier.rotate(-wiggleAngle))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Learn Sign Language!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5D8AA8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎮", fontSize = 20.sp, modifier = Modifier.rotate(wiggleAngle))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Fun input card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Label with emoji
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("👤", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "What's your name?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D5A7B)
                            )
                        }
                        
                        OutlinedTextField(
                            value = studentUsername,
                            onValueChange = { 
                                if (it.length <= 15) {
                                    studentUsername = it
                                    showError = false
                                }
                            },
                            placeholder = { Text("Type here...", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = grassGreen,
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color(0xFFF8FFF8),
                                unfocusedContainerColor = Color(0xFFFAFAFA)
                            )
                        )

                        // Error message
                        AnimatedVisibility(
                            visible = showError,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(
                                        Color(0xFFFFE5E5),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("😅", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    errorMessage,
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Big colorful Play button
                val playInteraction = remember { MutableInteractionSource() }
                val playPressed by playInteraction.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (playPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f),
                    label = "playScale"
                )
                
                Button(
                    onClick = { 
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                        if (studentUsername.isBlank()) {
                            showError = true
                            errorMessage = "Please type your name!"
                        } else {
                            scope.launch {
                                isLoading = true
                                showError = false
                                focusManager.clearFocus()
                                
                                firestoreService.findStudentByUsername(studentUsername)
                                    .onSuccess { studentProfile ->
                                        if (studentProfile != null) {
                                            prefs.edit().putString("logged_in_username", studentProfile.username).apply()
                                            firestoreService.updateLoginStreak(studentUsername)
                                            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                                            navController.navigate("studentDashboard/${studentProfile.username}?tab=0")
                                        } else {
                                            showError = true
                                            errorMessage = "Name not found! Try registering first 📝"
                                            toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 200)
                                        }
                                    }
                                    .onFailure {
                                        showError = true
                                        errorMessage = "Oops! Something went wrong 😢"
                                    }
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .scale(playScale),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    interactionSource = playInteraction,
                    enabled = !isLoading
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(grassGreen, Color(0xFF4CAF50))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("▶️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Let's Play!",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Register button
                val registerInteraction = remember { MutableInteractionSource() }
                val registerPressed by registerInteraction.collectIsPressedAsState()
                val registerScale by animateFloatAsState(
                    targetValue = if (registerPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f),
                    label = "registerScale"
                )
                
                Button(
                    onClick = { 
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("studentRegister") 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(registerScale),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    interactionSource = registerInteraction
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(candyPink, Color(0xFFFF8FAB))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📝", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "New Here? Join Us!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Teacher login (smaller)
                TextButton(
                    onClick = { 
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("teacherLogin") 
                    }
                ) {
                    Text(
                        "👩‍🏫 I'm a Teacher",
                        fontSize = 14.sp,
                        color = Color(0xFF5D8AA8)
                    )
                }
            }
        }
    }
}
