package com.example.signbuddy.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.DashPathEffect
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.ExtraBold
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.example.signbuddy.ui.components.*
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.signbuddy.services.ProgressTrackingService
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationTestScreen(navController: NavController? = null, username: String = "") {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Kindergarten-friendly gradient
    val gradient = Brush.verticalGradient(colors = listOf(
        Color(0xFFFFE0B2), // Warm orange
        Color(0xFFFFF8E1), // Cream
        Color(0xFFE8F5E8), // Light green
        Color(0xFFE3F2FD)  // Light blue
    ))
    
    // Gamification elements
    val soundEffects = rememberSoundEffects()
    val hapticFeedback = rememberHapticFeedback()
    var showEncouragingMessage by remember { mutableStateOf(false) }
    var encouragingText by remember { mutableStateOf("") }
    var streak by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var showAchievement by remember { mutableStateOf(false) }
    var currentAchievement by remember { mutableStateOf<com.example.signbuddy.ui.components.Achievement?>(null) }
    var progressData by remember { mutableStateOf(ProgressData()) }
    val scope = rememberCoroutineScope()

    // Permissions
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    // Camera preview view
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() } // background thread for CameraX analyzers

    // Screen navigation state
    var showLevelSelection by remember { mutableStateOf(true) }

    // Camera & practice states (extended)
    var useFrontCamera by remember { mutableStateOf(true) }
    var selectedLevel by remember { mutableStateOf("Easy") }
    var isPracticing by remember { mutableStateOf(false) }

    // Test queue and progression
    var letterQueue by remember { mutableStateOf(mutableStateListOf<String>()) }
    var currentLetter by remember { mutableStateOf("") }
    var initialTotalAttempts by remember { mutableStateOf(0) }

    // Timer (10s per letter)
    var timeLeft by remember { mutableStateOf(10) }

    // Scoring & stats
    var correctCount by remember { mutableStateOf(0) }
    var wrongCount by remember { mutableStateOf(0) }
    var mistakes by remember { mutableStateOf(mutableStateListOf<String>()) }

    // Current prediction & feedback
    var currentPrediction by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackCorrect by remember { mutableStateOf(false) }

    // End dialog
    var showEndDialog by remember { mutableStateOf(false) }
    
    // Progress tracking
    val progressTrackingService = remember { ProgressTrackingService() }
    var sessionStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressUpdate by remember { mutableStateOf<ProgressTrackingService.ProgressUpdate?>(null) }

    // Load model off the UI thread and store interpreter in state
    var modelInterpreter by remember { mutableStateOf<Interpreter?>(null) }
    var handSignAnalyzer by remember { mutableStateOf<EvaluationHandSignAnalyzer?>(null) }
    var shouldStopAnalysis by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Loads model on IO dispatcher
        modelInterpreter = withContext(Dispatchers.IO) { loadPracticeModel(context) }
    }
    
    // Cleanup analyzer when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            shouldStopAnalysis = true
            handSignAnalyzer?.cleanup()
        }
    }

    // Use 640 if that's your trained imgsz; change to 320 if you want faster inference
    val inputSize = 640
    val imageProcessor = remember {
        ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    // Audio feedback
    val correctToneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val incorrectToneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val coroutineScope = rememberCoroutineScope()

    // Auto-hide feedback overlay after a short delay
    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            delay(1500)
            showFeedback = false
        }
    }

    // Save progress immediately when evaluation completes (before user clicks Done)
    LaunchedEffect(showEndDialog) {
        if (showEndDialog && username.isNotEmpty()) {
            // Save progress immediately so it persists even if user closes app
            android.util.Log.d("EvaluationTestScreen", "=== AUTO-SAVING PROGRESS ===")
            android.util.Log.d("EvaluationTestScreen", "Correct count: $correctCount")
            android.util.Log.d("EvaluationTestScreen", "Letters completed: $correctCount")
            
            try {
                val sessionResult = ProgressTrackingService.SessionResult(
                    mode = "evaluation",
                    accuracy = if (initialTotalAttempts > 0) {
                        (correctCount.toFloat() / initialTotalAttempts.toFloat()).coerceAtMost(1.0f)
                    } else 0f,
                    timeSpent = if (sessionStartTime > 0) {
                        (System.currentTimeMillis() - sessionStartTime) / 1000
                    } else 0,
                    lettersCompleted = correctCount,
                    perfectSigns = correctCount,
                    mistakes = wrongCount,
                    actualScore = score // Pass the actual score earned in evaluation
                )
                
                android.util.Log.d("EvaluationTestScreen", "Calling updateProgress with actualScore=$score, lettersCompleted=$correctCount")
                val result = progressTrackingService.updateProgress(username, sessionResult)
                result.onSuccess { update ->
                    android.util.Log.d("EvaluationTestScreen", "✅ Progress saved successfully!")
                    android.util.Log.d("EvaluationTestScreen", "Achievements unlocked: ${update.achievementsUnlocked}")
                    progressUpdate = update
                }.onFailure { error ->
                    android.util.Log.e("EvaluationTestScreen", "❌ Failed to save progress", error)
                }
            } catch (e: Exception) {
                android.util.Log.e("EvaluationTestScreen", "Exception during progress save", e)
            }
        }
    }

    // Helper: prepare a new test session based on selected level
    fun startNewSession(level: String) {
        val pool = when (level) {
            "Easy" -> listOf('A', 'B', 'C', 'O', 'S', 'L', 'D', 'E')
            "Average" -> listOf('F', 'M', 'N', 'P', 'U', 'T', 'I', 'J')
            "Difficult" -> listOf('X', 'Y', 'Z', 'Q', 'R', 'V', 'W', 'K', 'G', 'H')
            else -> listOf('A', 'B', 'C', 'O', 'S', 'L', 'D', 'E')
        }
        val count = when (level) {
            "Easy" -> 10
            "Average" -> 15
            "Difficult" -> 10
            else -> 10
        }
        // Make sure we can sample; allow repeats if count > pool.size
        val expanded = mutableListOf<String>()
        val shuffledPool = pool.map { it.toString() }.shuffled().toMutableList()
        while (expanded.size < count) {
            if (shuffledPool.isEmpty()) shuffledPool.addAll(pool.map { it.toString() }.shuffled())
            expanded.add(shuffledPool.removeAt(0))
        }
        letterQueue.clear()
        letterQueue.addAll(expanded)
        initialTotalAttempts = expanded.size
        score = 0
        correctCount = 0
        wrongCount = 0
        mistakes.clear()
        streak = 0 // Reset streak when starting new session
        // set first
        currentLetter = if (letterQueue.isNotEmpty()) letterQueue[0] else ""
        timeLeft = 10
    }

    // Coroutine-driven timer per currentLetter
    LaunchedEffect(isPracticing, currentLetter) {
        if (!isPracticing || currentLetter.isEmpty()) return@LaunchedEffect
        timeLeft = 10
        val startMillis = System.currentTimeMillis()
        while (isPracticing && timeLeft > 0 && this.isActive) {
            delay(1000)
            timeLeft -= 1
        }
        // time ran out for current letter
        if (isPracticing && timeLeft <= 0) {
            // treat as wrong (no points) and move to next
            wrongCount += 1
            streak = 0 // Reset streak when time runs out
            if (!mistakes.contains(currentLetter)) mistakes.add(currentLetter)
            // remove first element
            if (letterQueue.isNotEmpty()) letterQueue.removeAt(0)
            // advance
            if (letterQueue.isEmpty()) {
                isPracticing = false
                showEndDialog = true
            } else {
                currentLetter = letterQueue[0]
                timeLeft = 10
            }
        }
    }

    // CameraX setup: runs only when permission granted, practicing enabled, model loaded, and on evaluation screen
    LaunchedEffect(hasPermission, useFrontCamera, isPracticing, modelInterpreter, showLevelSelection) {
        if (!hasPermission || !isPracticing || modelInterpreter == null || showLevelSelection) return@LaunchedEffect

        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        // ImageAnalysis: keep latest frames only and analyze on background executor
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(inputSize, inputSize))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Use the analyzer on the background executor
        val analyzer = EvaluationHandSignAnalyzer(
            modelInterpreter = modelInterpreter,
            imageProcessor = imageProcessor,
            inputSize = inputSize,
            useFrontCamera = useFrontCamera,
            context = context,
            shouldStop = { shouldStopAnalysis },
            onPrediction = { prediction: String ->
                // update UI states based on prediction; only act on non-empty string
                currentPrediction = prediction
                if (!prediction.isNullOrEmpty() && currentLetter.isNotEmpty() && isPracticing && !showFeedback) {
                    // normalize
                    val pred = prediction.uppercase()
                    val target = currentLetter.uppercase()
                    if (pred == target) {
                        // Correct: compute time-based bonus
                        val basePoints = 10
                        val bonus = if (timeLeft >= 6) 5 else 0 // faster: if solved within first half (>=6s left) give +5
                        val gained = basePoints + bonus
                        score += gained
                        correctCount += 1
                        // Update streak: increment on correct
                        streak += 1
                        feedbackCorrect = true
                        correctToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                        showFeedback = true
                        // delay 1.5s before advancing to next letter
                        coroutineScope.launch {
                            delay(1500)
                            // remove from queue and advance if still in session
                            if (letterQueue.isNotEmpty()) letterQueue.removeAt(0)
                            if (letterQueue.isEmpty()) {
                                isPracticing = false
                                showEndDialog = true
                            } else {
                                currentLetter = letterQueue[0]
                                timeLeft = 10
                            }
                        }
                    } else {
                        // Wrong: 0 points, proceed to next
                        wrongCount += 1
                        // Reset streak on wrong answer
                        streak = 0
                        feedbackCorrect = false
                        incorrectToneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 200)
                        showFeedback = true
                        if (!mistakes.contains(currentLetter)) mistakes.add(currentLetter)
                        if (letterQueue.isNotEmpty()) letterQueue.removeAt(0)
                        if (letterQueue.isEmpty()) {
                            isPracticing = false
                            showEndDialog = true
                        } else {
                            currentLetter = letterQueue[0]
                            timeLeft = 10
                        }
                    }
                }
            }
        )
        
        // Store analyzer instance for cleanup
        handSignAnalyzer = analyzer
        
        imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📝 Evaluation Mode", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { 
                        shouldStopAnalysis = true
                        showFeedback = false
                        if (showLevelSelection) {
                        navController?.navigate("studentDashboard/$username") {
                            popUpTo("studentDashboard/{username}") { inclusive = false }
                            launchSingleTop = true
                        }
                        } else {
                            isPracticing = false
                            showLevelSelection = true
                        }
                    }) {
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
            // Gamification overlays
            if (showEncouragingMessage) {
                FloatingMessage(
                    message = encouragingText,
                    isVisible = showEncouragingMessage,
                    onComplete = { showEncouragingMessage = false }
                )
            }
            
            if (showAchievement && currentAchievement != null) {
                AchievementUnlockAnimation(
                    achievement = currentAchievement!!,
                    onComplete = { 
                        showAchievement = false
                        currentAchievement = null
                    }
                )
            }
            // Screen 1: Level Selection
            if (showLevelSelection) {
                EvaluationLevelSelectionScreen(
                    selectedLevel = selectedLevel,
                    onLevelSelected = { level ->
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        selectedLevel = level
                        score = 0
                        streak = 0
                    },
                    onContinue = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        showLevelSelection = false
                    },
                    onBackToDashboard = {
                        shouldStopAnalysis = true
                        navController?.navigate("studentDashboard/$username") {
                            popUpTo("studentDashboard/{username}") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    gradient = gradient
                )
            } else {
                // Screen 2: Evaluation Interface
                EvaluationInterfaceScreen(
                    selectedLevel = selectedLevel,
                    currentLetter = currentLetter,
                    timeLeft = timeLeft,
                    score = score,
                    streak = streak,
                    initialTotalAttempts = initialTotalAttempts,
                    letterQueueSize = letterQueue.size,
                    useFrontCamera = useFrontCamera,
                    isPracticing = isPracticing,
                    showFeedback = showFeedback,
                    feedbackCorrect = feedbackCorrect,
                    currentPrediction = currentPrediction,
                    previewView = previewView,
                    context = context,
                    soundEffects = soundEffects,
                    hapticFeedback = hapticFeedback,
                    onCameraSwitch = { useFrontCamera = !useFrontCamera },
                    onSkip = {
                        if (letterQueue.isNotEmpty()) {
                            val first = letterQueue.removeAt(0)
                            letterQueue.add(first)
                            currentLetter = letterQueue[0]
                            timeLeft = 10
                        }
                    },
                    onStartStop = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        if (!isPracticing) {
                            startNewSession(selectedLevel)
                            isPracticing = true
                        } else {
                            isPracticing = false
                            showEndDialog = true
                        }
                    },
                    onReset = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        isPracticing = false
                        letterQueue.clear()
                        currentLetter = ""
                        score = 0
                        correctCount = 0
                        wrongCount = 0
                        mistakes.clear()
                        streak = 0 // Reset streak on reset
                        timeLeft = 10
                    },
                    gradient = gradient
                )
            }

            if (showEndDialog) {
                AlertDialog(
                    onDismissRequest = { showEndDialog = false },
                    title = { Text("Session Results") },
                    text = {
                        val accuracy = if (initialTotalAttempts > 0) (correctCount * 100 / initialTotalAttempts) else 0
                        Column {
                            Text("Score: $score")
                            Text("Correct: $correctCount")
                            Text("Wrong: $wrongCount")
                            Text("Accuracy: $accuracy%")
                            if (mistakes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Mistakes:")
                                mistakes.forEach { m -> Text(m) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showEndDialog = false
                            // Progress already auto-saved when session ended
                            // Just show progress dialog if we have updates
                            if (progressUpdate != null) {
                                showProgressDialog = true
                            }
                            // reset session to allow replay
                            isPracticing = false
                            letterQueue.clear()
                            currentLetter = ""
                            score = 0
                            correctCount = 0
                            wrongCount = 0
                            mistakes.clear()
                            streak = 0 // Reset streak when done
                            timeLeft = 10
                        }) { Text("Done") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showEndDialog = false
                        }) { Text("Close") }
                    }
                )
            }
            
            // Progress Update Dialog
            if (showProgressDialog && progressUpdate != null) {
                AlertDialog(
                    onDismissRequest = { showProgressDialog = false },
                    title = { Text("🎉 Great Job!") },
                    text = {
                        Column {
                            Text("You completed the evaluation test!")
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
                                shouldStopAnalysis = true
                                navController?.navigate("studentDashboard/$username?tab=1") {
                                    popUpTo("studentDashboard/{username}") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text("Continue")
                        }
                    }
                )
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            correctToneGenerator.release()
            incorrectToneGenerator.release()
            modelInterpreter?.close()
            cameraExecutor.shutdown()
        }
    }
}

