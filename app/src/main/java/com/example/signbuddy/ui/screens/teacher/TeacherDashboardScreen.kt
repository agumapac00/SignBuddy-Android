package com.example.signbuddy.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.text.font.FontWeight
import com.example.signbuddy.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(navController: NavController? = null) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )
    
    // Additional background elements
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
    var showConfetti by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👩‍🏫 Teacher Dashboard", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            showLogoutDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
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
            if (showConfetti) {
                com.example.signbuddy.ui.screens.teacher.components.ConfettiOverlay(
                    visible = showConfetti,
                    onFinished = { showConfetti = false }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Enhanced Header
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
                        Text(
                            text = "Welcome, Teacher! 👩‍🏫",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage your classroom and help students learn ASL! 🌟",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "🚀 Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Enhanced STUDENT MANAGEMENT CARD
                run {
                    val cardIs = MutableInteractionSource()
                    val pressed by cardIs.collectIsPressedAsState()
                    val scale by animateFloatAsState(targetValue = if (pressed) 0.99f else 1f, label = "cardScale1")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4ECDC4)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        interactionSource = cardIs,
                        onClick = {}
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("👥", style = MaterialTheme.typography.headlineLarge)
                                Text(
                                    text = "Student Management",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "View, add, and remove students from your class",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val chipViewIs = MutableInteractionSource()
                                val chipViewPressed by chipViewIs.collectIsPressedAsState()
                                val chipViewScale by animateFloatAsState(
                                    targetValue = if (chipViewPressed) 0.94f else 1f,
                                    label = "chipView"
                                )
                                Button(
                                    onClick = {
                                        soundEffects.playButtonClick()
                                        hapticFeedback.lightTap()
                                        navController?.navigate("teacher/students")
                                    },
                                    interactionSource = chipViewIs,
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = chipViewScale, scaleY = chipViewScale),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("👀 View All", color = Color(0xFF4ECDC4), fontWeight = FontWeight.Bold)
                                }

                                val chipAddIs = MutableInteractionSource()
                                val chipAddPressed by chipAddIs.collectIsPressedAsState()
                                val chipAddScale by animateFloatAsState(
                                    targetValue = if (chipAddPressed) 0.94f else 1f,
                                    label = "chipAdd"
                                )
                                Button(
                                    onClick = {
                                        soundEffects.playButtonClick()
                                        hapticFeedback.lightTap()
                                        navController?.navigate("teacher/students/add")
                                    },
                                    interactionSource = chipAddIs,
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = chipAddScale, scaleY = chipAddScale),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("➕ Add", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Enhanced STUDENT ACTION BUTTONS
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val viewStudentsIs = MutableInteractionSource()
                    val viewStudentsPressed by viewStudentsIs.collectIsPressedAsState()
                    val viewStudentsScale by animateFloatAsState(targetValue = if (viewStudentsPressed) 0.96f else 1f, label = "viewStudentsScale")
                    Card(
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            navController?.navigate("teacher/students")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer(scaleX = viewStudentsScale, scaleY = viewStudentsScale),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B6B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        interactionSource = viewStudentsIs
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👥", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "View Students",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val addStudentIs = MutableInteractionSource()
                    val addStudentPressed by addStudentIs.collectIsPressedAsState()
                    val addStudentScale by animateFloatAsState(targetValue = if (addStudentPressed) 0.96f else 1f, label = "addStudentScale")
                    Card(
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            navController?.navigate("teacher/students/add")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer(scaleX = addStudentScale, scaleY = addStudentScale),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF6C63FF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        interactionSource = addStudentIs
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("➕", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add Student",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Enhanced PERFORMANCE BUTTONS
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val perfIs = MutableInteractionSource()
                    val perfPressed by perfIs.collectIsPressedAsState()
                    val perfScale by animateFloatAsState(targetValue = if (perfPressed) 0.96f else 1f, label = "perfScale")
                    Card(
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            navController?.navigate("teacher/class/performance")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer(scaleX = perfScale, scaleY = perfScale),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB74D)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        interactionSource = perfIs
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📊", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Class Performance",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val lbIs = MutableInteractionSource()
                    val lbPressed by lbIs.collectIsPressedAsState()
                    val lbScale by animateFloatAsState(targetValue = if (lbPressed) 0.96f else 1f, label = "lbScale")
                    Card(
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            navController?.navigate("teacher/class/leaderboard")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer(scaleX = lbScale, scaleY = lbScale),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF48FB1)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        interactionSource = lbIs
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏆", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Leaderboards",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // REPORTS CARD
                run {
                    val reportsCardIs = MutableInteractionSource()
                    val pressed by reportsCardIs.collectIsPressedAsState()
                    val scale by animateFloatAsState(targetValue = if (pressed) 0.99f else 1f, label = "cardScale2")
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
                        interactionSource = reportsCardIs,
                        onClick = {}
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Assessment, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("AI Reports & Insights", style = MaterialTheme.typography.titleMedium)
                                Text("Analyze student and class performance with charts and tips", color = Color.Gray)
                            }
                            val openIs = MutableInteractionSource()
                            val openPressed by openIs.collectIsPressedAsState()
                            val openScale by animateFloatAsState(targetValue = if (openPressed) 0.96f else 1f, label = "openScale")
                            Button(
                                onClick = { navController?.navigate("teacher/reports") },
                                interactionSource = openIs,
                                modifier = Modifier.graphicsLayer(scaleX = openScale, scaleY = openScale)
                            ) {
                                Text("Open")
                            }
                        }
                    }
                }

                // STUDENT STATISTICS CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📊 Class Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Total Students
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👥", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "24",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total Students",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Active Students
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌟", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "18",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active Today",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Average Progress
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📈", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "78%",
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

                // ENHANCED DASHBOARD CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Dashboard,
                            contentDescription = null,
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "🎯 Teacher Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Manage your students, track progress, and view insights all in one place!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    } // ✅ properly closes Scaffold

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("🚪 Logout") },
            text = { Text("Are you sure you want to logout? You'll need to sign in again to access your teacher account.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        navController?.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Logout", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
