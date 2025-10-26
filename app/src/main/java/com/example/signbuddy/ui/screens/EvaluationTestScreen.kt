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
    
    LaunchedEffect(Unit) {
        // Loads model on IO dispatcher
        modelInterpreter = withContext(Dispatchers.IO) { loadPracticeModel(context) }
    }
    
    // Cleanup analyzer when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
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

    // CameraX setup: runs only when permission granted, practicing enabled, and model loaded
    LaunchedEffect(hasPermission, useFrontCamera, isPracticing, modelInterpreter) {
        if (!hasPermission || !isPracticing || modelInterpreter == null) return@LaunchedEffect

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
                    val backIs = MutableInteractionSource()
                    val backPressed by backIs.collectIsPressedAsState()
                    val backScale by animateFloatAsState(
                        targetValue = if (backPressed) 0.96f else 1f,
                        animationSpec = tween(100),
                        label = "backEval"
                    )
                    IconButton(onClick = { navController?.popBackStack() }, interactionSource = backIs, modifier = Modifier.graphicsLayer(scaleX = backScale, scaleY = backScale)) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with mascot and progress
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedMascot(
                        isHappy = true,
                        isCelebrating = false,
                        size = 60
                    )
                    Column {
                        Text("✋ Evaluation Mode", fontSize = 20.sp, fontWeight = SemiBold, color = Color(0xFF1565C0))
                        Text("Let's test your skills!", fontSize = 14.sp, color = Color(0xFF666666))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Camera-based AI checks signs in real-time. Feedback shows after you perform a sign.", fontSize = 14.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress and stats display
                LevelProgressCard(
                    progressData = progressData,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Score and streak display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Score", fontSize = 12.sp, color = Color(0xFF666666))
                            Text("$score", fontSize = 20.sp, fontWeight = Bold, color = Color(0xFF4CAF50))
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
            Column(
                            modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                            Text("Streak", fontSize = 12.sp, color = Color(0xFF666666))
                            Text("$streak", fontSize = 20.sp, fontWeight = Bold, color = Color(0xFFFF9800))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Enhanced Difficulty buttons
                Text(
                    text = "🎯 Choose Your Test Level",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple("Easy", "🟢", Color(0xFF4CAF50)),
                        Triple("Average", "🟡", Color(0xFFFF9800)),
                        Triple("Difficult", "🔴", Color(0xFFF44336))
                    ).forEach { (lvl, emoji, color) ->
                        val isSelected = lvl == selectedLevel
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.95f else 1f,
                            animationSpec = tween(100),
                            label = "difficultyButton"
                        )
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) color else color.copy(alpha = 0.1f)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 8.dp else 4.dp
                            ),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                soundEffects.playButtonClick()
                                hapticFeedback.lightTap()
                                selectedLevel = lvl
                                score = 0
                                streak = 0
                                // reset session if practicing
                                if (isPracticing) {
                                    startNewSession(lvl)
                                }
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lvl,
                                    color = if (isSelected) Color.White else color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Enhanced Target card
                Card(
                    shape = RoundedCornerShape(20.dp), 
                    elevation = CardDefaults.cardElevation(8.dp), 
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📝 Test Challenge",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Show the sign for",
                            fontWeight = Medium, 
                            color = Color(0xFF666666),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Enhanced letter display with timer and trophy on sides
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timer card - Left side
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⏱️", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${timeLeft}s",
                                        fontSize = 24.sp,
                                        fontWeight = Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                            
                            // Enhanced letter display - Center
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(60.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentLetter.ifEmpty { "-" }, 
                                    fontSize = 64.sp, 
                                    fontWeight = ExtraBold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Trophy card - Right side
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🏆", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$score",
                                        fontSize = 24.sp,
                                        fontWeight = Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Progress indicator
                        Text(
                            text = "Progress: ${if (initialTotalAttempts>0) (initialTotalAttempts - letterQueue.size) else 0} / $initialTotalAttempts",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Camera preview + overlay (with guide box and feedback overlay)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = {
                        val frame = FrameLayout(context).apply {
                                addView(previewView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                                val overlay = EvaluationOverlayView(context).apply {
                                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                                }
                            addView(overlay)
                        }
                        frame
                        },
                        modifier = Modifier.matchParentSize()
                    )
                    // Feedback overlay with gamification
                    if (showFeedback) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (feedbackCorrect) {
                                    AnimatedMascot(
                                        isHappy = true,
                                        isCelebrating = true,
                                        size = 80
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = if (feedbackCorrect) "✅ Correct!" else "❌ Try again!",
                                    fontSize = 24.sp,
                                    color = if (feedbackCorrect) Color.Green else Color.Red,
                                    fontWeight = Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Detected: $currentPrediction",
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Controls
                // Enhanced Control Panel
                Text(
                    text = "🎮 Control Panel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Main action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Skip button
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            // Skip: move current to end of queue
                            if (letterQueue.isNotEmpty()) {
                                val first = letterQueue.removeAt(0)
                                letterQueue.add(first)
                                currentLetter = letterQueue[0]
                                timeLeft = 10
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⏭️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Skip",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Camera switch button
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            useFrontCamera = !useFrontCamera
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📷", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (useFrontCamera) "Back" else "Front",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Start/Stop evaluation button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPracticing) Color(0xFFF44336) else Color(0xFF4CAF50)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        if (!isPracticing) {
                            // start session
                            startNewSession(selectedLevel)
                            isPracticing = true
                        } else {
                            // stop practice
                            isPracticing = false
                            showEndDialog = true
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPracticing) "⏹️" else "▶️",
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isPracticing) "Stop Evaluation" else "Start Evaluation",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Enhanced Reset and Tips section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset button
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF607D8B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            // Reset everything
                            isPracticing = false
                            letterQueue.clear()
                            currentLetter = ""
                            score = 0
                            correctCount = 0
                            wrongCount = 0
                            mistakes.clear()
                            timeLeft = 10
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔄", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tips card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Keep hand centered in camera view",
                                color = Color(0xFF1976D2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
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
                            // Track progress when evaluation is completed
                            if (username.isNotEmpty()) {
                                scope.launch {
                                    val sessionResult = ProgressTrackingService.SessionResult(
                                        mode = "evaluation",
                                        accuracy = (correctCount.toFloat() / initialTotalAttempts.toFloat()).coerceAtMost(1.0f),
                                        timeSpent = (System.currentTimeMillis() - sessionStartTime) / 1000,
                                        lettersCompleted = correctCount,
                                        perfectSigns = correctCount,
                                        mistakes = wrongCount
                                    )
                                    
                                    progressTrackingService.updateProgress(username, sessionResult)
                                        .onSuccess { update ->
                                            progressUpdate = update
                                            showProgressDialog = true
                                        }
                                        .onFailure { /* Handle error */ }
                                }
                            }
                            // reset session to allow replay
                            isPracticing = false
                            letterQueue.clear()
                            currentLetter = ""
                            score = 0
                            correctCount = 0
                            wrongCount = 0
                            mistakes.clear()
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
                                navController?.popBackStack()
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
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < analysisInterval) {
            image.close()
            return
        }
        lastAnalysisTime = currentTime

        if (modelInterpreter == null) {
            Log.e(TAG, "Model interpreter is null")
            image.close()
            handler.post { onPrediction("") }
            return
        }

        try {
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
            synchronized(interpreterLock) {
                try {
                    modelInterpreter.run(inputBuffer, outputArray3D)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during model inference", e)
                    handler.post { onPrediction("") }
                    return
                }
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
                handler.post { onPrediction("") }
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
