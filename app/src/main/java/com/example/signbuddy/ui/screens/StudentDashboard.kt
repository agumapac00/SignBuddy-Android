package com.example.signbuddy.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavHostController
import com.example.signbuddy.ui.screens.tabs.HomeScreen
import com.example.signbuddy.ui.screens.tabs.LessonsScreen
import com.example.signbuddy.ui.screens.tabs.ProfileScreen

@Composable
fun StudentDashboard(
    navController: NavHostController,
    username: String
) {
    var selectedTab by remember { mutableStateOf(0) }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { 
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.9f else 1f,
                            animationSpec = tween(100),
                            label = "homeTab"
                        )
                        Icon(
                            Icons.Default.Home, 
                            contentDescription = "Home",
                            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { 
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.9f else 1f,
                            animationSpec = tween(100),
                            label = "lessonsTab"
                        )
                        Icon(
                            Icons.Default.Book, 
                            contentDescription = "Lessons",
                            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    },
                    label = { Text("Lessons") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { 
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.9f else 1f,
                            animationSpec = tween(100),
                            label = "profileTab"
                        )
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = "Profile",
                            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    },
                    label = { Text("Profile") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(gradientBackground),
            color = Color.Transparent
        ) {
            when (selectedTab) {
                0 -> HomeScreen(navController = navController, username = username)
                1 -> LessonsScreen(navController = navController)
                2 -> ProfileScreen(username = username, navController = navController)
            }
        }
    }
}
