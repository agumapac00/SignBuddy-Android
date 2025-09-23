package com.example.signbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerScreen(navController: NavController? = null) {
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF7AE), Color(0xFFE1F5FE))
    )

    // Placeholder states
    var player1Score by remember { mutableStateOf(40) }
    var player2Score by remember { mutableStateOf(60) }
    var timer by remember { mutableStateOf("00:20") }
    var currentSign by remember { mutableStateOf("A") } // Placeholder for the hand sign
    var feedbackText by remember { mutableStateOf("Who will win this round?") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiplayer Battle") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer
            Text(
                "Time Left: $timer",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Score Bars
            ScoreBar("Player 1", player1Score, Color(0xFF82B1FF))
            Spacer(modifier = Modifier.height(8.dp))
            ScoreBar("Player 2", player2Score, Color(0xFFFF8A80))

            Spacer(modifier = Modifier.height(24.dp))

            // Player Panels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlayerPanel(name = "Player 1", score = player1Score, color = Color(0xFF82B1FF))
                PlayerPanel(name = "Player 2", score = player2Score, color = Color(0xFFFF8A80))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current Sign Prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Perform this sign:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.Gray, shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        currentSign,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feedback
            Text(
                feedbackText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons: Next Round / Submit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        // Placeholder: Start new round
                        feedbackText = "New round started!"
                        currentSign = "B"
                        player1Score += 5
                        player2Score += 3
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next Round")
                }

                Button(
                    onClick = {
                        // Placeholder: Submit round or end match
                        feedbackText = "Round submitted!"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
fun PlayerPanel(name: String, score: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(150.dp)
            .padding(8.dp)
            .background(color.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Score: $score",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScoreBar(playerName: String, score: Int, color: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "$playerName: $score pts",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        LinearProgressIndicator(
            progress = score / 100f,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(vertical = 4.dp)
        )
    }
}
