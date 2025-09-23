package com.example.signbuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.signbuddy.ui.screens.AdminLoginScreen
import com.example.signbuddy.ui.screens.SignBuddyUsernameScreen
import com.example.signbuddy.ui.dashboard.StudentDashboard
import com.example.signbuddy.ui.screens.AchievementsScreen
import com.example.signbuddy.ui.screens.EvaluationTestScreen
import com.example.signbuddy.ui.screens.LeaderboardScreen
import com.example.signbuddy.ui.screens.MultiplayerScreen
import com.example.signbuddy.ui.screens.PracticeScreen
import com.example.signbuddy.ui.screens.TeacherLoginScreen
import com.example.signbuddy.ui.screens.TutorialScreen
import com.example.signbuddy.ui.screens.tabs.LessonsScreen
import com.example.signbuddy.ui.screens.tabs.ProgressScreen
import com.example.signbuddy.ui.theme.SignBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 Ask for camera permission once when activity starts
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }

        setContent {
            SignBuddyTheme {
                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        // Username + Role selection
                        composable("login") {
                            SignBuddyUsernameScreen(navController = navController)
                        }

                        // Teacher login
                        composable("teacherLogin") {
                            TeacherLoginScreen(navController = navController)
                        }

                        // Admin login
                        composable("adminLogin") {
                            AdminLoginScreen(navController = navController)
                        }

                        // Student dashboard (with username argument)
                        composable(
                            route = "studentDashboard/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username =
                                backStackEntry.arguments?.getString("username") ?: "Student"
                            StudentDashboard(
                                navController = navController,
                                username = username
                            )
                        }

                        // Progress
                        composable("progress") {
                            ProgressScreen(navController = navController)
                        }

                        // Achievements
                        composable("achievements") {
                            AchievementsScreen(navController = navController)
                        }

                        // Leaderboard
                        composable("leaderboard") {
                            LeaderboardScreen(navController = navController)
                        }

                        // Tutorial
                        composable("tutorial") {
                            TutorialScreen(navController = navController)
                        }

                        // Lessons
                        composable("lessons") {
                            LessonsScreen(navController = navController)
                        }

                        // Practice screen
                        composable("practice") {
                            PracticeScreen(navController = navController)
                        }
                        // Evaluation Test screen
                        composable("evaluation") {
                            EvaluationTestScreen(navController = navController)
                        }
                        composable("multiplayer") {
                            MultiplayerScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
