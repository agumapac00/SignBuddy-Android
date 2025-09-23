package com.example.signbuddy.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFE1F5FE), Color(0xFFFFF7AE))
    )

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

    var feedbackText by remember { mutableStateOf("Waiting for sign...") }
    var difficultyLevel by remember { mutableStateOf("Easy") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Mode") },
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
        ) {
            // Scrollable content (camera + other info)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 100.dp), // give space for buttons at the bottom
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Practice with AI Recognition",
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Difficulty selector
                Text("Difficulty Level")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Easy", "Average", "Difficult").forEach { level ->
                        Button(onClick = { difficultyLevel = level }, modifier = Modifier.weight(1f)) {
                            Text(level)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Camera preview with feedback text
                if (hasPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            { previewView },
                            modifier = Modifier.matchParentSize()
                        )

                        Text(
                            feedbackText,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                } else {
                    Text("Requesting camera permission…")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Buttons anchored at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { useFrontCamera = !useFrontCamera },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (useFrontCamera) "Switch to Back Camera" else "Switch to Front Camera")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { /* Future: start AI detection */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Practice")
                }
            }
        }
    }
}
