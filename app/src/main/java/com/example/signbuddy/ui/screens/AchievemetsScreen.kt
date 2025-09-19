package com.example.signbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// 🔹 Data Model for Achievements
data class Achievement(
    val title: String,
    val description: String,
    val unlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavController) {
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF7AE), Color(0xFFFFE5C2), Color(0xFFE1F5FE))
    )

    // 🔹 Example achievements list (can be replaced with database or API later)
    val achievements = listOf(
        Achievement("Beginner Badge", "Completed your first lesson", unlocked = true),
        Achievement("Consistency Star", "Logged in 5 days in a row", unlocked = false),
        Achievement("Master Learner", "Completed all lessons", unlocked = false)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏆 Your Achievements",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Show achievements dynamically
            achievements.forEach { achievement ->
                AchievementBadge(achievement)
            }
        }
    }
}

@Composable
fun AchievementBadge(achievement: Achievement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) Color.White.copy(alpha = 0.95f)
            else Color.LightGray.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = achievement.title,
                tint = if (achievement.unlocked) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = if (achievement.unlocked) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (achievement.unlocked)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else Color.Gray.copy(alpha = 0.6f)
                )
            }
        }
    }
}
