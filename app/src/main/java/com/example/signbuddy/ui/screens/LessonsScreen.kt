package com.example.signbuddy.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun LessonsScreen(navController: NavHostController) {
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF7AE), Color(0xFFFFE5C2), Color(0xFFE1F5FE))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Learning Modes",
            style = MaterialTheme.typography.headlineMedium
        )

        // 🔹 Tutorial card navigates to TutorialScreen
        LessonCard(
            title = "Tutorial",
            icon = Icons.Default.MenuBook,
            color = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate("tutorial") } // ✅ this now uses dashboardNavController
        )


        LessonCard(
            title = "Practice",
            icon = Icons.Default.PlayCircle,
            color = MaterialTheme.colorScheme.secondary,
            onClick = { navController.navigate("practice") }
        )

        LessonCard(
            title = "Evaluation",
            icon = Icons.Default.CheckCircle,
            color = MaterialTheme.colorScheme.tertiary,
            onClick = { navController.navigate("evaluation") }
        )

        LessonCard(
            title = "Multiplayer",
            icon = Icons.Default.People,
            color = MaterialTheme.colorScheme.error,
            onClick = { navController.navigate("Multiplayer") }
        )
    }
}

@Composable
fun LessonCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit = {} // ✅ Add onClick parameter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = onClick // ✅ Hook click here
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