// Screen 1: Level Selection Screen - Compact, fits on one page
@Composable
fun EvaluationLevelSelectionScreen(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onBackToDashboard: () -> Unit,
    gradient: Brush
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        // Compact Header
        AnimatedMascot(
            isHappy = true,
            isCelebrating = false,
            size = 50
        )
        Spacer(modifier = Modifier.height(8.dp))
                Text(
            text = "📝 Evaluation Mode",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose your test level!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        
        // Compact Level selection cards
        Text(
            text = "🎯 Select Difficulty",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
        Spacer(modifier = Modifier.height(10.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                    listOf(
                LevelOption("Easy", "🟢", Color(0xFF4CAF50), "Perfect for beginners!"),
                LevelOption("Average", "🟡", Color(0xFFFF9800), "Ready for a challenge?"),
                LevelOption("Difficult", "🔴", Color(0xFFF44336), "Master level!")
            ).forEach { (lvl, emoji, color, description) ->
                        val isSelected = lvl == selectedLevel
                        Card(
                            modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) color else color.copy(alpha = 0.1f)
                            ),
                            elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 10.dp else 4.dp
                            ),
                            shape = RoundedCornerShape(16.dp),
                    onClick = { onLevelSelected(lvl) }
                ) {
                    Row(
                                modifier = Modifier
                                    .fillMaxSize()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 32.sp)
                            Column {
                                Text(
                                    text = lvl,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) Color.White else color,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF666666),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Continue button
                Card(
                    modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = onContinue
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                    text = "Continue ➡️",
                    color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
            }
        }
    }
}

// Screen 2: Evaluation Interface Screen - Compact, fits on one page
@Composable
fun EvaluationInterfaceScreen(
    selectedLevel: String,
    currentLetter: String,
    timeLeft: Int,
    score: Int,
    streak: Int,
    initialTotalAttempts: Int,
    letterQueueSize: Int,
    useFrontCamera: Boolean,
    isPracticing: Boolean,
    showFeedback: Boolean,
    feedbackCorrect: Boolean,
    currentPrediction: String,
    previewView: PreviewView,
    context: Context,
    soundEffects: com.example.signbuddy.ui.components.SoundEffectsManager,
    hapticFeedback: com.example.signbuddy.ui.components.HapticFeedbackManager,
    onCameraSwitch: () -> Unit,
    onSkip: () -> Unit,
    onStartStop: () -> Unit,
    onReset: () -> Unit,
    gradient: Brush
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Compact Target letter card with Timer, Letter, Score, and Streak
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                        Text(
                    text = "🎯 Show the sign for",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Timer, Letter, Streak, and Score in a row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                    // Timer card - Left side (smaller)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                            modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                            Text("⏱️", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${timeLeft}s",
                                fontSize = 14.sp,
                                        fontWeight = Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                            
                    // Letter display - Center
                            Box(
                                modifier = Modifier
                            .size(50.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                            )
                                        ),
                                shape = RoundedCornerShape(25.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentLetter.ifEmpty { "-" }, 
                            fontSize = 32.sp,
                                    fontWeight = ExtraBold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                    // Streak and Score - Right side (side by side, streak moved left)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp), // Reduced spacing to move streak left
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Streak card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔥", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "$streak",
                                    fontSize = 14.sp,
                                    fontWeight = Bold,
                                    color = Color(0xFFFF6F00)
                                )
                            }
                        }
                        // Score card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🏆", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "$score",
                                    fontSize = 14.sp,
                                    fontWeight = Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                        }
                        
                Spacer(modifier = Modifier.height(6.dp))
                        
                        // Progress indicator
                        Text(
                    text = "Progress: ${if (initialTotalAttempts > 0) (initialTotalAttempts - letterQueueSize) else 0} / $initialTotalAttempts",
                    fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
        
        // Camera preview - larger but still fits one page
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                .height(277.dp)
                .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = {
                            val frame = FrameLayout(context).apply {
                                // Remove previewView from its current parent if it has one
                                val currentParent = previewView.parent as? ViewGroup
                                if (currentParent != null && currentParent != this) {
                                    currentParent.removeView(previewView)
                                }
                                // Only add if not already a child
                                if (previewView.parent == null) {
                                    addView(
                                        previewView,
                                        FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT
                                        )
                                    )
                                }
                                // Add overlay (create new one each time as it's lightweight)
                                val overlay = EvaluationOverlayView(context).apply {
                                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                                }
                                addView(overlay)
                            }
                            frame
                        },
                        modifier = Modifier.matchParentSize(),
                        update = { view ->
                            // Update handler - ensure previewView is in the frame
                            val frame = view as FrameLayout
                            if (previewView.parent != frame && previewView.parent != null) {
                                (previewView.parent as? ViewGroup)?.removeView(previewView)
                            }
                            if (previewView.parent == null) {
                                frame.addView(
                                    previewView,
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        }
                    )
                    if (showFeedback) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (feedbackCorrect) {
                                    AnimatedMascot(
                                        isHappy = true,
                                        isCelebrating = true,
                                size = 40
                                    )
                            Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = if (feedbackCorrect) "✅ Correct!" else "❌ Try again!",
                            fontSize = 16.sp,
                                    color = if (feedbackCorrect) Color.Green else Color.Red,
                                    fontWeight = Bold
                                )
                        Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Detected: $currentPrediction",
                            fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
        
        // Compact Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Skip
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(10.dp),
                onClick = onSkip
                    ) {
                        Column(
                    modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                    Text("⏭️  Skip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            // Camera switch
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(10.dp),
                onClick = onCameraSwitch
                    ) {
                        Column(
                    modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                        text = if (useFrontCamera) "📷  Back" else "📷  Front",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                            )
                        }
                    }
                }

        // Start/Stop button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPracticing) Color(0xFFF44336) else Color(0xFF4CAF50)
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(12.dp),
            onClick = onStartStop
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPracticing) "⏹️" else "▶️",
                    fontSize = 18.sp
                        )
                Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPracticing) "Stop Evaluation" else "Start Evaluation",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        
                    // Reset button
                    Card(
            modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF607D8B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(10.dp),
            onClick = onReset
                    ) {
                        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔄", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// (Removed duplicate LevelOption data class; using the single definition in PracticeScreen.kt)

// Load TFLite model from assets (CPU-only)
private fun loadPracticeModel(context: Context): Interpreter? {
    return try {
        val model = loadModelFile(context, "asl_model.tflite")
        Interpreter(model).also {
            val inputShape = it.getInputTensor(0).shape()
            val outputShape = it.getOutputTensor(0).shape()
            Log.d("PracticeScreen", "Model loaded. Input: ${'$'}{inputShape.contentToString()}, Output: ${'$'}{outputShape.contentToString()}")
        }
    } catch (e: Exception) {
        Log.e("PracticeScreen", "Failed to load model", e)
        null
    }
}

private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd(modelPath)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

// (Removed duplicate toBitmap extension; using the single definition in PracticeScreen.kt)

// Function to horizontally flip bitmap for front camera mirror effect
private fun flipHorizontally(bitmap: Bitmap): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1f, 1f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Hand sign analyzer for YOLOv8-style TFLite (post-processing + confidence threshold 0.75)
class EvaluationHandSignAnalyzer(
    private val modelInterpreter: Interpreter?,
    private val imageProcessor: ImageProcessor,
    private val inputSize: Int,
    private val useFrontCamera: Boolean,
    private val context: Context,
    private val shouldStop: () -> Boolean,
    private val onPrediction: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val handler = Handler(Looper.getMainLooper())
    private var lastAnalysisTime = 0L
    private val analysisInterval = 300L // ms
    private val TAG = "HandSignAnalyzer"
    // ASL labels: 0-25 A-Z, 26 nothing
    private val labels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "nothing")
    // Model params (adapt if your model differs)
    private val numClasses = 27
    private val numFeatures = 4 + numClasses // 31
    private val numDetections = 8400
    private val confThreshold = 0.3f // raw detection threshold
    private val feedbackThreshold = 0.60f // FINAL confidence threshold (user requested 75%)
    private val iouThreshold = 0.5f

    // Reusable buffers to reduce allocations and GC pressure
    private val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val outputArray3D = Array(1) { Array(numFeatures) { FloatArray(numDetections) } }
    private val pixels = IntArray(inputSize * inputSize)
    private val inputArray = FloatArray(inputSize * inputSize * 3)
    
    // Thread safety for interpreter access
    private val interpreterLock = Any()

    override fun analyze(image: ImageProxy) {
        try {
            // Check if we should stop analysis
            if (shouldStop()) {
                image.close()
                return
            }
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAnalysisTime < analysisInterval) {
                return
            }
            lastAnalysisTime = currentTime

            if (modelInterpreter == null) {
                Log.e(TAG, "Model interpreter is null")
                handler.post { onPrediction("") }
                return
            }
            // Handle rotation and flip for front camera
            val rotationDegrees = if (useFrontCamera) 270 else 0
            var bitmap = image.toBitmap(rotationDegrees)
            if (useFrontCamera) {
                bitmap = flipHorizontally(bitmap)
            }

            // Prepare TensorImage and process
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)

            // Prepare float input buffer (NHWC float [0,1] normalized)
            inputBuffer.rewind()
            processedImage.bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            var floatIndex = 0
            for (pixel in pixels) {
                val r = (pixel shr 16 and 0xFF) / 255.0f
                val g = (pixel shr 8 and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                inputArray[floatIndex++] = r
                inputArray[floatIndex++] = g
                inputArray[floatIndex++] = b
            }
            inputBuffer.asFloatBuffer().put(inputArray)

            // Run inference (output shape expected [1, numFeatures, numDetections])
            // Use synchronized block to prevent concurrent access
            var inferenceSucceeded = false
            
            // Check again before running expensive inference
            if (!shouldStop()) {
                synchronized(interpreterLock) {
                    // Double-check after acquiring lock
                    if (!shouldStop()) {
                        try {
                            modelInterpreter.run(inputBuffer, outputArray3D)
                            inferenceSucceeded = true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during model inference", e)
                            handler.post { onPrediction("") }
                        }
                    }
                }
            }
            
            // If inference failed or we should stop, exit early
            if (!inferenceSucceeded || shouldStop()) {
                handler.post { onPrediction("") }
                return
            }

            // Flatten output for processing
            val outputFlat = FloatArray(numFeatures * numDetections)
            var idx = 0
            for (batch in outputArray3D) {
                for (feat in batch) {
                    for (det in feat) {
                        outputFlat[idx++] = det
                    }
                }
            }

            // Post-process: decode detections
            val boundingBoxes = mutableListOf<BoundingBox>()
            for (c in 0 until numDetections) {
                var maxConf = 0f
                var maxIdx = -1
                var j = 4
                var arrayIdx = c + numDetections * j
                while (j < numFeatures) {
                    if (outputFlat[arrayIdx] > maxConf) {
                        maxConf = outputFlat[arrayIdx]
                        maxIdx = j - 4
                    }
                    j++
                    arrayIdx += numDetections
                }
                if (maxConf > confThreshold && maxIdx != 26) { // ignore "nothing"
                    val cx = outputFlat[c]
                    val cy = outputFlat[c + numDetections]
                    val w = outputFlat[c + numDetections * 2]
                    val h = outputFlat[c + numDetections * 3]
                    val x1 = cx - (w / 2f)
                    val y1 = cy - (h / 2f)
                    val x2 = cx + (w / 2f)
                    val y2 = cy + (h / 2f)
                    if (x1 >= 0f && y1 >= 0f && x2 <= 1f && y2 <= 1f) {
                        val clsName = labels[maxIdx]
                        boundingBoxes.add(BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName))
                    }
                }
            }

            if (boundingBoxes.isEmpty()) {
                return
            }

            // Apply NMS
            val selectedBoxes = applyNMS(boundingBoxes)

            // Pick highest conf box
            val bestBox = selectedBoxes.maxByOrNull { it.cnf }
            val predictedLetter = if (bestBox != null && bestBox.cnf > feedbackThreshold) bestBox.clsName else ""
            Log.d(TAG, "Final prediction: ${'$'}predictedLetter (conf: ${'$'}{bestBox?.cnf ?: 0f})")
            handler.post { onPrediction(predictedLetter) }

    } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during analysis", e)
            // Force garbage collection
            System.gc()
            handler.post { onPrediction("") }
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyze", e)
            handler.post { onPrediction("") }
        } finally {
            image.close()
        }
    }

    data class BoundingBox(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val cx: Float, val cy: Float, val w: Float, val h: Float,
        val cnf: Float, val cls: Int, val clsName: String
    )

    private fun applyNMS(boxes: List<BoundingBox>): List<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()
        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.removeAt(0)
            selectedBoxes.add(first)
            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val interArea = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val unionArea = (box1.x2 - box1.x1) * (box1.y2 - box1.y1) + (box2.x2 - box2.x1) * (box2.y2 - box2.y1) - interArea
        return if (unionArea > 0) interArea / unionArea else 0f
    }
    
    /**
     * Clean up resources to prevent memory leaks
     */
    fun cleanup() {
        try {
            // Clear buffers to free memory
            inputBuffer.clear()
            inputArray.fill(0f)
            pixels.fill(0)
            
            // Clear output array
            for (batch in outputArray3D) {
                for (feat in batch) {
                    feat.fill(0f)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

// Custom overlay view with static guide box at center
class EvaluationOverlayView(context: Context) : View(context) {
    private val paintGuide = Paint().apply {
        color = android.graphics.Color.argb(150, 173, 216, 230) // Semi-transparent light blue
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f) // Dashed for guide
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate() // Ensure draw on layout
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        // Draw static guide box at center (40% of view size for hand fitting)
        val guideSize = 0.4f * min(width, height).toFloat()
        val guideLeft = (width / 2f - guideSize / 2f)
        val guideTop = (height / 2f - guideSize / 2f)
        val guideRight = guideLeft + guideSize
        val guideBottom = guideTop + guideSize
        canvas.drawRect(guideLeft, guideTop, guideRight, guideBottom, paintGuide)
    }
}
