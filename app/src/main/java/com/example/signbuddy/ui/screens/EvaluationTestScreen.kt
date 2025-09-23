package com.example.signbuddy.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationTestScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF7AE), Color(0xFFE1F5FE))
    )

    // camera permission
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    var useFrontCamera by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }

    // bind camera
    LaunchedEffect(hasPermission, useFrontCamera) {
        if (hasPermission) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val selector =
                if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
        }
    }

    // evaluation states
    var progress by remember { mutableStateOf(0f) } // 0–1
    var timeLeft by remember { mutableStateOf(10f) } // seconds per sign
    val totalTime = 10f

    // animate the timer bar
    val timerProgress by animateFloatAsState(
        targetValue = timeLeft / totalTime,
        label = "timerProgress"
    )

    var feedbackText by remember { mutableStateOf("Show the correct sign!") }

    // countdown & move to next sign
    LaunchedEffect(progress) {
        val tickMs = 100L          // 👈 speed: tick interval
        val decrement = 0.1f       // 👈 speed: how much time reduces per tick
        while (true) {
            delay(tickMs)
            if (timeLeft > 0f) {
                timeLeft -= decrement
            } else {
                // timer ended → next sign
                progress = (progress + 0.1f).coerceAtMost(1f)
                timeLeft = totalTime
                feedbackText = "Next sign!"
            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- CAMERA PREVIEW ONLY ---
            if (hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp) // camera height
                ) {
                    AndroidView(
                        { previewView },
                        modifier = Modifier.matchParentSize()
                    )

                    // timer bar stays INSIDE the camera preview
                    LinearProgressIndicator(
                        progress = timerProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text("Requesting camera permission…")
            }

            // --- EVERYTHING BELOW IS OUTSIDE CAMERA ---
            Spacer(modifier = Modifier.height(100.dp))

            // progress bar outside camera
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Time left: ${timeLeft.toInt()} s",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                feedbackText,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Progress: ${(progress * 100).toInt()}%", fontSize = 16.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (useFrontCamera) "Switch to Back Camera" else "Switch to Front Camera")
            }
        }
    }
}
