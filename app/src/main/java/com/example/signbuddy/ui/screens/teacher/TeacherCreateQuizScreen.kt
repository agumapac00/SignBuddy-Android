package com.example.signbuddy.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryAdd
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherCreateQuizScreen(navController: NavController? = null) {
    Scaffold(topBar = { TopAppBar(title = { Text("Create Quiz") }) }) { inner ->
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
            com.example.signbuddy.ui.screens.teacher.components.MascotBadge("Quiz Builder")
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = Color(0xFFC5CAE9), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Quiz Details",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color(0xFF0D47A1)
                        )
                    }
                    Text("Build a custom quiz (title, questions, per-letter tasks)")
                    OutlinedTextField(value = "", onValueChange = {}, label = { Text("Quiz Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = "", onValueChange = {}, label = { Text("Instructions (optional)") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chipAZIs = androidx.compose.foundation.interaction.MutableInteractionSource()
                        val chipAZPressed by chipAZIs.collectIsPressedAsState()
                        val chipAZScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (chipAZPressed) 0.94f else 1f, label = "chipAZ")
                        AssistChip(onClick = { /* add A-Z */ }, label = { Text("Add A-Z") }, interactionSource = chipAZIs, modifier = Modifier.graphicsLayer(scaleX = chipAZScale, scaleY = chipAZScale))

                        val chipRndIs = androidx.compose.foundation.interaction.MutableInteractionSource()
                        val chipRndPressed by chipRndIs.collectIsPressedAsState()
                        val chipRndScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (chipRndPressed) 0.94f else 1f, label = "chipRnd")
                        AssistChip(onClick = { /* randomize */ }, label = { Text("Randomize") }, interactionSource = chipRndIs, modifier = Modifier.graphicsLayer(scaleX = chipRndScale, scaleY = chipRndScale))
                    }
                    val saveIs = androidx.compose.foundation.interaction.MutableInteractionSource()
                    val savePressed by saveIs.collectIsPressedAsState()
                    val saveScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (savePressed) 0.96f else 1f, label = "saveScale")
                    Button(onClick = { showConfetti = true }, interactionSource = saveIs, modifier = Modifier.graphicsLayer(scaleX = saveScale, scaleY = saveScale)) {
                        Icon(Icons.Filled.LibraryAdd, contentDescription = null, tint = Color(0xFF81C784))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Draft")
                    }
                }
            }
            Button(onClick = { navController?.popBackStack() }, shape = RoundedCornerShape(10.dp)) { Text("Back") }
            com.example.signbuddy.ui.screens.teacher.components.ConfettiOverlay(visible = showConfetti) { showConfetti = false }
        }
    }
}


