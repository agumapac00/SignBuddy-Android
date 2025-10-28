package com.example.signbuddy.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.unit.sp
import com.example.signbuddy.services.TeacherService
import com.example.signbuddy.viewmodels.AuthViewModel

// Data class for leaderboard entries
data class LeaderboardEntry(
    val rank: String,
    val name: String,
    val score: String,
    val medal: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLeaderboardsScreen(navController: NavController? = null, authViewModel: AuthViewModel? = null) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 Class Leaderboards", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
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
    ) { inner ->
        var showConfetti by remember { mutableStateOf(false) }
        var leaderboardEntries by remember { mutableStateOf<List<TeacherService.LeaderboardEntry>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var teacherId by remember { mutableStateOf("") }
        val teacherService = remember { TeacherService() }
        val scope = rememberCoroutineScope()
        
        // Fetch leaderboard data
        LaunchedEffect(Unit) {
            if (authViewModel != null) {
                val teacherInfo = authViewModel.getCurrentTeacherInfo()
                android.util.Log.d("TeacherLeaderboardsScreen", "Teacher info: $teacherInfo")
                teacherInfo?.let { info ->
                    teacherId = info["uid"] as? String ?: ""
                    android.util.Log.d("TeacherLeaderboardsScreen", "Teacher ID: $teacherId")
                    
                    if (teacherId.isNotEmpty()) {
                        try {
                            android.util.Log.d("TeacherLeaderboardsScreen", "Fetching leaderboard for teacher: $teacherId")
                            leaderboardEntries = teacherService.getClassLeaderboard(teacherId, 10)
                            android.util.Log.d("TeacherLeaderboardsScreen", "Got ${leaderboardEntries.size} leaderboard entries")
                            leaderboardEntries.forEachIndexed { index, entry ->
                                android.util.Log.d("TeacherLeaderboardsScreen", "Entry $index: ${entry.studentName} - Score: ${entry.score}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TeacherLeaderboardsScreen", "Error fetching leaderboard", e)
                        }
                        isLoading = false
                    }
                }
            }
        }
        
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
                Text("🏆", style = MaterialTheme.typography.headlineLarge)
                Column {
                    Text(
                        text = "Class Leaderboards",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See how your students are performing! 🌟",
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🥇", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Top 5 Performers",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "Here are your star students! Keep encouraging them! 🌟",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (leaderboardEntries.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🏆", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Students Yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add students to see the leaderboard!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Enhanced leaderboard entries
                        leaderboardEntries.forEach { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when(entry.rank) {
                                    1 -> Color(0xFFFFD700).copy(alpha = 0.1f)
                                    2 -> Color(0xFFC0C0C0).copy(alpha = 0.1f)
                                    3 -> Color(0xFFCD7F32).copy(alpha = 0.1f)
                                    else -> Color(0xFFF5F5F5)
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = when(entry.rank) {
                                        1 -> "🥇"
                                        2 -> "🥈"
                                        3 -> "🥉"
                                        else -> "⭐"
                                    },
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "${entry.rank}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = entry.studentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${entry.score} pts",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    }
                }
            }
            
            // Celebrate button only
            val celebrateIs = MutableInteractionSource()
            val celebratePressed by celebrateIs.collectIsPressedAsState()
            val celebrateScale by animateFloatAsState(targetValue = if (celebratePressed) 0.96f else 1f, label = "celebrateLb")
            Button(
                onClick = { showConfetti = true },
                shape = RoundedCornerShape(16.dp),
                interactionSource = celebrateIs,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = celebrateScale, scaleY = celebrateScale),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
            ) {
                Text(
                    text = "🎉 Celebrate Winners",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            com.example.signbuddy.ui.screens.teacher.components.ConfettiOverlay(visible = showConfetti) { showConfetti = false }
        }
    }
}


