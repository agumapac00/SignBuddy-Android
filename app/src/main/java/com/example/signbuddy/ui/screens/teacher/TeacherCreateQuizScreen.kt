package com.example.signbuddy.ui.screens.teacher

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherCreateQuizScreen(navController: NavController? = null) {
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
        colors = listOf(Color(0xFFFFF59D), Color(0xFFFFFDE7), Color(0xFFFFF5F5))
    )

    var showConfetti by remember { mutableStateOf(false) }
    var quizTitle by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📝", fontSize = 28.sp, modifier = Modifier.rotate(wiggleAngle))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Quiz", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController?.popBackStack()
                    }) { Text("⬅️", fontSize = 24.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFBC02D),
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
            Text("✏️", fontSize = 28.sp, modifier = Modifier.offset(x = 320.dp, y = 80.dp).rotate(wiggleAngle))
            Text("📚", fontSize = 26.sp, modifier = Modifier.offset(x = 320.dp, y = 450.dp).rotate(-wiggleAngle))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 50 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
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
                            Text("📝", fontSize = 48.sp, modifier = Modifier.rotate(wiggleAngle))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Quiz Builder", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF9A825))
                            Text("Create custom quizzes for your class! ✨", fontSize = 14.sp, color = Color.Gray)
                        }
                    }

                    // Form Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("📋 Quiz Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF9A825))
                            Text("Build a custom quiz with title and questions", fontSize = 13.sp, color = Color.Gray)
                            
                            OutlinedTextField(
                                value = quizTitle,
                                onValueChange = { quizTitle = it },
                                label = { Text("📌 Quiz Title") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFBC02D),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                            
                            OutlinedTextField(
                                value = instructions,
                                onValueChange = { instructions = it },
                                label = { Text("📝 Instructions (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFBC02D),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                            
                            // Quick Actions
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50) },
                                    label = { Text("Add A-Z") }
                                )
                                AssistChip(
                                    onClick = { toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50) },
                                    label = { Text("Randomize") }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val saveInteraction = remember { MutableInteractionSource() }
                            val savePressed by saveInteraction.collectIsPressedAsState()
                            val saveScale by animateFloatAsState(
                                targetValue = if (savePressed) 0.95f else 1f,
                                animationSpec = spring(dampingRatio = 0.5f), label = "save"
                            )
                            
                            Button(
                                onClick = {
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                                    showConfetti = true
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp).scale(saveScale),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                interactionSource = saveInteraction
                            ) {
                                Text("💾", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Draft", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Back Button
                    Button(
                        onClick = {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                            navController?.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                    ) {
                        Text("⬅️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            com.example.signbuddy.ui.screens.teacher.components.ConfettiOverlay(visible = showConfetti) { showConfetti = false }
        }
    }
}
