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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.signbuddy.ui.components.*
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
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
fun PracticeScreen(navController: NavController? = null, username: String = "") {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Kindergarten-friendly bright colors
    val gradient = Brush.verticalGradient(colors = listOf(
        Color(0xFF90EE90), // Light green
        Color(0xFFB8F5B8), // Pale green
        Color(0xFFF0FFF0)  // Honeydew
    ))
    
    // Gamification elements
    val soundEffects = rememberSoundEffects()
    val hapticFeedback = rememberHapticFeedback()
    var showEncouragingMessage by remember { mutableStateOf(false) }
    var encouragingText by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    
    // Progress tracking
    val progressTrackingService = remember { ProgressTrackingService() }
    var sessionStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var lettersCompleted by remember { mutableStateOf(0) }
    var perfectSigns by remember { mutableStateOf(0) }
    var mistakes by remember { mutableStateOf(0) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressUpdate by remember { mutableStateOf<ProgressTrackingService.ProgressUpdate?>(null) }
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
    
    // Camera & practice states (same UI as your original)
    var useFrontCamera by remember { mutableStateOf(true) }
    var selectedLevel by remember { mutableStateOf("Easy") }
    var isPracticing by remember { mutableStateOf(false) }
    var targetLetter by remember { mutableStateOf(getRandomTarget(selectedLevel)) }
    var totalAttempts by remember { mutableStateOf(10) }
    var currentPrediction by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackCorrect by remember { mutableStateOf(false) }
    var showBadgeDialog by remember { mutableStateOf(false) }

    // Load model off the UI thread and store interpreter in state
    var modelInterpreter by remember { mutableStateOf<Interpreter?>(null) }
    var handSignAnalyzer by remember { mutableStateOf<HandSignAnalyzer?>(null) }
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

    // Revert to 640 for this model to maximize detection accuracy
    val inputSize = 640
    val imageProcessor = remember {
        ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    // Audio feedback
    val correctToneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val incorrectToneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    // Feedback handler with gamification
    val feedbackHandler = remember { Handler(Looper.getMainLooper()) }
    val showFeedbackRunnable = remember {
        Runnable {
            showFeedback = false
            if (feedbackCorrect) {
                // Play success sound and haptic
                soundEffects.playCorrect()
                hapticFeedback.successPattern()
                
                // Update score and streak
                score += 10
                streak += 1
                lettersCompleted++
                perfectSigns++
                
                // Show encouraging message
                encouragingText = EncouragingMessages.getRandomCorrectMessage()
                showEncouragingMessage = true
                
                if (score < totalAttempts * 10) {
                    targetLetter = getRandomTarget(selectedLevel)
                } else {
                    // Session completed - track progress
                    if (username.isNotEmpty()) {
                        scope.launch {
                            val sessionResult = ProgressTrackingService.SessionResult(
                                mode = "practice",
                                accuracy = (perfectSigns.toFloat() / lettersCompleted.toFloat()).coerceAtMost(1.0f),
                                timeSpent = (System.currentTimeMillis() - sessionStartTime) / 1000,
                                lettersCompleted = lettersCompleted,
                                perfectSigns = perfectSigns,
                                mistakes = mistakes,
                                actualScore = score // Pass the actual score earned in practice
                            )
                            
                            android.util.Log.d("PracticeScreen", "Calling updateProgress with actualScore=$score, lettersCompleted=$lettersCompleted")
                            progressTrackingService.updateProgress(username, sessionResult)
                                .onSuccess { update ->
                                    android.util.Log.d("PracticeScreen", "✅ Progress saved successfully!")
                                    android.util.Log.d("PracticeScreen", "XP Gained: ${update.xpGained}, Score Gained: ${update.scoreGained}")
                                    progressUpdate = update
                                    showProgressDialog = true
                                }
                                .onFailure { error ->
                                    android.util.Log.e("PracticeScreen", "❌ Failed to save progress", error)
                                }
                        }
                    }
                    
                    if (selectedLevel == "Easy") {
                        selectedLevel = "Average"
                        encouragingText = "🎉 Great job! Moving to Average mode!"
                        showEncouragingMessage = true
                    } else if (selectedLevel == "Average") {
                        selectedLevel = "Difficult"
                        encouragingText = "🌟 Excellent! Now try Difficult mode!"
                        showEncouragingMessage = true
                    } else {
                        showBadgeDialog = true
                        isPracticing = false
                        encouragingText = "🏆 Mastered all levels! You're amazing!"
                        showEncouragingMessage = true
                        return@Runnable
                    }
                    score = 0
                    targetLetter = getRandomTarget(selectedLevel)
                }
            } else {
                // Play wrong sound and haptic
                soundEffects.playWrong()
                hapticFeedback.errorPattern()
                
                // Reset streak
                streak = 0
                mistakes++
                
                // Show encouraging message
                encouragingText = EncouragingMessages.getRandomWrongMessage()
                showEncouragingMessage = true
            }
        }
    }

    // CameraX setup: runs only when permission granted, practicing enabled, model loaded, and on practice screen
    LaunchedEffect(hasPermission, useFrontCamera, isPracticing, modelInterpreter, showLevelSelection, shouldStopAnalysis) {
        Log.d("PracticeScreen", "=== CAMERA SETUP LaunchedEffect ===")
        Log.d("PracticeScreen", "hasPermission: $hasPermission, isPracticing: $isPracticing, modelInterpreter: ${modelInterpreter != null}, showLevelSelection: $showLevelSelection, shouldStopAnalysis: $shouldStopAnalysis")
        
        if (!hasPermission || !isPracticing || modelInterpreter == null || showLevelSelection || shouldStopAnalysis) {
            Log.d("PracticeScreen", "Skipping camera setup - conditions not met")
            return@LaunchedEffect
        }

        Log.d("PracticeScreen", "Setting up camera and analyzer...")
        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        // ImageAnalysis: keep latest frames only and analyze on background executor
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(320, 320))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Use the analyzer on the background executor
        val analyzer = HandSignAnalyzer(
            modelInterpreter = modelInterpreter,
            imageProcessor = imageProcessor,
            inputSize = inputSize,
            useFrontCamera = useFrontCamera,
            context = context,
            shouldStop = { shouldStopAnalysis },
            onPrediction = { prediction: String ->
                // update UI states based on prediction; guard while feedback is visible
                currentPrediction = prediction
                // Only process if not already showing feedback (prevents double detection)
                if (!showFeedback && prediction.isNotEmpty() && isPracticing) {
                    Log.d("PracticeScreen", "Prediction received: '$prediction', target: '$targetLetter'")
                    if (prediction.uppercase() == targetLetter.uppercase()) {
                        feedbackCorrect = true
                        correctToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                    } else {
                        feedbackCorrect = false
                        incorrectToneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 200)
                    }
                    showFeedback = true
                    feedbackHandler.postDelayed(showFeedbackRunnable, 1500)
                }
            }
        )
        
        // Store analyzer instance for cleanup
        handSignAnalyzer = analyzer
        
        imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer)
            Log.d("PracticeScreen", "Camera bound successfully")
        } catch (e: Exception) {
            Log.e("PracticeScreen", "Error binding camera", e)
            e.printStackTrace()
        }
    }

    // Kindergarten-friendly UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤟", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Practice!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🎯", fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        shouldStopAnalysis = true
                        showFeedback = false
                        if (showLevelSelection) {
                            navController?.navigate("studentDashboard/$username?tab=1") {
                                popUpTo("studentDashboard/{username}") { inclusive = false }
                                launchSingleTop = true
                            }
                        } else {
                            isPracticing = false
                            showLevelSelection = true
                        }
                    }) {
                        Text("⬅️", fontSize = 26.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
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
            // Encouraging message overlay
            if (showEncouragingMessage) {
                FloatingMessage(
                    message = encouragingText,
                    isVisible = showEncouragingMessage,
                    onComplete = { showEncouragingMessage = false }
                )
            }
            // Screen 1: Level Selection
            if (showLevelSelection) {
                LevelSelectionScreen(
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
                        shouldStopAnalysis = false // Ensure detection is enabled
                        showLevelSelection = false
                        targetLetter = getRandomTarget(selectedLevel)
                        sessionStartTime = System.currentTimeMillis() // Reset session start time
                        lettersCompleted = 0
                        perfectSigns = 0
                        mistakes = 0
                    },
                    onBackToLessons = {
                        shouldStopAnalysis = true
                        navController?.navigate("studentDashboard/$username?tab=1") {
                            popUpTo("studentDashboard/{username}") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    gradient = gradient
                )
            } else {
                // Screen 2: Practice Interface
                PracticeInterfaceScreen(
                    selectedLevel = selectedLevel,
                    targetLetter = targetLetter,
                    score = score,
                    streak = streak,
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
                    onSkip = { targetLetter = getRandomTarget(selectedLevel) },
                    onStartStop = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        isPracticing = !isPracticing
                        if (isPracticing) {
                            shouldStopAnalysis = false // Reset to allow detection
                            sessionStartTime = System.currentTimeMillis() // Reset session start time
                            targetLetter = getRandomTarget(selectedLevel)
                            score = 0
                            streak = 0
                            lettersCompleted = 0
                            perfectSigns = 0
                            mistakes = 0
                        } else {
                            shouldStopAnalysis = true
                        }
                    },
                    onReset = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        score = 0
                        streak = 0
                        targetLetter = getRandomTarget(selectedLevel)
                        currentPrediction = ""
                        showFeedback = false
                        feedbackHandler.removeCallbacks(showFeedbackRunnable)
                    },
                    gradient = gradient
                )
            }
            
            if (showBadgeDialog) {
                AlertDialog(
                    onDismissRequest = { showBadgeDialog = false },
                    title = { Text("🎉 Master Learner Badge Unlocked!") },
                    text = { Text("Congratulations! You have mastered all levels.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showBadgeDialog = false
                            isPracticing = true
                            selectedLevel = "Easy"
                            score = 0
                            targetLetter = getRandomTarget(selectedLevel)
                        }) {
                            Text("Continue Practicing")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showBadgeDialog = false
                            shouldStopAnalysis = true
                            navController?.popBackStack()
                        }) {
                            Text("Back to Menu")
                        }
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
                            Text("You completed the practice session!")
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
            // Stop analysis before closing interpreter to prevent crashes
            shouldStopAnalysis = true
            correctToneGenerator.release()
            incorrectToneGenerator.release()
            // Close interpreter after a brief delay to let running analysis finish
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                modelInterpreter?.close()
            }, 300)
            feedbackHandler.removeCallbacks(showFeedbackRunnable)
            cameraExecutor.shutdown()
        }
    }
}

