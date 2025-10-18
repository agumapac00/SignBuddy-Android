package com.example.signbuddy.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherClassPerformanceScreen(navController: NavController? = null) {
    Scaffold(topBar = { TopAppBar(title = { Text("Class Performance") }) }) { inner ->
        var showConfetti by remember { mutableStateOf(false) }
        val gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF7AE), Color(0xFFFFE5C2), Color(0xFFE1F5FE))
        )
        Column(
            Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            com.example.signbuddy.ui.screens.teacher.components.MascotBadge("Class Insights")
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Accuracy Distribution (Bar Chart)")
                    Icon(Icons.Filled.BarChart, contentDescription = null, tint = Color(0xFF90CAF9))
                }
            }
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) { Text("Common Confusions (Heatmap)") }
            }
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) { Text("Average Speed per Level (Line Chart)") }
            }
            val celebrateIs = MutableInteractionSource()
            val celebratePressed by celebrateIs.collectIsPressedAsState()
            val celebrateScale by animateFloatAsState(targetValue = if (celebratePressed) 0.96f else 1f, label = "celebrateScale")
            Button(onClick = { showConfetti = true }, shape = RoundedCornerShape(10.dp), interactionSource = celebrateIs, modifier = Modifier.graphicsLayer(scaleX = celebrateScale, scaleY = celebrateScale)) { Text("Celebrate Improvements") }

            val backIs = MutableInteractionSource()
            val backPressed by backIs.collectIsPressedAsState()
            val backScale by animateFloatAsState(targetValue = if (backPressed) 0.96f else 1f, label = "backScalePerf")
            Button(onClick = { navController?.popBackStack() }, shape = RoundedCornerShape(10.dp), interactionSource = backIs, modifier = Modifier.graphicsLayer(scaleX = backScale, scaleY = backScale)) { Text("Back") }
            com.example.signbuddy.ui.screens.teacher.components.ConfettiOverlay(visible = showConfetti) { showConfetti = false }
        }
    }
}


