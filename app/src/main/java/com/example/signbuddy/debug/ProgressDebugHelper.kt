package com.example.signbuddy.debug

import android.util.Log
import com.example.signbuddy.services.ProgressTrackingService
import com.example.signbuddy.services.StudentService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ProgressDebugHelper {
    companion object {
        private const val TAG = "ProgressDebug"
        
        fun logProgressUpdate(update: ProgressTrackingService.ProgressUpdate) {
            Log.d(TAG, "=== PROGRESS UPDATE ===")
            Log.d(TAG, "XP Gained: ${update.xpGained}")
            Log.d(TAG, "Score Gained: ${update.scoreGained}")
            Log.d(TAG, "Level Up: ${update.levelUp}")
            Log.d(TAG, "New Level: ${update.newLevel}")
            Log.d(TAG, "Achievements Unlocked: ${update.achievementsUnlocked}")
            Log.d(TAG, "========================")
        }
        
        fun logSessionResult(result: ProgressTrackingService.SessionResult) {
            Log.d(TAG, "=== SESSION RESULT ===")
            Log.d(TAG, "Mode: ${result.mode}")
            Log.d(TAG, "Accuracy: ${result.accuracy}")
            Log.d(TAG, "Time Spent: ${result.timeSpent}s")
            Log.d(TAG, "Letters Completed: ${result.lettersCompleted}")
            Log.d(TAG, "Perfect Signs: ${result.perfectSigns}")
            Log.d(TAG, "Mistakes: ${result.mistakes}")
            Log.d(TAG, "======================")
        }
        
        suspend fun testProgressTracking(
            username: String,
            progressTrackingService: ProgressTrackingService,
            studentService: StudentService,
            scope: CoroutineScope
        ) {
            Log.d(TAG, "=== TESTING PROGRESS TRACKING ===")
            Log.d(TAG, "Username: $username")
            
            // Get current stats
            val currentStats = studentService.getStudentStats(username)
            Log.d(TAG, "Current Stats: $currentStats")
            
            // Create a test session result
            val testSession = ProgressTrackingService.SessionResult(
                mode = "tutorial",
                accuracy = 1.0f,
                timeSpent = 60,
                lettersCompleted = 5,
                perfectSigns = 5,
                mistakes = 0
            )
            
            logSessionResult(testSession)
            
            // Update progress
            scope.launch {
                val result = progressTrackingService.updateProgress(username, testSession)
                result.onSuccess { update ->
                    logProgressUpdate(update)
                    
                    // Get updated stats
                    val updatedStats = studentService.getStudentStats(username)
                    Log.d(TAG, "Updated Stats: $updatedStats")
                }.onFailure { error ->
                    Log.e(TAG, "Progress update failed: ${error.message}")
                }
            }
        }
    }
}







