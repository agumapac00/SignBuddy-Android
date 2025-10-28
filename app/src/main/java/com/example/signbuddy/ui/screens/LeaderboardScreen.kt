package com.example.signbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.services.StudentService
import com.example.signbuddy.services.TeacherService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class for leaderboard entries
data class LeaderboardEntry(
    val name: String,
    val score: Int,
    val emoji: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavController, username: String = "") {
    // 🌈 Kindergarten-friendly gradient background
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )

    // Real data state
    var classLeaderboard by remember { mutableStateOf<List<TeacherService.LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isEnrolled by remember { mutableStateOf(false) }
    var teacherId by remember { mutableStateOf<String?>(null) }
    val studentService = remember { StudentService() }
    val teacherService = remember { com.example.signbuddy.services.TeacherService() }
    val scope = rememberCoroutineScope()
    
    // Fetch student's enrollment status and class leaderboard
    LaunchedEffect(username) {
        try {
            if (username.isNotEmpty()) {
                android.util.Log.d("LeaderboardScreen", "Fetching leaderboard for username: $username")
                
                // Get student profile from Firestore to check teacherId
                val studentSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("studentProfiles")
                    .whereEqualTo("username", username)
                    .limit(1)
                    .get()
                    .await()
                
                android.util.Log.d("LeaderboardScreen", "Student snapshot size: ${studentSnapshot.size()}")
                
                if (!studentSnapshot.isEmpty) {
                    val student = studentSnapshot.documents.first().toObject(com.example.signbuddy.data.StudentProfile::class.java)
                    android.util.Log.d("LeaderboardScreen", "Student profile: $student")
                    student?.let {
                        teacherId = it.teacherId
                        isEnrolled = !it.teacherId.isNullOrEmpty()
                        
                        android.util.Log.d("LeaderboardScreen", "teacherId: $teacherId, isEnrolled: $isEnrolled")
                        
                        // If enrolled, fetch class leaderboard
                        if (isEnrolled && teacherId != null) {
                            android.util.Log.d("LeaderboardScreen", "Fetching class leaderboard for teacherId: $teacherId")
                            classLeaderboard = teacherService.getClassLeaderboard(teacherId!!, 20)
                            android.util.Log.d("LeaderboardScreen", "Class leaderboard entries: ${classLeaderboard.size}")
                            classLeaderboard.forEachIndexed { index, entry ->
                                android.util.Log.d("LeaderboardScreen", "Entry $index: ${entry.studentName} - ${entry.score}")
                            }
                        }
                    }
                } else {
                    android.util.Log.w("LeaderboardScreen", "No student found with username: $username")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LeaderboardScreen", "Error fetching leaderboard", e)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏅 Class Leaderboard", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header items
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌟 Top Learners! 🌟",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "See who's doing great with their sign language!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            // Leaderboard entries
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (classLeaderboard.isEmpty() && !isEnrolled) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👨‍🏫", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Not Enrolled Yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please enroll to your teacher first before you can see the class leaderboards",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (classLeaderboard.isEmpty() && isEnrolled) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(20.dp)
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
                                text = "No classmates yet. Be the first to start learning!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(classLeaderboard) { index, entry ->
                    run {
                        val cardIs = MutableInteractionSource()
                        val pressed by cardIs.collectIsPressedAsState()
                        val scalePress by animateFloatAsState(targetValue = if (pressed) 0.99f else 1f, label = "lbCardPress")
                        var mounted by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { mounted = true }
                        val appearScale by animateFloatAsState(targetValue = if (mounted) 1f else 0.96f, label = "lbCardAppear")
                        val appearAlpha by animateFloatAsState(targetValue = if (mounted) 1f else 0f, label = "lbCardAlpha")
                        
                        val isCurrentUser = entry.studentName == username
                        val rankColor = when (index) {
                            0 -> Color(0xFFFFD700) // Gold
                            1 -> Color(0xFFC0C0C0) // Silver
                            2 -> Color(0xFFCD7F32) // Bronze
                            else -> MaterialTheme.colorScheme.primary
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(scaleX = scalePress * appearScale, scaleY = scalePress * appearScale, alpha = appearAlpha),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentUser) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else 
                                    Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isCurrentUser) 8.dp else 4.dp
                            ),
                            interactionSource = cardIs,
                            onClick = {}
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rank with special styling for top 3
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(
                                            color = rankColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(25.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (index) {
                                            0 -> "🥇"
                                            1 -> "🥈"
                                            2 -> "🥉"
                                            else -> "#${index + 1}"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (index < 3) 20.sp else 16.sp,
                                        color = rankColor
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Student emoji (using level-based emoji)
                                Text(
                                    text = when {
                                        entry.level >= 10 -> "🌟"
                                        entry.level >= 7 -> "⭐"
                                        entry.level >= 4 -> "🎯"
                                        else -> "🔥"
                                    },
                                    fontSize = 24.sp
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Name and score
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = entry.studentName,
                                        fontSize = 18.sp,
                                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrentUser) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${entry.score} points • Level ${entry.level}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                
                                // Score badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = rankColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${entry.score}",
                                        fontWeight = FontWeight.Bold,
                                        color = rankColor,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
