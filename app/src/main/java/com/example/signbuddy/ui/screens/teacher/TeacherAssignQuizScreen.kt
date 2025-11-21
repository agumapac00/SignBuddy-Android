package com.example.signbuddy.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAssignQuizScreen(navController: NavController? = null) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📤 Assign Quiz", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { inner ->
        var showConfetti by remember { mutableStateOf(false) }
        val gradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFE0B2), // Warm orange
                Color(0xFFFFF8E1), // Cream
                Color(0xFFE8F5E8), // Light green
                Color(0xFFE3F2FD)  // Light blue
            )
        )
        Column(
            Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(inner)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Enhanced Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📤", style = MaterialTheme.typography.headlineLarge)
                Column {
                    Text(
                        text = "Assign Quiz to Students",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose a quiz and assign it to your class! 🎯",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📝", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Assignment Details",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Fill in the details below to assign a quiz to your students",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    ExposedDropdownMenuBox(expanded = false, onExpandedChange = { }) {
                        val chipClassIs = androidx.compose.foundation.interaction.MutableInteractionSource()
                        val chipClassPressed by chipClassIs.collectIsPressedAsState()
                        val chipClassScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (chipClassPressed) 0.94f else 1f, label = "chipClass")
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            label = { Text("🏫 Select Class") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(scaleX = chipClassScale, scaleY = chipClassScale),
                            interactionSource = chipClassIs,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    ExposedDropdownMenuBox(expanded = false, onExpandedChange = { }) {
                        val chipQuizIs = androidx.compose.foundation.interaction.MutableInteractionSource()
                        val chipQuizPressed by chipQuizIs.collectIsPressedAsState()
                        val chipQuizScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (chipQuizPressed) 0.94f else 1f, label = "chipQuiz")
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            label = { Text("📝 Select Quiz") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(scaleX = chipQuizScale, scaleY = chipQuizScale),
                            interactionSource = chipQuizIs,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("📅 Due Date") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val assignIs = androidx.compose.foundation.interaction.MutableInteractionSource()
                    val assignPressed by assignIs.collectIsPressedAsState()
                    val assignScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (assignPressed) 0.96f else 1f, label = "assignBtnScale")
                    Button(
                        onClick = { showConfetti = true },
                        interactionSource = assignIs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer(scaleX = assignScale, scaleY = assignScale),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ECDC4)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "🚀 Assign Quiz",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Button(
                onClick = { navController?.popBackStack() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Text(
                    text = "⬅️ Back to Dashboard",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            com.example.signbuddy.ui.screens.teacher.components.ConfettiOverlay(visible = showConfetti) { showConfetti = false }
        }
    }
}


