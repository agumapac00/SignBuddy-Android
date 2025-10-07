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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
fun PracticeScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep your UI gradient & styles
    val gradient = Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFFFF8E1)))

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

    // Camera & practice states (same UI as your original)
    var useFrontCamera by remember { mutableStateOf(true) }
    var selectedLevel by remember { mutableStateOf("Easy") }
    var isPracticing by remember { mutableStateOf(false) }
    var targetLetter by remember { mutableStateOf(getRandomTarget(selectedLevel)) }
    var score by remember { mutableStateOf(0) }
    var totalAttempts by remember { mutableStateOf(10) }
    var currentPrediction by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackCorrect by remember { mutableStateOf(false) }
    var showBadgeDialog by remember { mutableStateOf(false) }

    // Load model off the UI thread and store interpreter in state
    var modelInterpreter by remember { mutableStateOf<Interpreter?>(null) }
    LaunchedEffect(Unit) {
        // Loads model on IO dispatcher
        modelInterpreter = withContext(Dispatchers.IO) { loadModel(context) }
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

    // Feedback handler
    val feedbackHandler = remember { Handler(Looper.getMainLooper()) }
    val showFeedbackRunnable = remember {
        Runnable {
            showFeedback = false
            if (feedbackCorrect) {
                score = min(score + 1, totalAttempts)
                if (score < totalAttempts) {
                    targetLetter = getRandomTarget(selectedLevel)
                } else {
                    if (selectedLevel == "Easy") {
                        selectedLevel = "Average"
                        Toast.makeText(context, "Great job on Easy! Now Average mode.", Toast.LENGTH_SHORT).show()
                    } else if (selectedLevel == "Average") {
                        selectedLevel = "Difficult"
                        Toast.makeText(context, "Excellent on Average! Now Difficult mode.", Toast.LENGTH_SHORT).show()
                    } else {
                        showBadgeDialog = true
                        isPracticing = false
                        Toast.makeText(context, "Mastered Difficult! Unlocking badge...", Toast.LENGTH_SHORT).show()
                        return@Runnable
                    }
                    score = 0
                    targetLetter = getRandomTarget(selectedLevel)
                }
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
        imageAnalyzer.setAnalyzer(cameraExecutor, HandSignAnalyzer(
            modelInterpreter = modelInterpreter,
            imageProcessor = imageProcessor,
            inputSize = inputSize,
            useFrontCamera = useFrontCamera,
            context = context,
            onPrediction = { prediction: String ->
                // update UI states based on prediction; only act on non-empty string
                currentPrediction = prediction
                if (prediction.isNotEmpty() && prediction.uppercase() == targetLetter.uppercase()) {
                    feedbackCorrect = true
                    correctToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                } else if (prediction.isNotEmpty()) {
                    feedbackCorrect = false
                    incorrectToneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 200)
                }
                if (prediction.isNotEmpty()) {
                    showFeedback = true
                    feedbackHandler.postDelayed(showFeedbackRunnable, 1500)
                }
            }
        ))

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // UI (kept intact from your original)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Mode") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(inner)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✋ Practice Mode (Student, AI Recognition)", fontSize = 20.sp, fontWeight = SemiBold, color = Color(0xFF1565C0))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Camera-based AI checks signs in real-time. Feedback shows after you perform a sign.", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                // Difficulty buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Easy", "Average", "Difficult").forEach { lvl ->
                        val isSelected = lvl == selectedLevel
                        Button(
                            onClick = {
                                selectedLevel = lvl
                                score = 0
                                if (isPracticing) {
                                    targetLetter = getRandomTarget(lvl)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFF90CAF9)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(lvl, color = Color.White) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Target card
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Show the sign for", fontWeight = Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(targetLetter, fontSize = 48.sp, fontWeight = ExtraBold, color = Color(0xFF0D47A1))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Score: $score / $totalAttempts", fontSize = 14.sp, color = Color.Gray)
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
                                val overlay = OverlayView(context).apply {
                                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                                }
                                addView(overlay)
                            }
                            frame
                        },
                        modifier = Modifier.matchParentSize()
                    )
                    // Feedback overlay
                    if (showFeedback) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { targetLetter = getRandomTarget(selectedLevel) }, modifier = Modifier.weight(1f)) {
                        Text("Skip")
                    }
                    Button(onClick = { useFrontCamera = !useFrontCamera }, modifier = Modifier.weight(1f)) {
                        Text(if (useFrontCamera) "Back Camera" else "Front Camera")
                    }
                    Button(
                        onClick = {
                            isPracticing = !isPracticing
                            if (isPracticing) {
                                targetLetter = getRandomTarget(selectedLevel)
                                score = 0
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPracticing) "Stop Practice" else "Start Practice")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = {
                        score = 0
                        targetLetter = getRandomTarget(selectedLevel)
                        currentPrediction = ""
                        showFeedback = false
                        feedbackHandler.removeCallbacks(showFeedbackRunnable)
                    }) { Text("Reset") }
                    Text("Tip: keep hand centered in the camera view", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                }
                Spacer(modifier = Modifier.height(40.dp))
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
                            navController?.popBackStack()
                        }) {
                            Text("Back to Menu")
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

// Load TFLite model from assets (CPU-only)
private fun loadModel(context: Context): Interpreter? {
    return try {
        val model = loadModelFile(context, "asl_model.tflite")
        Interpreter(model).also {
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
    private val feedbackThreshold = 0.75f // FINAL confidence threshold (user requested 75%)
    private val iouThreshold = 0.5f

    // Reusable buffers to reduce allocations and GC pressure
    private val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val outputArray3D = Array(1) { Array(numFeatures) { FloatArray(numDetections) } }
    private val pixels = IntArray(inputSize * inputSize)
    private val inputArray = FloatArray(inputSize * inputSize * 3)

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
            modelInterpreter.run(inputBuffer, outputArray3D)

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
            Log.d(TAG, "Final prediction: $predictedLetter (conf: ${bestBox?.cnf ?: 0f})")
            handler.post { onPrediction(predictedLetter) }

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
