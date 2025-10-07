package com.example.signbuddy.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Size
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.signbuddy.ml.ASLModelHelper
import com.example.signbuddy.ml.HandSignAnalyzer
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationTestScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // UI gradient
    val gradient = Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFFFF8E1)))

    // Permissions
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
        }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var useFrontCamera by remember { mutableStateOf(true) }
    var selectedLevel by remember { mutableStateOf("Easy") }
    var isTesting by remember { mutableStateOf(false) }
    var targetLetter by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }
    var totalAttempts by remember { mutableStateOf(10) }
    var timeLeft by remember { mutableStateOf(0) }

    val correctToneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val incorrectToneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val feedbackHandler = remember { Handler(Looper.getMainLooper()) }

    // Load Model
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ASLModelHelper.loadModel(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evaluation Test") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    "✋ Practice Mode (Student, AI Recognition)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1565C0)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Camera-based AI checks signs in real-time. Feedback shows after you perform a sign.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Difficulty buttons (unchanged)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Easy", "Average", "Difficult").forEach { lvl ->
                        val isSelected = lvl == selectedLevel
                        Button(
                            onClick = { selectedLevel = lvl },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFF90CAF9)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(lvl, color = Color.White) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target card (same layout, improved text + timer icon)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Show the sign for", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            targetLetter,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D47A1)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // SCORE section — bigger and bolder
                        Text(
                            "Score: $score / $totalAttempts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // TIMER section — add circular icon to the left
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (timeLeft > 5) Color(0xFF4CAF50)
                                        else Color(0xFFFF5252)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Time left: ${timeLeft}s",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF333333)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Camera preview (unchanged)
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
                                addView(
                                    previewView,
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                            frame
                        },
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls (same layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { /* skip */ }, modifier = Modifier.weight(1f)) {
                        Text("Skip")
                    }
                    Button(onClick = { useFrontCamera = !useFrontCamera }, modifier = Modifier.weight(1f)) {
                        Text(if (useFrontCamera) "Back Camera" else "Front Camera")
                    }
                    Button(
                        onClick = { isTesting = !isTesting },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTesting) "Stop Practice" else "Start Practice")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { score = 0 }) { Text("Reset") }
                    Text(
                        "Tip: keep hand centered in the camera view",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            correctToneGenerator.release()
            incorrectToneGenerator.release()
            feedbackHandler.removeCallbacksAndMessages(null)
            cameraExecutor.shutdown()
        }
    }
}
