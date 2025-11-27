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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.signbuddy.data.StudentProfile
import com.example.signbuddy.data.User
import com.example.signbuddy.data.UserType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun StudentRegisterScreen(navController: NavController) {
    var studentUsername by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val firestoreService = remember { FirestoreService() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Sound effects
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // Fun animations
    val infiniteTransition = rememberInfiniteTransition(label = "fun")
    
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    val wiggleAngle by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )
    
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    // Kindergarten colors
    val mintGreen = Color(0xFF98D8C8)
    val peachOrange = Color(0xFFFFAB76)
    val lavender = Color(0xFFE8D5FF)
    val skyBlue = Color(0xFFAED9E0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        lavender,
                        Color(0xFFF5E6FF),
                        Color(0xFFFFF5F5)
                    )
                )
            )
    ) {
        // Decorations
        Box(modifier = Modifier.fillMaxSize()) {
            // Balloons
            Text("🎈", fontSize = 40.sp, modifier = Modifier.offset(x = 30.dp, y = 60.dp).rotate(-wiggleAngle))
            Text("🎈", fontSize = 35.sp, modifier = Modifier.offset(x = 320.dp, y = 40.dp).rotate(wiggleAngle))
            // Stars
            Text("⭐", fontSize = 28.sp, modifier = Modifier.offset(x = 50.dp, y = 180.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 24.sp, modifier = Modifier.offset(x = 330.dp, y = 150.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("✨", fontSize = 22.sp, modifier = Modifier.offset(x = 20.dp, y = 450.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("💫", fontSize = 26.sp, modifier = Modifier.offset(x = 340.dp, y = 500.dp).graphicsLayer { alpha = sparkleAlpha })
            // Rainbow
            Text("🌈", fontSize = 45.sp, modifier = Modifier.offset(x = 280.dp, y = 80.dp))
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(
                initialOffsetY = { 80 },
                animationSpec = tween(600, easing = EaseOutBounce)
            )
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
                        .size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        peachOrange.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    Text(
                        "🎓",
                        fontSize = 70.sp,
                        modifier = Modifier.rotate(wiggleAngle / 2)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Join the Fun!",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF6B4E9B),
                    textAlign = TextAlign.Center
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎉", fontSize = 18.sp, modifier = Modifier.rotate(-wiggleAngle))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create your account",
                        fontSize = 16.sp,
                        color = Color(0xFF8B7AAF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎉", fontSize = 18.sp, modifier = Modifier.rotate(wiggleAngle))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Input card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("🏷️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pick a cool name!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B4E9B)
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
                            placeholder = { Text("Your awesome name...", color = Color.Gray) },
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
                                focusedBorderColor = Color(0xFF9B7FCF),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color(0xFFFAF5FF),
                                unfocusedContainerColor = Color(0xFFFAFAFA)
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
                                Text("😅", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMessage, color = Color(0xFFD32F2F), fontSize = 14.sp)
                            }
                        }

                        // Success
                        AnimatedVisibility(visible = showSuccess, enter = fadeIn() + scaleIn()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(Color(0xFFE5FFE5), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎊", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Welcome aboard!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text("Get ready for fun!", fontSize = 12.sp, color = Color(0xFF4CAF50))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Register button
                val registerInteraction = remember { MutableInteractionSource() }
                val registerPressed by registerInteraction.collectIsPressedAsState()
                val registerScale by animateFloatAsState(
                    targetValue = if (registerPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f),
                    label = "scale"
                )
                
                Button(
                    onClick = { 
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        if (studentUsername.isBlank()) {
                            showError = true
                            errorMessage = "Please pick a name!"
                        } else if (studentUsername.length < 3) {
                            showError = true
                            errorMessage = "Name needs 3+ letters!"
                        } else {
                            scope.launch {
                                isLoading = true
                                showError = false
                                focusManager.clearFocus()
                                
                                firestoreService.isUsernameAvailable(studentUsername)
                                    .onSuccess { isAvailable ->
                                        if (!isAvailable) {
                                            showError = true
                                            errorMessage = "Name taken! Try another 🤔"
                                            isLoading = false
                                        } else {
                                            createStudentAccount(studentUsername, firestoreService) { success, _ ->
                                                if (success) {
                                                    showSuccess = true
                                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 400)
                                                    val name = studentUsername
                                                    studentUsername = ""
                                                    scope.launch {
                                                        delay(1500)
                                                        navController.navigate("studentDashboard/$name?tab=0") {
                                                            popUpTo(0) { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    showError = true
                                                    errorMessage = "Oops! Try again 😢"
                                                }
                                                isLoading = false
                                            }
                                        }
                                    }
                                    .onFailure {
                                        showError = true
                                        errorMessage = "Something went wrong!"
                                        isLoading = false
                                    }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .scale(registerScale),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    interactionSource = registerInteraction,
                    enabled = !isLoading && !showSuccess
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(peachOrange, Color(0xFFFFBD59))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🚀", fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Start Adventure!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Back button
                TextButton(
                    onClick = { 
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.popBackStack() 
                    }
                ) {
                    Text("⬅️ Already have an account? Sign In", fontSize = 14.sp, color = Color(0xFF6B4E9B))
                }
            }
        }
    }
}

private suspend fun createStudentAccount(
    username: String,
    firestoreService: FirestoreService,
    callback: (Boolean, String) -> Unit
) {
    val studentUid = "student_${System.currentTimeMillis()}_${username}"
    
    val user = User(
        uid = studentUid,
        email = "",
        username = username,
        displayName = username,
        userType = UserType.STUDENT,
        createdAt = Date()
    )
    
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
        teacherId = null,
        grade = null,
        emoji = null,
        email = null,
        lastActive = null,
        streakDays = 0,
        lastStreakDate = null
    )
    
    firestoreService.createUser(user)
        .onSuccess {
            firestoreService.createStudentProfile(studentProfile)
                .onSuccess { callback(true, "Success!") }
                .onFailure { callback(false, it.message ?: "Error") }
        }
        .onFailure { callback(false, it.message ?: "Error") }
}
