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
import androidx.compose.runtime.remember
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.signbuddy.ui.screens.SignBuddyUsernameScreen
import com.example.signbuddy.ui.screens.StudentRegisterScreen
import com.example.signbuddy.ui.screens.TeacherLoginScreen
import com.example.signbuddy.ui.screens.TeacherRegisterScreen
import com.example.signbuddy.ui.screens.teacher.TeacherDashboardScreen
import com.example.signbuddy.ui.dashboard.StudentDashboard
import com.example.signbuddy.viewmodels.AuthViewModel
import com.example.signbuddy.viewmodels.MultiplayerViewModel
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
                    val authViewModel = remember { AuthViewModel() }
                    val multiplayerViewModel = remember { MultiplayerViewModel() }

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
                        // Student login/role selection
                        composable("login") {
                            SignBuddyUsernameScreen(navController = navController)
                        }

                        // Student registration
                        composable("studentRegister") {
                            StudentRegisterScreen(navController = navController)
                        }

                        // Teacher login
                        composable("teacherLogin") {
                            TeacherLoginScreen(navController = navController, authViewModel = authViewModel)
                        }

                        // Teacher registration
                        composable("teacherRegister") {
                            TeacherRegisterScreen(navController = navController, authViewModel = authViewModel)
                        }

                        // Teacher dashboard and tools
                        composable("teacher/dashboard") {
                            TeacherDashboardScreen(navController = navController, authViewModel = authViewModel)
                        }
                        composable("teacher/quizzes/create") {
                            TeacherCreateQuizScreen(navController = navController)
                        }
                        composable("teacher/quizzes/assign") {
                            TeacherAssignQuizScreen(navController = navController)
                        }
                        composable(
                            route = "teacher/class/performance/{teacherId}",
                            arguments = listOf(navArgument("teacherId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val teacherId = backStackEntry.arguments?.getString("teacherId") ?: ""
                            TeacherClassPerformanceScreen(navController = navController, teacherId = teacherId)
                        }
                        composable("teacher/class/leaderboard") {
                            TeacherLeaderboardsScreen(navController = navController, authViewModel = authViewModel)
                        }
                        composable(
                            route = "teacher/reports/{teacherId}",
                            arguments = listOf(navArgument("teacherId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val teacherId = backStackEntry.arguments?.getString("teacherId") ?: ""
                            TeacherReportsScreen(navController = navController, teacherId = teacherId)
                        }
                        composable("teacher/students") {
                            TeacherStudentsScreen(navController = navController, authViewModel = authViewModel)
                        }
                        composable("teacher/students/add") {
                            TeacherAddStudentScreen(navController = navController, authViewModel = authViewModel)
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
                        composable(
                            route = "progress/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: ""
                            ProgressScreen(navController = navController, username = username)
                        }

                        // Achievements
                        composable(
                            route = "achievements/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: ""
                            AchievementsScreen(navController = navController, username = username)
                        }

                        // Leaderboard
                        composable(
                            route = "leaderboard/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: ""
                            LeaderboardScreen(navController = navController, username = username)
                        }

                        // Tutorial
                        composable(
                            route = "tutorial/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: ""
                            TutorialScreen(navController = navController, username = username)
                        }

                        // Lessons
                        composable(
                            route = "lessons/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: ""
                            LessonsScreen(navController = navController, username = username)
                        }

                        // Practice screen
                        composable(
                            route = "practice/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: ""
                            PracticeScreen(navController = navController, username = username)
                        }
                        // Evaluation Test screen
                        composable(
                            route = "evaluation/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: ""
                            EvaluationTestScreen(navController = navController, username = username)
                        }
                        composable(
                            route = "multiplayer/{username}",
                            arguments = listOf(navArgument("username") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: ""
                            MultiplayerScreen(navController = navController, multiplayerViewModel = multiplayerViewModel, username = username)
                        }
                    }
                }
            }
        }
    }
}
