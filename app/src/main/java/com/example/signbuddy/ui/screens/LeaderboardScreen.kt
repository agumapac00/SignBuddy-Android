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

// Data class for leaderboard entries
data class LeaderboardEntry(
    val name: String,
    val score: Int,
    val emoji: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavController) {
    // 🌈 Kindergarten-friendly gradient background
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )

    val leaderboard = listOf(
        LeaderboardEntry("Emma", 95, "🌟"),
        LeaderboardEntry("Liam", 88, "⭐"),
        LeaderboardEntry("Sophia", 82, "🎯"),
        LeaderboardEntry("You", 75, "👑"),
        LeaderboardEntry("Noah", 70, "🔥"),
        LeaderboardEntry("Ava", 65, "💪"),
        LeaderboardEntry("Oliver", 60, "🎨"),
        LeaderboardEntry("Isabella", 55, "🚀")
    )

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
                itemsIndexed(leaderboard) { index, entry ->
                    run {
                        val cardIs = MutableInteractionSource()
                        val pressed by cardIs.collectIsPressedAsState()
                        val scalePress by animateFloatAsState(targetValue = if (pressed) 0.99f else 1f, label = "lbCardPress")
                        var mounted by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { mounted = true }
                        val appearScale by animateFloatAsState(targetValue = if (mounted) 1f else 0.96f, label = "lbCardAppear")
                        val appearAlpha by animateFloatAsState(targetValue = if (mounted) 1f else 0f, label = "lbCardAlpha")
                        
                        val isCurrentUser = entry.name == "You"
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
                                
                                // Student emoji
                                Text(
                                    text = entry.emoji,
                                    fontSize = 24.sp
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Name and score
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = entry.name,
                                        fontSize = 18.sp,
                                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrentUser) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${entry.score} points",
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
