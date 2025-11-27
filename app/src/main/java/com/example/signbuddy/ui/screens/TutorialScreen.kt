package com.example.signbuddy.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.R
import com.example.signbuddy.services.ProgressTrackingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(navController: NavController? = null, username: String = "") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Sound
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var narrationEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
            toneGenerator.release()
        }
    }

    // Letters
    val letters = ('A'..'Z').map { it.toString() }
    var index by remember { mutableStateOf(0) }
    val total = letters.size
    
    // Progress
    val progressTrackingService = remember { ProgressTrackingService() }
    var sessionStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var lettersCompleted by remember { mutableStateOf(0) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressUpdate by remember { mutableStateOf<ProgressTrackingService.ProgressUpdate?>(null) }
    var showCelebration by remember { mutableStateOf(false) }

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
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Letter change animation
    var letterVisible by remember { mutableStateOf(true) }
    LaunchedEffect(index) {
        letterVisible = false
        delay(100)
        letterVisible = true
    }

    // Speak letter
    LaunchedEffect(index, narrationEnabled) {
        if (narrationEnabled) {
            tts?.speak("Letter ${letters[index]}", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Letter images
    val letterImages = mapOf(
        "A" to R.drawable.sign_a, "B" to R.drawable.sign_b, "C" to R.drawable.sign_c,
        "D" to R.drawable.sign_d, "E" to R.drawable.sign_e, "F" to R.drawable.sign_f,
        "G" to R.drawable.sign_g, "H" to R.drawable.sign_h, "I" to R.drawable.sign_i,
        "J" to R.drawable.sign_j, "K" to R.drawable.sign_k, "L" to R.drawable.sign_l,
        "M" to R.drawable.sign_m, "N" to R.drawable.sign_n, "O" to R.drawable.sign_o,
        "P" to R.drawable.sign_p, "Q" to R.drawable.sign_q, "R" to R.drawable.sign_r,
        "S" to R.drawable.sign_s, "T" to R.drawable.sign_t, "U" to R.drawable.sign_u,
        "V" to R.drawable.sign_v, "W" to R.drawable.sign_w, "X" to R.drawable.sign_x,
        "Y" to R.drawable.sign_y, "Z" to R.drawable.sign_z
    )

    // Kindergarten colors
    val sunYellow = Color(0xFFFFE066)
    val skyBlue = Color(0xFF87CEEB)
    val grassGreen = Color(0xFF90EE90)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        sunYellow,
                        Color(0xFFFFF5CC),
                        Color(0xFFFFFFF0)
                    )
                )
            )
    ) {
        // Decorations
        Box(modifier = Modifier.fillMaxSize()) {
            Text("🌞", fontSize = 50.sp, modifier = Modifier.offset(x = 300.dp, y = 20.dp).rotate(wiggleAngle))
            Text("☁️", fontSize = 40.sp, modifier = Modifier.offset(x = 20.dp, y = 30.dp).graphicsLayer { alpha = 0.7f })
            Text("⭐", fontSize = 24.sp, modifier = Modifier.offset(x = 40.dp, y = 200.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 22.sp, modifier = Modifier.offset(x = 340.dp, y = 300.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("✨", fontSize = 20.sp, modifier = Modifier.offset(x = 30.dp, y = 500.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🦋", fontSize = 28.sp, modifier = Modifier.offset(x = 330.dp, y = 550.dp).rotate(wiggleAngle))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    navController?.navigate("studentDashboard/$username") {
                        popUpTo("studentDashboard/{username}") { inclusive = false }
                    }
                }) {
                    Text("⬅️", fontSize = 28.sp)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📚", fontSize = 24.sp, modifier = Modifier.rotate(wiggleAngle))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Learn ABC!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4E37))
                }
                
                IconButton(onClick = {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    narrationEnabled = !narrationEnabled
                }) {
                    Text(if (narrationEnabled) "🔊" else "🔇", fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Card(
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Progress", fontWeight = FontWeight.Bold, color = Color(0xFF5D4E37))
                        }
                        Text(
                            "${index + 1} / $total",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val animatedProgress by animateFloatAsState(
                        targetValue = (index + 1f) / total,
                        animationSpec = tween(500),
                        label = "progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFFE8F5E9)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main letter card
            val currentLetter = letters[index]
            val imageRes = letterImages[currentLetter]

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .offset(y = bounceOffset.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Letter badge
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF9800),
                                        Color(0xFFFFB74D)
                                    )
                                ),
                                CircleShape
                            )
                            .shadow(8.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currentLetter,
                            fontSize = 50.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sign image
                    AnimatedVisibility(
                        visible = letterVisible,
                        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
                        exit = fadeOut(tween(100))
                    ) {
                        if (imageRes != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = imageRes),
                                    contentDescription = "Sign for $currentLetter",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instruction
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤟", fontSize = 26.sp, modifier = Modifier.rotate(wiggleAngle))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Try this sign!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Previous
                val prevInteraction = remember { MutableInteractionSource() }
                val prevPressed by prevInteraction.collectIsPressedAsState()
                val prevScale by animateFloatAsState(
                    targetValue = if (prevPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f),
                    label = "prev"
                )
                
                Button(
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        if (index > 0) index--
                    },
                    enabled = index > 0,
                    modifier = Modifier.weight(1f).height(60.dp).scale(prevScale),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skyBlue,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    interactionSource = prevInteraction
                ) {
                    Text("⬅️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // Next/Finish
                val nextInteraction = remember { MutableInteractionSource() }
                val nextPressed by nextInteraction.collectIsPressedAsState()
                val nextScale by animateFloatAsState(
                    targetValue = if (nextPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f),
                    label = "next"
                )
                
                Button(
                    onClick = {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 150)
                        if (index < total - 1) {
                            lettersCompleted++
                            index++
                        } else {
                            lettersCompleted++
                            showCelebration = true
                            
                            if (username.isNotEmpty()) {
                                scope.launch {
                                    val sessionResult = ProgressTrackingService.SessionResult(
                                        mode = "tutorial",
                                        accuracy = 1.0f,
                                        timeSpent = (System.currentTimeMillis() - sessionStartTime) / 1000,
                                        lettersCompleted = lettersCompleted,
                                        perfectSigns = lettersCompleted,
                                        mistakes = 0
                                    )
                                    progressTrackingService.updateProgress(username, sessionResult)
                                        .onSuccess { update ->
                                            progressUpdate = update
                                            showProgressDialog = true
                                        }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(60.dp).scale(nextScale),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (index < total - 1) grassGreen else Color(0xFFFF9800)
                    ),
                    interactionSource = nextInteraction
                ) {
                    Text(
                        if (index < total - 1) "Next" else "Done!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (index < total - 1) "➡️" else "🎉", fontSize = 24.sp)
                }
            }
        }

        // Celebration overlay
        AnimatedVisibility(
            visible = showCelebration && !showProgressDialog,
            enter = fadeIn() + scaleIn(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp).shadow(24.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉🎊🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Amazing!", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
                        Text("You learned all 26 letters!", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = Color(0xFFFF9800), modifier = Modifier.size(32.dp))
                        Text("Saving...", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Progress dialog
        if (showProgressDialog && progressUpdate != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Great Job!", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row {
                                    Text("⭐ XP: ", fontWeight = FontWeight.Bold)
                                    Text("+${progressUpdate!!.xpGained}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                                Row {
                                    Text("🎯 Score: ", fontWeight = FontWeight.Bold)
                                    Text("+${progressUpdate!!.scoreGained}", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        if (progressUpdate!!.achievementsUnlocked.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("🎖️ Achievements:", fontWeight = FontWeight.Bold)
                            progressUpdate!!.achievementsUnlocked.forEach { id ->
                                val (title, _) = progressTrackingService.getAchievementDetails(id)
                                Text("• $title", fontSize = 14.sp, color = Color(0xFFFF9800))
                            }
                        }
                        
                        if (progressUpdate!!.levelUp) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🚀", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Level Up! Level ${progressUpdate!!.newLevel}", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showProgressDialog = false
                            showCelebration = false
                            navController?.navigate("studentDashboard/$username?tab=0") {
                                popUpTo("tutorial/{username}") { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("🎮 Continue!", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
