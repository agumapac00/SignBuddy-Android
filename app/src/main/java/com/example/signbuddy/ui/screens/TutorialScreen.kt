package com.example.signbuddy.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.services.ProgressTrackingService
import com.example.signbuddy.R
import com.example.signbuddy.ui.components.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(navController: NavController? = null, username: String = "") {
    val context = LocalContext.current
    
    // Gamification elements
    val soundEffects = rememberSoundEffects()
    val hapticFeedback = rememberHapticFeedback()
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var narrationEnabled by remember { mutableStateOf(true) }

    // 🔹 Initialize TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // 🔹 Letters
    val letters = ('A'..'Z').map { it.toString() }
    var index by remember { mutableStateOf(0) }
    val total = letters.size
    
    // Progress tracking
    val progressTrackingService = remember { ProgressTrackingService() }
    var sessionStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var lettersCompleted by remember { mutableStateOf(0) }
    var perfectSigns by remember { mutableStateOf(0) }
    var mistakes by remember { mutableStateOf(0) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressUpdate by remember { mutableStateOf<ProgressTrackingService.ProgressUpdate?>(null) }
    val scope = rememberCoroutineScope()
    val progress = (index + 1).toFloat() / total.toFloat()

    var unlockedBeginnerBadge by remember { mutableStateOf(false) }

    // 🔹 Map letters to images (sign_a, sign_b … sign_z)
    val letterImages = mapOf(
        "A" to R.drawable.sign_a,
        "B" to R.drawable.sign_b,
        "C" to R.drawable.sign_c,
        "D" to R.drawable.sign_d,
        "E" to R.drawable.sign_e,
        "F" to R.drawable.sign_f,
        "G" to R.drawable.sign_g,
        "H" to R.drawable.sign_h,
        "I" to R.drawable.sign_i,
        "J" to R.drawable.sign_j,
        "K" to R.drawable.sign_k,
        "L" to R.drawable.sign_l,
        "M" to R.drawable.sign_m,
        "N" to R.drawable.sign_n,
        "O" to R.drawable.sign_o,
        "P" to R.drawable.sign_p,
        "Q" to R.drawable.sign_q,
        "R" to R.drawable.sign_r,
        "S" to R.drawable.sign_s,
        "T" to R.drawable.sign_t,
        "U" to R.drawable.sign_u,
        "V" to R.drawable.sign_v,
        "W" to R.drawable.sign_w,
        "X" to R.drawable.sign_x,
        "Y" to R.drawable.sign_y,
        "Z" to R.drawable.sign_z
    )

    // 🔹 Speak current letter
    LaunchedEffect(index, narrationEnabled) {
        if (narrationEnabled) {
            tts?.speak(letters[index], TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // 🔹 Animation for image scaling
    val scale = remember { Animatable(1f) }
    LaunchedEffect(index) {
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(200, easing = LinearEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = LinearEasing)
        )
    }

    // 🔹 Background
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 Tutorial: A–Z Signs", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController?.navigate("studentDashboard/$username") {
                            popUpTo("studentDashboard/{username}") { inclusive = false }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Help/Info Button
                    IconButton(
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            // Show help dialog or navigate to help screen
                        }
                    ) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = "Help",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 🔹 Enhanced Header with Mascot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                AnimatedMascot(
                    isHappy = true,
                    isCelebrating = false,
                    size = 48
                )
                Column {
                    Text(
                        text = "📚 Learn the Alphabet!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Let's discover all the letters in sign language! 🌟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            // 🔹 Enhanced Image Display with Card
            val currentLetter = letters[index]
            val imageRes = letterImages[currentLetter]

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Letter $currentLetter",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (imageRes != null) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "Sign for $currentLetter",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((240 * scale.value).dp) // Animate size
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try making this sign with your hands! 🤟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // 🔹 Progress Indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Letter ${index + 1} of $total",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (index + 1).toFloat() / total.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFFC8E6C9)
                    )
                }
            }

            // 🔹 Enhanced Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { 
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        if (index > 0) index-- 
                    },
                    enabled = index > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4ECDC4)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⬅️ Previous", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (narrationEnabled) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    IconToggleButton(
                        checked = narrationEnabled,
                        onCheckedChange = { 
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            narrationEnabled = it 
                        }
                    ) {
                        Icon(
                            imageVector = if (narrationEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Narration Toggle",
                            tint = if (narrationEnabled) Color.White else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        if (index < total - 1) {
                            lettersCompleted++
                            perfectSigns++
                            index++
                        } else {
                            lettersCompleted++
                            perfectSigns++
                            unlockedBeginnerBadge = true
                            
                            // Track progress when tutorial is completed
                            if (username.isNotEmpty()) {
                                android.util.Log.d("TutorialScreen", "=== TUTORIAL COMPLETE ===")
                                android.util.Log.d("TutorialScreen", "Username: $username")
                                android.util.Log.d("TutorialScreen", "Letters completed: $lettersCompleted")
                                android.util.Log.d("TutorialScreen", "Perfect signs: $perfectSigns")
                                android.util.Log.d("TutorialScreen", "Mistakes: $mistakes")
                                
                                scope.launch {
                                    val sessionResult = ProgressTrackingService.SessionResult(
                                        mode = "tutorial",
                                        accuracy = 1.0f, // Tutorial is always 100% as it's just learning
                                        timeSpent = (System.currentTimeMillis() - sessionStartTime) / 1000,
                                        lettersCompleted = lettersCompleted,
                                        perfectSigns = perfectSigns,
                                        mistakes = mistakes
                                    )
                                    
                                    android.util.Log.d("TutorialScreen", "Calling updateProgress with username: $username")
                                    
                                    progressTrackingService.updateProgress(username, sessionResult)
                                        .onSuccess { update ->
                                            android.util.Log.d("TutorialScreen", "✅ Progress updated successfully!")
                                            android.util.Log.d("TutorialScreen", "Achievements unlocked: ${update.achievementsUnlocked}")
                                            android.util.Log.d("TutorialScreen", "XP gained: ${update.xpGained}")
                                            android.util.Log.d("TutorialScreen", "Score gained: ${update.scoreGained}")
                                            
                                            if (update.achievementsUnlocked.isNotEmpty()) {
                                                android.util.Log.d("TutorialScreen", "🎉 ${update.achievementsUnlocked.size} achievements unlocked!")
                                                update.achievementsUnlocked.forEach { achievementId ->
                                                    android.util.Log.d("TutorialScreen", "  - $achievementId")
                                                }
                                            } else {
                                                android.util.Log.w("TutorialScreen", "⚠️ No achievements unlocked!")
                                            }
                                            
                                            progressUpdate = update
                                            showProgressDialog = true
                                        }
                                        .onFailure { exception ->
                                            android.util.Log.e("TutorialScreen", "❌ Failed to update progress", exception)
                                            exception.printStackTrace()
                                        }
                                }
                            } else {
                                android.util.Log.w("TutorialScreen", "⚠️ Username is empty! Cannot save progress.")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (index < total - 1) "Next ➡️" else "Finish 🎉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // 🔹 Badge Unlock Dialog
            if (unlockedBeginnerBadge) {
                AlertDialog(
                    onDismissRequest = { unlockedBeginnerBadge = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                unlockedBeginnerBadge = false
                                navController?.navigate("studentDashboard/$username") {
                                    popUpTo("tutorial") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    title = {
                        Text("🥇 Beginner Badge Unlocked!")
                    },
                    text = {
                        Text("Congrats on completing the tutorial!")
                    }
                )
            }
        }
    }
    
    // Progress Update Dialog
    if (showProgressDialog && progressUpdate != null) {
        AlertDialog(
            onDismissRequest = { showProgressDialog = false },
            title = { Text("🎉 Great Job!") },
            text = {
                Column {
                    Text("You completed the tutorial!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("XP Gained: ${progressUpdate!!.xpGained}")
                    Text("Score Gained: ${progressUpdate!!.scoreGained}")
                    if (progressUpdate!!.achievementsUnlocked.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Achievements Unlocked:")
                        progressUpdate!!.achievementsUnlocked.forEach { achievementId ->
                            val (title, description) = progressTrackingService.getAchievementDetails(achievementId)
                            Text("• $title: $description", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (progressUpdate!!.levelUp) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🎊 Level Up! You're now level ${progressUpdate!!.newLevel}!", 
                            color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showProgressDialog = false
                        navController?.popBackStack()
                    }
                ) {
                    Text("Continue")
                }
            }
        )
    }
}