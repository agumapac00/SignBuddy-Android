package com.example.signbuddy.ui.screens.tabs

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
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

@Composable
fun LessonsScreen(navController: NavHostController, username: String = "") {
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // Fun animations
    val infiniteTransition = rememberInfiniteTransition(label = "fun")
    
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
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

    // Kindergarten colors - mint green theme
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF98D8C8), // Mint
            Color(0xFFB8E8D8),
            Color(0xFFF0FFF8)
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
            Text("📚", fontSize = 40.sp, modifier = Modifier.offset(x = 300.dp, y = 25.dp).rotate(wiggleAngle))
            Text("☁️", fontSize = 35.sp, modifier = Modifier.offset(x = 20.dp, y = 30.dp).graphicsLayer { alpha = 0.6f })
            Text("⭐", fontSize = 22.sp, modifier = Modifier.offset(x = 50.dp, y = 150.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 20.sp, modifier = Modifier.offset(x = 340.dp, y = 250.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🎮", fontSize = 26.sp, modifier = Modifier.offset(x = 30.dp, y = 550.dp).rotate(wiggleAngle))
            Text("✨", fontSize = 24.sp, modifier = Modifier.offset(x = 320.dp, y = 500.dp).graphicsLayer { alpha = sparkleAlpha })
        }

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
                // Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = bounceOffset.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 36.sp, modifier = Modifier.rotate(wiggleAngle))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Learning Modes",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D6B)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Pick how you want to learn! 🤟",
                            fontSize = 16.sp,
                            color = Color(0xFF5D8B7D)
                        )
                    }
                }

                // Learning Mode Cards
                LessonModeCard(
                    title = "Tutorial",
                    subtitle = "Learn step by step",
                    emoji = "📖",
                    color = Color(0xFF6C63FF),
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("tutorial/$username")
                    }
                )

                LessonModeCard(
                    title = "Practice",
                    subtitle = "Try with your camera",
                    emoji = "🎯",
                    color = Color(0xFF4CAF50),
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("practice/$username")
                    }
                )

                LessonModeCard(
                    title = "Evaluation",
                    subtitle = "Test your skills",
                    emoji = "📝",
                    color = Color(0xFFFF9800),
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("evaluation/$username")
                    }
                )

                LessonModeCard(
                    title = "Multiplayer",
                    subtitle = "Play with friends",
                    emoji = "👫",
                    color = Color(0xFFE91E63),
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        navController.navigate("multiplayer/$username")
                    }
                )
            }
        }
    }
}

@Composable
fun LessonModeCard(
    title: String,
    subtitle: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = onClick,
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 42.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