// Function to get random target based on level
fun getRandomTarget(level: String): String {
    val easyLetters = listOf('A', 'B', 'C', 'O', 'S', 'L', 'D', 'E')
    val averageLetters = listOf('F', 'M', 'N', 'P', 'U', 'T', 'I', 'J')
    val difficultLetters = listOf('X', 'Y', 'Z', 'Q', 'R', 'V', 'W', 'K', 'G', 'H')
    val letters = when (level) {
        "Easy" -> easyLetters
        "Average" -> averageLetters
        "Difficult" -> difficultLetters
        else -> easyLetters
    }
    return letters.random().toString()
}

// Load TFLite model from assets with NNAPI acceleration
private fun loadPracticeModel(context: Context): Interpreter? {
    return try {
        val model = loadModelFile(context, "asl_model.tflite")
        // Use NNAPI delegate for hardware acceleration (faster inference)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            // Try NNAPI first for hardware acceleration
            try {
                val nnApiDelegate = org.tensorflow.lite.nnapi.NnApiDelegate()
                addDelegate(nnApiDelegate)
                Log.d("PracticeScreen", "NNAPI delegate enabled")
            } catch (e: Exception) {
                Log.d("PracticeScreen", "NNAPI not available, using CPU")
            }
        }
        Interpreter(model, options).also {
            val inputShape = it.getInputTensor(0).shape()
            val outputShape = it.getOutputTensor(0).shape()
            Log.d("PracticeScreen", "Model loaded. Input: ${inputShape.contentToString()}, Output: ${outputShape.contentToString()}")
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

// Extension: convert ImageProxy to Bitmap with rotation
@androidx.annotation.OptIn(ExperimentalGetImage::class)
fun ImageProxy.toBitmap(rotationDegrees: Int): Bitmap {
    val image = this.image ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()
    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    // Apply rotation
    val matrix = Matrix()
    matrix.postRotate(rotationDegrees.toFloat())
    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    return bitmap
}

// Function to horizontally flip bitmap for front camera mirror effect
private fun flipHorizontally(bitmap: Bitmap): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1f, 1f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Hand sign analyzer for YOLOv8-style TFLite (post-processing + confidence threshold 0.75)
class HandSignAnalyzer(
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
    private val analysisInterval = 1500L // ms - analyze every 1.5 seconds for smooth camera
    private val TAG = "HandSignAnalyzer"
    // ASL labels: 0-25 A-Z, 26 nothing
    private val labels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "nothing")
    // Model params (adapt if your model differs)
    private val numClasses = 27
    private val numFeatures = 4 + numClasses // 31
    private val numDetections = 8400
    private val confThreshold = 0.30f // raw detection threshold (filter noise early)
    private val feedbackThreshold = 0.70f // FINAL confidence threshold (70%)
    private val iouThreshold = 0.5f
    
    // Single high-confidence detection (no consecutive required at 70%+)
    @Volatile private var lastPredictionTime = 0L
    private val predictionCooldown = 2500L // 2.5 sec cooldown after prediction to avoid double

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
                image.close()
                return
            }
            lastAnalysisTime = currentTime

            if (modelInterpreter == null) {
                Log.e(TAG, "Model interpreter is null")
                handler.post { onPrediction("") }
                image.close()
                return
            }
            // Handle rotation and flip for front/back camera
            // Front camera: 270 degrees rotation + horizontal flip
            // Back camera: 90 degrees rotation (no flip needed)
            val rotationDegrees = if (useFrontCamera) 270 else 90
            var bitmap = image.toBitmap(rotationDegrees)
            if (useFrontCamera) {
                bitmap = flipHorizontally(bitmap)
            }

            // Validate bitmap
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                Log.e(TAG, "Invalid bitmap: recycled=${bitmap.isRecycled}, size=${bitmap.width}x${bitmap.height}")
                handler.post { onPrediction("") }
                image.close()
                return
            }

            // Prepare TensorImage and process
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)
            
            // Clean up original bitmap to free memory
            if (bitmap != processedImage.bitmap) {
                bitmap.recycle()
            }

            // Prepare float input buffer (NHWC float [0,1] normalized)
            inputBuffer.rewind()
            
            // Validate bitmap size
            if (processedImage.bitmap.width != inputSize || processedImage.bitmap.height != inputSize) {
                Log.e(TAG, "Bitmap size mismatch: expected ${inputSize}x${inputSize}, got ${processedImage.bitmap.width}x${processedImage.bitmap.height}")
                handler.post { onPrediction("") }
                image.close()
                return
            }
            
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
            
            // Validate input array size
            if (floatIndex != inputArray.size) {
                Log.e(TAG, "Input array size mismatch: expected ${inputArray.size}, got $floatIndex")
                handler.post { onPrediction("") }
                image.close()
                return
            }
            
            inputBuffer.asFloatBuffer().put(inputArray)

            // Run inference (output shape expected [1, numFeatures, numDetections])
            // Use synchronized block to prevent concurrent access
            var inferenceSucceeded = false
            
            // Check again before running expensive inference - also check interpreter is not null
            val interpreter = modelInterpreter
            if (!shouldStop() && interpreter != null) {
                try {
                    synchronized(interpreterLock) {
                        // Double-check after acquiring lock - interpreter might have been closed
                        if (shouldStop() || modelInterpreter == null) {
                            image.close()
                            return
                        }
                        // Additional safety check - try to access interpreter in a safe way
                        try {
                            modelInterpreter!!.run(inputBuffer, outputArray3D)
                            inferenceSucceeded = true
                        } catch (e: IllegalStateException) {
                            // Interpreter was closed/released during inference
                            Log.w(TAG, "Interpreter closed during inference", e)
                            image.close()
                            return
                        } catch (e: NullPointerException) {
                            // Interpreter became null
                            Log.w(TAG, "Interpreter became null during inference", e)
                            image.close()
                            return
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during model inference", e)
                            image.close()
                            return
                        }
                    }
                } catch (e: Exception) {
                    // Catch any exception from synchronized block itself
                    Log.e(TAG, "Error in synchronized block", e)
                    image.close()
                    return
                }
            } else {
                image.close()
                return
            }
            
            // If inference failed or we should stop, exit early
            if (!inferenceSucceeded || shouldStop()) {
                image.close()
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
                image.close()
                return
            }

            // Apply NMS
            val selectedBoxes = applyNMS(boundingBoxes)

            // Pick highest conf box
            val bestBox = selectedBoxes.maxByOrNull { it.cnf }
            
            // Check if we're in cooldown period (avoid double prediction)
            val now = System.currentTimeMillis()
            val inCooldown = (now - lastPredictionTime) < predictionCooldown
            
            val predictedLetter = if (bestBox != null && bestBox.cnf >= feedbackThreshold && !inCooldown) {
                lastPredictionTime = now // Start cooldown
                Log.d(TAG, "Final prediction: ${bestBox.clsName} (conf: ${bestBox.cnf})")
                bestBox.clsName
            } else {
                ""
            }
            
            handler.post { onPrediction(predictedLetter) }
            // Close image after successful analysis
            image.close()

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during analysis", e)
            // Force garbage collection
            System.gc()
            handler.post { onPrediction("") }
        } catch (e: IllegalStateException) {
            // Interpreter or camera was closed
            Log.w(TAG, "Illegal state during analysis (likely closed)", e)
            handler.post { onPrediction("") }
        } catch (e: NullPointerException) {
            // Something became null during analysis
            Log.w(TAG, "Null pointer during analysis", e)
            handler.post { onPrediction("") }
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyze", e)
            handler.post { onPrediction("") }
        } finally {
            // Always close the image, even if there was an error
            try {
                image.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing image", e)
            }
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

// Data class for level options
data class LevelOption(
    val level: String,
    val emoji: String,
    val color: Color,
    val description: String
)

// Screen 1: Level Selection Screen - Kindergarten-friendly, fits on one page
@Composable
fun LevelSelectionScreen(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onBackToLessons: () -> Unit,
    gradient: Brush
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        // Decorations
        Box(modifier = Modifier.fillMaxSize()) {
            Text("🌞", fontSize = 45.sp, modifier = Modifier.offset(x = 300.dp, y = 30.dp).graphicsLayer { rotationZ = wiggleAngle })
            Text("☁️", fontSize = 35.sp, modifier = Modifier.offset(x = 20.dp, y = 40.dp).graphicsLayer { alpha = 0.6f })
            Text("⭐", fontSize = 22.sp, modifier = Modifier.offset(x = 40.dp, y = 200.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🌟", fontSize = 20.sp, modifier = Modifier.offset(x = 340.dp, y = 350.dp).graphicsLayer { alpha = sparkleAlpha })
            Text("🦋", fontSize = 26.sp, modifier = Modifier.offset(x = 30.dp, y = 500.dp).graphicsLayer { rotationZ = wiggleAngle })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                Text("🤟", fontSize = 70.sp, modifier = Modifier.graphicsLayer { rotationZ = wiggleAngle / 2 })
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Pick Your Level!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2E7D32)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎮", fontSize = 18.sp, modifier = Modifier.graphicsLayer { rotationZ = -wiggleAngle })
                Spacer(modifier = Modifier.width(8.dp))
                Text("How hard do you want?", fontSize = 16.sp, color = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎮", fontSize = 18.sp, modifier = Modifier.graphicsLayer { rotationZ = wiggleAngle })
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Level cards - kindergarten style
            listOf(
                LevelOption("Easy", "🐣", Color(0xFF81C784), "For beginners!"),
                LevelOption("Average", "🐥", Color(0xFFFFB74D), "Getting better!"),
                LevelOption("Difficult", "🦅", Color(0xFFE57373), "Super hard!")
            ).forEachIndexed { index, (lvl, emoji, color, description) ->
                val isSelected = lvl == selectedLevel
                val cardScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f),
                    label = "scale$index"
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .padding(vertical = 4.dp)
                        .graphicsLayer { scaleX = cardScale; scaleY = cardScale },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) color else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 12.dp else 4.dp
                    ),
                    shape = RoundedCornerShape(24.dp),
                    onClick = { onLevelSelected(lvl) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 40.sp)
                            Column {
                                Text(
                                    lvl,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else color
                                )
                                Text(
                                    description,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.Gray
                                )
                            }
                        }
                        if (isSelected) {
                            Text("✅", fontSize = 32.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Start button
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(65.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("▶️", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Let's Go!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// Screen 2: Practice Interface Screen - Compact, fits on one page
@Composable
fun PracticeInterfaceScreen(
    selectedLevel: String,
    targetLetter: String,
    score: Int,
    streak: Int,
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
        // Compact Target letter card with Score and Streak on left side
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score and Streak on left side
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Score
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Score:", fontSize = 11.sp, color = Color(0xFF666666), fontWeight = Medium)
                        Text("$score", fontSize = 16.sp, fontWeight = Bold, color = Color(0xFF4CAF50))
                    }
                    // Streak
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Streak:", fontSize = 11.sp, color = Color(0xFF666666), fontWeight = Medium)
                        Text("$streak", fontSize = 16.sp, fontWeight = Bold, color = Color(0xFFFF9800))
                    }
                }
                
                // Letter display on right side
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🎯 Show the sign for",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = targetLetter,
                            fontSize = 36.sp,
                            fontWeight = ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Camera preview - larger but still fits one page with controls and reset button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
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
                        val overlay = OverlayView(context).apply {
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
                    text = if (isPracticing) "Stop Practice" else "Start Practice",
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

// Custom overlay view with static guide box at center
class OverlayView(context: Context) : View(context) {
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
