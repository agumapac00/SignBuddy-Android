package com.example.signbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.navigation.NavController
import com.example.signbuddy.data.AchievementsData
import com.example.signbuddy.ui.components.rememberSoundEffects
import com.example.signbuddy.ui.components.rememberHapticFeedback
import com.example.signbuddy.services.StudentService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavController, username: String = "") {
    // Log the username to debug
    android.util.Log.d("AchievementsScreen", "🔍 AchievementsScreen loaded with username: '$username'")
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )

    val soundEffects = rememberSoundEffects()
    val hapticFeedback = rememberHapticFeedback()
    
    // Real data state
    var studentStats by remember { mutableStateOf<StudentService.StudentStats?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            val studentService = remember { StudentService() }
            val scope = rememberCoroutineScope()
    
    // Fetch student stats on load
    LaunchedEffect(username) {
        android.util.Log.d("AchievementsScreen", "=== LOADING ACHIEVEMENTS ===")
        android.util.Log.d("AchievementsScreen", "Username received: $username")
        
        if (username.isNotEmpty()) {
            try {
                android.util.Log.d("AchievementsScreen", "Fetching stats for: $username")
                studentStats = studentService.getStudentStats(username)
                
                android.util.Log.d("AchievementsScreen", "Stats fetched: $studentStats")
                android.util.Log.d("AchievementsScreen", "Achievements from stats: ${studentStats?.achievements}")
                
                // Provide fallback data if no stats found
                if (studentStats == null) {
                    android.util.Log.w("AchievementsScreen", "No stats found for username: $username")
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
                } else {
                    android.util.Log.d("AchievementsScreen", "Stats loaded successfully!")
                    android.util.Log.d("AchievementsScreen", "Achievement count: ${studentStats?.achievements?.size}")
                    studentStats?.achievements?.forEach { achievement ->
                        android.util.Log.d("AchievementsScreen", "  - $achievement")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AchievementsScreen", "Error in initial load", e)
                e.printStackTrace()
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
        }
        isLoading = false
    }
    
    // Refresh stats periodically to show newly unlocked achievements
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000) // Refresh every 2 seconds
            if (username.isNotEmpty()) {
                try {
                    studentStats = studentService.getStudentStats(username)
                } catch (e: Exception) {
                    // Keep existing stats on error
                }
            }
        }
    }
    
    // Get achievements with real unlock status
    val achievements = AchievementsData.allAchievements.map { achievement ->
        val isUnlocked = studentStats?.achievements?.contains(achievement.id) ?: false
        achievement.copy(unlocked = isUnlocked)
    }
    val unlockedCount = achievements.count { it.unlocked }
    
    // Log achievement status only when it changes
    LaunchedEffect(studentStats?.achievements) {
        if (studentStats != null) {
            android.util.Log.d("AchievementsScreen", "=== ACHIEVEMENT STATUS UPDATE ===")
            android.util.Log.d("AchievementsScreen", "Total achievements: ${achievements.size}")
            android.util.Log.d("AchievementsScreen", "Unlocked: $unlockedCount")
            android.util.Log.d("AchievementsScreen", "Beginner Badge: ${achievements.find { it.id == "beginner_badge" }?.unlocked}")
            android.util.Log.d("AchievementsScreen", "Student's achievement list: ${studentStats?.achievements}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 My Achievements", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { 
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        navController.popBackStack() 
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
            // Header with celebration
            Text(
                text = "🎉 You're Amazing! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Keep learning and unlock more achievements!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Achievement statistics
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
                        text = "📊 Your Progress",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Unlocked achievements
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$unlockedCount",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Unlocked",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Total achievements
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${achievements.size}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Progress percentage
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(unlockedCount * 100 / achievements.size)}%",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Complete",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = unlockedCount.toFloat() / achievements.size.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Achievements list header
                        Text(
                            text = "🏆 All Achievements",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Achievements list
                items(achievements) { achievement ->
                    AchievementCard(achievement = achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: com.example.signbuddy.data.Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.unlocked) 6.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Achievement emoji/icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (achievement.unlocked) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Gray.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Achievement details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (achievement.unlocked) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color.Gray.copy(alpha = 0.7f)
                    },
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (achievement.unlocked) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    } else {
                        Color.Gray.copy(alpha = 0.5f)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (achievement.category) {
                            com.example.signbuddy.data.AchievementCategory.LEARNING -> Color(0xFF4CAF50)
                            com.example.signbuddy.data.AchievementCategory.PROGRESS -> Color(0xFF2196F3)
                            com.example.signbuddy.data.AchievementCategory.SKILL -> Color(0xFFFF9800)
                            com.example.signbuddy.data.AchievementCategory.SOCIAL -> Color(0xFF9C27B0)
                            com.example.signbuddy.data.AchievementCategory.SPECIAL -> Color(0xFFFFD700)
                        }.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = achievement.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (achievement.category) {
                            com.example.signbuddy.data.AchievementCategory.LEARNING -> Color(0xFF2E7D32)
                            com.example.signbuddy.data.AchievementCategory.PROGRESS -> Color(0xFF1565C0)
                            com.example.signbuddy.data.AchievementCategory.SKILL -> Color(0xFFE65100)
                            com.example.signbuddy.data.AchievementCategory.SOCIAL -> Color(0xFF6A1B9A)
                            com.example.signbuddy.data.AchievementCategory.SPECIAL -> Color(0xFFF57F17)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status indicator
            if (achievement.unlocked) {
                Text(
                    text = "✅",
                    fontSize = 24.sp
                )
            } else {
                Text(
                    text = "🔒",
                    fontSize = 24.sp
                )
            }
        }
    }
}
