package com.example.signbuddy.ui.screens.teacher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.example.signbuddy.ui.components.*

// Data class for student information
data class Student(
    val id: String,
    val name: String,
    val emoji: String,
    val progress: Int,
    val isActive: Boolean,
    val lastActive: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentsScreen(navController: NavController? = null) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )
    
    val accentGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFF6B6B).copy(alpha = 0.1f),
            Color(0xFF4ECDC4).copy(alpha = 0.1f),
            Color.Transparent
        ),
        radius = 800f
    )

    val soundEffects = rememberSoundEffects()
    val hapticFeedback = rememberHapticFeedback()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

    // Sample student data
    val students = remember {
        listOf(
            Student("1", "Emma Johnson", "🌟", 85, true, "2 min ago"),
            Student("2", "Liam Smith", "⭐", 92, true, "5 min ago"),
            Student("3", "Sophia Brown", "🎯", 78, false, "1 hour ago"),
            Student("4", "Noah Davis", "🔥", 88, true, "3 min ago"),
            Student("5", "Ava Wilson", "💪", 76, false, "2 hours ago"),
            Student("6", "Oliver Garcia", "🎨", 94, true, "1 min ago"),
            Student("7", "Isabella Martinez", "🚀", 81, true, "4 min ago"),
            Student("8", "William Anderson", "🏆", 89, false, "30 min ago")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👥 My Students", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            navController?.navigate("teacher/students/add")
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Student")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
            // Enhanced background with layered gradients
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentGradient)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with statistics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📊 Class Overview",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Total Students
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👥", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${students.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Active Students
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌟", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${students.count { it.isActive }}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Average Progress
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📈", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${(students.map { it.progress }.average()).toInt()}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Avg Progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Students List
                Text(
                    text = "🎓 Student List",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(students) { index, student ->
                        StudentCard(
                            student = student,
                            onRemove = {
                                studentToDelete = student
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Student") },
            text = { Text("Are you sure you want to remove ${studentToDelete?.name} from your class?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        showDeleteDialog = false
                        studentToDelete = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        showDeleteDialog = false
                        studentToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentCard(
    student: Student,
    onRemove: () -> Unit
) {
    val cardIs = MutableInteractionSource()
    val pressed by cardIs.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.tween(100),
        label = "studentCard"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = { /* Student card click - could add navigation to student details */ },
        interactionSource = cardIs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Student info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Student avatar
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            color = if (student.isActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                            shape = RoundedCornerShape(25.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.emoji,
                        fontSize = 24.sp
                    )
                }

                Column {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (student.isActive) "🟢 Active - ${student.lastActive}" else "⚫ Last seen ${student.lastActive}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Progress and actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Progress indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${student.progress}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            student.progress >= 90 -> Color(0xFF4CAF50)
                            student.progress >= 70 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Student",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
