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
import com.google.accompanist.navigation.animation.AnimatedNavHost
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.signbuddy.ui.screens.SignBuddyUsernameScreen
import com.example.signbuddy.ui.dashboard.StudentDashboard
import com.example.signbuddy.ui.screens.AchievementsScreen
import com.example.signbuddy.ui.screens.EvaluationTestScreen
import com.example.signbuddy.ui.screens.LeaderboardScreen
import com.example.signbuddy.ui.screens.MultiplayerScreen
import com.example.signbuddy.ui.screens.PracticeScreen
import com.example.signbuddy.ui.screens.TeacherLoginScreen
import com.example.signbuddy.ui.screens.teacher.TeacherDashboardScreen
import com.example.signbuddy.ui.screens.teacher.TeacherCreateQuizScreen
import com.example.signbuddy.ui.screens.teacher.TeacherAssignQuizScreen
import com.example.signbuddy.ui.screens.teacher.TeacherClassPerformanceScreen
import com.example.signbuddy.ui.screens.teacher.TeacherLeaderboardsScreen
import com.example.signbuddy.ui.screens.teacher.TeacherReportsScreen
import com.example.signbuddy.ui.screens.teacher.TeacherStudentsScreen
import com.example.signbuddy.ui.screens.teacher.TeacherAddStudentScreen
import com.example.signbuddy.ui.screens.TutorialScreen
import com.example.signbuddy.ui.screens.tabs.LessonsScreen
import com.example.signbuddy.ui.screens.tabs.ProgressScreen
import com.example.signbuddy.ui.theme.SignBuddyTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class)
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

                    AnimatedNavHost(
                        navController = navController,
                        startDestination = "login",
                        enterTransition = {
                            fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(180))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(220))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f, animationSpec = tween(180))
                        }
                    ) {
                        // Username + Role selection
                        composable("login") {
                            SignBuddyUsernameScreen(navController = navController)
                        }

                        // Teacher login
                        composable("teacherLogin") {
                            TeacherLoginScreen(navController = navController)
                        }

                        // Teacher dashboard and tools
                        composable("teacher/dashboard") {
                            TeacherDashboardScreen(navController = navController)
                        }
                        composable("teacher/quizzes/create") {
                            TeacherCreateQuizScreen(navController = navController)
                        }
                        composable("teacher/quizzes/assign") {
                            TeacherAssignQuizScreen(navController = navController)
                        }
                        composable("teacher/class/performance") {
                            TeacherClassPerformanceScreen(navController = navController)
                        }
                        composable("teacher/class/leaderboard") {
                            TeacherLeaderboardsScreen(navController = navController)
                        }
                        composable("teacher/reports") {
                            TeacherReportsScreen(navController = navController)
                        }
                        composable("teacher/students") {
                            TeacherStudentsScreen(navController = navController)
                        }
                        composable("teacher/students/add") {
                            TeacherAddStudentScreen(navController = navController)
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
