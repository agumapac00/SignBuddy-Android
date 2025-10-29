package com.example.signbuddy.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.signbuddy.services.StudentService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(navController: NavHostController, username: String) {
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )
    
    // Real data state
    var studentStats by remember { mutableStateOf<StudentService.StudentStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val studentService = remember { StudentService() }
    
    // Fetch student stats
    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            try {
                studentStats = studentService.getStudentStats(username)
                // Provide fallback data if no stats found
                if (studentStats == null) {
                    studentStats = StudentService.StudentStats(
                        totalScore = 0,
                        totalXp = 0,
                        level = 1,
                        practiceSessions = 0,
                        averageAccuracy = 0f,
                        lettersLearned = 0,
                        perfectSigns = 0,
                        streakDays = 1,
                        achievements = emptyList()
                    )
                }
            } catch (e: Exception) {
                // Create default stats on error
                studentStats = StudentService.StudentStats(
                    totalScore = 0,
                    totalXp = 0,
                    level = 1,
                    practiceSessions = 0,
                    averageAccuracy = 0f,
                    lettersLearned = 0,
                    perfectSigns = 0,
                    streakDays = 1,
                    achievements = emptyList()
                )
            }
            isLoading = false
        } else {
            // Create default stats for empty username
            studentStats = StudentService.StudentStats(
                totalScore = 0,
                totalXp = 0,
                level = 1,
                practiceSessions = 0,
                averageAccuracy = 0f,
                lettersLearned = 0,
                perfectSigns = 0,
                streakDays = 1,
                achievements = emptyList()
            )
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📈 Progress", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📈 Your Learning Progress",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Track your amazing journey in ASL!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (studentStats != null) {
                // Progress Stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "📊 Learning Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProgressStatCard(
                                title = "Letters Learned",
                                value = "${studentStats!!.lettersLearned}",
                                emoji = "📚",
                                color = Color(0xFF4ECDC4)
                            )
                            ProgressStatCard(
                                title = "Current Level",
                                value = "${studentStats!!.level}",
                                emoji = "⭐",
                                color = Color(0xFFFF6B6B)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProgressStatCard(
                                title = "Total Score",
                                value = "${studentStats!!.totalScore}",
                                emoji = "🏆",
                                color = Color(0xFFFFD93D)
                            )
                            ProgressStatCard(
                                title = "Accuracy",
                                value = "${(studentStats!!.averageAccuracy * 100).toInt()}%",
                                emoji = "🎯",
                                color = Color(0xFF6BCF7F)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProgressStatCard(
                                title = "Practice Sessions",
                                value = "${studentStats!!.practiceSessions}",
                                emoji = "💪",
                                color = Color(0xFF9B59B6)
                            )
                            ProgressStatCard(
                                title = "Achievements",
                                value = "${studentStats!!.achievements.size}",
                                emoji = "🏅",
                                color = Color(0xFFE67E22)
                            )
                        }
                    }
                }
                
                // Progress Chart (Simplified)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "📈 Learning Journey",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Simple progress visualization
                        val progress = (studentStats!!.lettersLearned.toFloat() / 26f).coerceAtMost(1f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(
                                        Color(0xFF4ECDC4),
                                        RoundedCornerShape(10.dp)
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Progress: ${(progress * 100).toInt()}% (${studentStats!!.lettersLearned}/26 letters)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // No data state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Progress Data",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start learning to see your progress!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun ProgressStatCard(
    title: String,
    value: String,
    emoji: String,
    color: Color
) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}