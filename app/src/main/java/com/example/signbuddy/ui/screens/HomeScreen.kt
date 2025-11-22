package com.example.signbuddy.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.signbuddy.services.StudentService

@Composable
fun HomeScreen(navController: NavHostController, username: String) {
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )

    // Real data state - keeping functionality but with original UI
    var studentStats by remember { mutableStateOf<StudentService.StudentStats?>(null) }
    var dayStreak by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val studentService = remember { StudentService() }

    // Fetch student stats and calculate day streak
    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            try {
                studentStats = studentService.getStudentStats(username)
                // Use streakDays from StudentStats or calculate based on practice sessions
                studentStats?.let { stats ->
                    dayStreak = if (stats.streakDays > 0) {
                        stats.streakDays
                    } else {
                        // Calculate streak based on practice sessions (rough estimate)
                        (stats.practiceSessions / 7).coerceAtLeast(1)
                    }
                } ?: run {
                    // Create default stats for new users
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
                    dayStreak = 1 // Default streak for new users
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
                dayStreak = 1 // Default streak
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
            dayStreak = 1
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
            .verticalScroll(rememberScrollState())
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Enhanced Welcome Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌟 Welcome, $username!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ready to learn some amazing sign language? Let's start with the ABCs! 🤟",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Days Streak Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B6B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    // --- ADJUSTED PADDING ---
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "$dayStreak",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Day Streak 🔥",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Letters Learned Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4ECDC4)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    // --- ADJUSTED PADDING ---
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (studentStats != null) "${studentStats!!.lettersLearned}" else "0",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Letters Learned 📚",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }


        DashboardCard(
            title = "My Progress 📈",
            subtitle = "Track your learning journey",
            icon = Icons.Default.Star,
            color = Color(0xFF4ECDC4), // Teal
            onClick = { navController.navigate("progress/$username") }
        )

        DashboardCard(
            title = "My Achievements 🏆",
            subtitle = "See your amazing badges",
            icon = Icons.Default.EmojiEvents,
            color = Color(0xFFFF6B6B), // Coral
            onClick = { navController.navigate("achievements/$username") }
        )

        DashboardCard(
            title = "Class Leaderboard 🏅",
            subtitle = "Compete with friends",
            icon = Icons.Default.Leaderboard,
            color = Color(0xFF6C63FF), // Purple
            onClick = { navController.navigate("leaderboard/$username") }
        )
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // --- ADJUSTED HEIGHT ---
            .height(if (subtitle != null) 125.dp else 100.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
