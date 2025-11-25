package com.example.signbuddy.services

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.Date

class ProgressTrackingService {
    private val firestore = FirebaseFirestore.getInstance()
    
    data class ProgressUpdate(
        val xpGained: Int,
        val scoreGained: Int,
        val achievementsUnlocked: List<String>,
        val levelUp: Boolean = false,
        val newLevel: Int? = null
    )
    
    data class SessionResult(
        val mode: String, // "tutorial", "practice", "evaluation", "multiplayer"
        val accuracy: Float,
        val timeSpent: Long, // in seconds
        val lettersCompleted: Int,
        val perfectSigns: Int,
        val mistakes: Int,
        val actualScore: Int = 0 // The actual score earned in the session
    )
    
    /**
     * Calculate XP and score based on session results
     */
    fun calculateProgress(sessionResult: SessionResult): ProgressUpdate {
        // Special rule: In multiplayer, if the player earned zero actual score,
        // award zero XP and zero score to avoid misleading rewards
        if (sessionResult.mode == "multiplayer" && sessionResult.actualScore <= 0) {
            return ProgressUpdate(
                xpGained = 0,
                scoreGained = 0,
                achievementsUnlocked = emptyList(),
                levelUp = false,
                newLevel = null
            )
        }

        val baseXP = when (sessionResult.mode) {
            "tutorial" -> 10
            "practice" -> 15
            "evaluation" -> 25
            "multiplayer" -> 20
            else -> 10
        }
        
        // XP multipliers based on performance
        val accuracyMultiplier = when {
            sessionResult.accuracy >= 0.9f -> 1.5f
            sessionResult.accuracy >= 0.7f -> 1.2f
            sessionResult.accuracy >= 0.5f -> 1.0f
            else -> 0.7f
        }
        
        val timeMultiplier = when {
            sessionResult.timeSpent < 60 -> 1.3f // Quick completion bonus
            sessionResult.timeSpent < 300 -> 1.1f
            else -> 1.0f
        }
        
        val xpGained = (baseXP * accuracyMultiplier * timeMultiplier).toInt()
        
        // If actualScore is provided (> 0), use it directly to avoid mismatches
        // This applies to multiplayer and any mode that reports actual in-game score
        val scoreGained = if (sessionResult.actualScore > 0) {
            sessionResult.actualScore
        } else {
            (xpGained * 10) + (sessionResult.perfectSigns * 5) - (sessionResult.mistakes * 2)
        }
        
        // Check for achievements
        val achievementsUnlocked = checkAchievements(sessionResult)
        
        return ProgressUpdate(
            xpGained = xpGained,
            scoreGained = maxOf(0, scoreGained),
            achievementsUnlocked = achievementsUnlocked
        )
    }
    
    /**
     * Update student progress after a session
     */
    suspend fun updateProgress(
        username: String,
        sessionResult: SessionResult
    ): Result<ProgressUpdate> {
        return try {
            android.util.Log.d("ProgressTrackingService", "=== UPDATE PROGRESS START ===")
            android.util.Log.d("ProgressTrackingService", "Username: $username")
            android.util.Log.d("ProgressTrackingService", "Session Result: $sessionResult")
            
            val progressUpdate = calculateProgress(sessionResult)
            android.util.Log.d("ProgressTrackingService", "Calculated Progress: $progressUpdate")
            
            val studentSnapshot = firestore
                .collection("studentProfiles")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            
            android.util.Log.d("ProgressTrackingService", "Student snapshot size: ${studentSnapshot.size()}")
            
            if (studentSnapshot.isEmpty) {
                android.util.Log.e("ProgressTrackingService", "Student not found for username: $username")
                Result.failure(Exception("Student not found"))
            } else {
                val studentDoc = studentSnapshot.documents.first()
                val student = studentDoc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
                
                android.util.Log.d("ProgressTrackingService", "Found student: $student")
                android.util.Log.d("ProgressTrackingService", "Student doc data: ${studentDoc.data}")
                android.util.Log.d("ProgressTrackingService", "Achievements from doc: ${studentDoc.get("achievements")}")
                android.util.Log.d("ProgressTrackingService", "Student achievements field: ${student?.achievements}")
                
                student?.let {
                    // Validate achievements field - it might be null or not deserialized correctly
                    val currentAchievements = it.achievements ?: emptyList()
                    android.util.Log.d("ProgressTrackingService", "=== STARTING UPDATE FOR ${it.username} ===")
                    android.util.Log.d("ProgressTrackingService", "Current student achievements: $currentAchievements")
                    android.util.Log.d("ProgressTrackingService", "Current letters learned: ${it.lettersLearned}")
                    android.util.Log.d("ProgressTrackingService", "Current streak days: ${it.streakDays}")
                    android.util.Log.d("ProgressTrackingService", "Already has first_steps? ${currentAchievements.contains("first_steps")}")
                    android.util.Log.d("ProgressTrackingService", "Already has beginner_badge? ${currentAchievements.contains("beginner_badge")}")
                    
                    val newTotalScore = it.totalScore + progressUpdate.scoreGained
                    val newXP = it.totalXp + progressUpdate.xpGained
                    val newLevel = calculateLevel(newXP)
                    val levelUp = newLevel > it.level
                    
                    // ONLY update letters learned for evaluation mode
                    // Practice and tutorial don't count as "learned"
                    // Cap at 26 (total letters in alphabet)
                    val newLettersLearned = if (sessionResult.mode == "evaluation") {
                        (it.lettersLearned + sessionResult.lettersCompleted).coerceAtMost(26)
                    } else {
                        it.lettersLearned // Don't update letters learned for tutorial or practice
                    }
                    
                    android.util.Log.d("ProgressTrackingService", "📊 LETTERS: Current=${it.lettersLearned}, New=${sessionResult.lettersCompleted}, Mode=${sessionResult.mode}, Total=$newLettersLearned")
                    android.util.Log.d("ProgressTrackingService", "✅ Will update letters learned in mode '${sessionResult.mode}': ${sessionResult.mode == "evaluation"}")
                    if (sessionResult.mode == "evaluation") {
                        android.util.Log.d("ProgressTrackingService", "📝 EVALUATION MODE: Adding ${sessionResult.lettersCompleted} correct letters to lettersLearned")
                    } else {
                        android.util.Log.d("ProgressTrackingService", "📝 ${sessionResult.mode.uppercase()} MODE: Not updating lettersLearned (only evaluation mode counts)")
                    }
                    
                    // ONLY check progress-based achievements for practice and evaluation modes
                    // Tutorial mode should ONLY give beginner_badge
                    val progressAchievements = if (sessionResult.mode != "tutorial") {
                        checkProgressBasedAchievements(it, sessionResult.lettersCompleted)
                    } else {
                        emptyList<String>()
                    }
                    
                    android.util.Log.d("ProgressTrackingService", "🎯 Session achievements: ${progressUpdate.achievementsUnlocked}")
                    android.util.Log.d("ProgressTrackingService", "🎯 Progress achievements: $progressAchievements")
                    
                    // Combine ALL new achievements
                    val allNewAchievements = (progressUpdate.achievementsUnlocked + progressAchievements).distinct()
                    val updatedAchievements = (currentAchievements + allNewAchievements).distinct()
                    
                    android.util.Log.d("ProgressTrackingService", "✅ ALL NEW ACHIEVEMENTS: $allNewAchievements")
                    android.util.Log.d("ProgressTrackingService", "✅ TOTAL ACHIEVEMENTS: $updatedAchievements")
                    android.util.Log.d("ProgressTrackingService", "  Total Score: ${it.totalScore} -> $newTotalScore")
                    android.util.Log.d("ProgressTrackingService", "  Total XP: ${it.totalXp} -> $newXP")
                    android.util.Log.d("ProgressTrackingService", "  Level: ${it.level} -> $newLevel")
                    android.util.Log.d("ProgressTrackingService", "  Letters Learned: ${it.lettersLearned} -> $newLettersLearned")
                    
                    // IMPORTANT: Copy ALL fields to prevent data loss when using .set()
                    val updatedStudent = it.copy(
                        uid = it.uid,
                        username = it.username,
                        displayName = it.displayName,
                        totalScore = newTotalScore,
                        totalXp = newXP,
                        level = newLevel,
                        achievements = updatedAchievements, // This should save properly now
                        practiceSessions = if (sessionResult.mode != "tutorial") it.practiceSessions + 1 else it.practiceSessions, // Tutorial doesn't count
                        averageAccuracy = if (sessionResult.mode != "tutorial") calculateNewAverageAccuracy(it.averageAccuracy, it.practiceSessions, sessionResult.accuracy) else it.averageAccuracy,
                        lettersLearned = if (sessionResult.mode == "evaluation") newLettersLearned else it.lettersLearned, // Only evaluation updates lettersLearned
                        enrolledAt = it.enrolledAt,
                        teacherId = it.teacherId,
                        grade = it.grade,
                        emoji = it.emoji,
                        email = it.email,
                        lastActive = Date(),
                        streakDays = it.streakDays,
                        lastStreakDate = it.lastStreakDate
                    )
                    
                    android.util.Log.d("ProgressTrackingService", "=== SAVING TO FIRESTORE ===")
                    android.util.Log.d("ProgressTrackingService", "Achievements to save: $updatedAchievements")
                    android.util.Log.d("ProgressTrackingService", "Achievement type: ${updatedAchievements.javaClass.name}")
                    android.util.Log.d("ProgressTrackingService", "Saving with uid: ${updatedStudent.uid}, username: ${updatedStudent.username}")
                    
                    // Save using set() with the data class (Firestore handles List<String> automatically)
                    studentDoc.reference.set(updatedStudent).await()
                    
                    android.util.Log.d("ProgressTrackingService", "✅ SAVED SUCCESSFULLY!")
                    android.util.Log.d("ProgressTrackingService", "Verifying save - reading back from database...")
                    
                    // Verify the save by reading back immediately
                    val verifyDoc = studentDoc.reference.get().await()
                    val verifyAchievements = verifyDoc.get("achievements")
                    android.util.Log.d("ProgressTrackingService", "Verified achievements in database: $verifyAchievements")
                    android.util.Log.d("ProgressTrackingService", "Verify type: ${verifyAchievements?.javaClass?.name}")
                    
                    // Return ALL achievements that were just unlocked (both session and progress-based)
                    val finalUpdate = progressUpdate.copy(
                        achievementsUnlocked = allNewAchievements,
                        levelUp = levelUp,
                        newLevel = if (levelUp) newLevel else null
                    )
                    
                    android.util.Log.d("ProgressTrackingService", "Final progress update: $finalUpdate")
                    android.util.Log.d("ProgressTrackingService", "=== UPDATE PROGRESS END ===")
                    
                    Result.success(finalUpdate)
                } ?: run {
                    android.util.Log.e("ProgressTrackingService", "Student data not found in document")
                    Result.failure(Exception("Student data not found"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProgressTrackingService", "Error updating progress", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check for achievements based on session results
     */
    private fun checkAchievements(sessionResult: SessionResult): List<String> {
        android.util.Log.d("ProgressTrackingService", "=== CHECKING ACHIEVEMENTS ===")
        android.util.Log.d("ProgressTrackingService", "Mode: ${sessionResult.mode}")
        android.util.Log.d("ProgressTrackingService", "Accuracy: ${sessionResult.accuracy}")
        android.util.Log.d("ProgressTrackingService", "Letters Completed: ${sessionResult.lettersCompleted}")
        android.util.Log.d("ProgressTrackingService", "Perfect Signs: ${sessionResult.perfectSigns}")
        android.util.Log.d("ProgressTrackingService", "Time Spent: ${sessionResult.timeSpent}s")
        
        val achievements = mutableListOf<String>()
        
        // Accuracy-based achievements
        when {
            sessionResult.accuracy >= 1.0f -> achievements.add("perfect_session")
            sessionResult.accuracy >= 0.9f -> achievements.add("excellent_accuracy")
            sessionResult.accuracy >= 0.8f -> achievements.add("good_accuracy")
        }
        
        // Speed-based achievements
        when {
            sessionResult.timeSpent < 30 -> achievements.add("speed_demon")
            sessionResult.timeSpent < 60 -> achievements.add("quick_learner")
        }
        
        // Mode-specific achievements
        when (sessionResult.mode) {
            "tutorial" -> {
                // ALWAYS unlock beginner badge when completing tutorial - this is the main tutorial completion achievement
                achievements.add("beginner_badge")
                android.util.Log.d("ProgressTrackingService", "✅ Added beginner_badge for tutorial completion")
                android.util.Log.d("ProgressTrackingService", "Letters completed in session: ${sessionResult.lettersCompleted}")
                
                if (sessionResult.lettersCompleted >= 26) {
                    achievements.add("tutorial_master")
                    android.util.Log.d("ProgressTrackingService", "✅ Added tutorial_master (all 26 letters)")
                }
                if (sessionResult.lettersCompleted >= 10) {
                    achievements.add("tutorial_explorer")
                    android.util.Log.d("ProgressTrackingService", "✅ Added tutorial_explorer (10+ letters)")
                }
            }
            "practice" -> {
                if (sessionResult.lettersCompleted >= 20) achievements.add("practice_champion")
                if (sessionResult.perfectSigns >= 10) achievements.add("perfect_practice")
            }
            "evaluation" -> {
                if (sessionResult.accuracy >= 0.9f) achievements.add("evaluation_expert")
                if (sessionResult.lettersCompleted >= 15) achievements.add("evaluation_warrior")
            }
            "multiplayer" -> {
                if (sessionResult.accuracy >= 0.8f) achievements.add("multiplayer_master")
                if (sessionResult.lettersCompleted >= 10) achievements.add("team_player")
            }
        }
        
        // Letters learned achievements (check student's total letters learned)
        // Note: We need to access student's current lettersLearned from the database
        // This will be handled in the updateProgress method after fetching student data
        
        android.util.Log.d("ProgressTrackingService", "Total achievements found: ${achievements.size}")
        android.util.Log.d("ProgressTrackingService", "Achievements: $achievements")
        android.util.Log.d("ProgressTrackingService", "=== END ACHIEVEMENTS CHECK ===")
        
        return achievements
    }
    
    /**
     * Check for achievements based on cumulative student progress
     */
    private fun checkProgressBasedAchievements(
        student: com.example.signbuddy.data.StudentProfile,
        newLettersLearned: Int
    ): List<String> {
        val achievements = mutableListOf<String>()
        
        // Use validated achievements list (handle null case)
        val currentAchievements = student.achievements ?: emptyList()
        
        val currentTotalLetters = student.lettersLearned
        val newTotalLetters = currentTotalLetters + newLettersLearned
        
        android.util.Log.d("ProgressTrackingService", "=== CHECKING PROGRESS ACHIEVEMENTS ===")
        android.util.Log.d("ProgressTrackingService", "Current total letters: $currentTotalLetters")
        android.util.Log.d("ProgressTrackingService", "New letters this session: $newLettersLearned")
        android.util.Log.d("ProgressTrackingService", "New total letters: $newTotalLetters")
        android.util.Log.d("ProgressTrackingService", "Has first_steps already? ${currentAchievements.contains("first_steps")}")
        
        // First letter achievement - if they had 0 letters and now have letters
        if (currentTotalLetters == 0 && newLettersLearned > 0 && !currentAchievements.contains("first_steps")) {
            achievements.add("first_steps")
            android.util.Log.d("ProgressTrackingService", "✅ ADDING first_steps (0 letters -> $newLettersLearned letters)")
        } else {
            android.util.Log.d("ProgressTrackingService", "❌ NOT adding first_steps: current=$currentTotalLetters, new=$newLettersLearned, has=${currentAchievements.contains("first_steps")}")
        }
        
        // Goal getter - 10 letters reached (checking if they just passed 10)
        if (currentTotalLetters < 10 && newTotalLetters >= 10 && !currentAchievements.contains("goal_getter")) {
            achievements.add("goal_getter")
            android.util.Log.d("ProgressTrackingService", "✅ ADDING goal_getter ($currentTotalLetters -> $newTotalLetters)")
        }
        
        // Alphabet master - all 26 letters (checking if they just completed all 26)
        if (currentTotalLetters < 26 && newTotalLetters >= 26 && !currentAchievements.contains("alphabet_master")) {
            achievements.add("alphabet_master")
            android.util.Log.d("ProgressTrackingService", "✅ ADDING alphabet_master ($currentTotalLetters -> $newTotalLetters)")
        } else {
            android.util.Log.d("ProgressTrackingService", "❌ NOT adding alphabet_master: current=$currentTotalLetters, newTotal=$newTotalLetters, has=${currentAchievements.contains("alphabet_master")}")
        }
        
        // Hot streak - 3+ consecutive days
        if (student.streakDays >= 3 && !currentAchievements.contains("hot_streak")) {
            achievements.add("hot_streak")
            android.util.Log.d("ProgressTrackingService", "✅ Adding hot_streak (3 day streak)")
        }
        
        // Practice champion - 7 days straight (streak-based, checked via streakDays)
        if (student.streakDays >= 7 && !currentAchievements.contains("practice_champion")) {
            achievements.add("practice_champion")
            android.util.Log.d("ProgressTrackingService", "✅ Adding practice_champion (7 day streak)")
        }
        
        android.util.Log.d("ProgressTrackingService", "Progress-based achievements to add: $achievements")
        android.util.Log.d("ProgressTrackingService", "=== END PROGRESS ACHIEVEMENTS CHECK ===")
        return achievements
    }
    
    /**
     * Calculate level based on XP
     */
    private fun calculateLevel(xp: Int): Int {
        return when {
            xp >= 1000 -> 10
            xp >= 800 -> 9
            xp >= 650 -> 8
            xp >= 500 -> 7
            xp >= 400 -> 6
            xp >= 300 -> 5
            xp >= 200 -> 4
            xp >= 100 -> 3
            xp >= 50 -> 2
            else -> 1
        }
    }
    
    /**
     * Calculate new average accuracy
     */
    private fun calculateNewAverageAccuracy(
        currentAverage: Float,
        currentSessions: Int,
        newAccuracy: Float
    ): Float {
        return if (currentSessions == 0) {
            newAccuracy
        } else {
            (currentAverage * currentSessions + newAccuracy) / (currentSessions + 1)
        }
    }
    
    /**
     * Get achievement details
     */
    fun getAchievementDetails(achievementId: String): Pair<String, String> {
        return when (achievementId) {
            "beginner_badge" -> "Beginner Badge" to "You completed the tutorial! Welcome to SignBuddy! 🥇"
            "first_steps" -> "First Steps" to "You learned your first letter! Great job! 🌟"
            "hot_streak" -> "Hot Streak" to "You practiced for 3 days in a row! Keep it up! 🔥"
            "goal_getter" -> "Goal Getter" to "You learned 10 letters! You're on fire! 🎯"
            "alphabet_master" -> "Alphabet Master" to "You learned all 26 letters! You're a superstar! 🏆"
            "practice_champion" -> "Practice Champion" to "You practiced for 7 days straight! Incredible! 💪"
            "evaluation_expert" -> "Evaluation Expert" to "You scored 90%+ in an evaluation! Well done! 📝"
            "speed_demon" -> "Speed Demon" to "You completed a session in under 30 seconds! Lightning fast! ⚡"
            "perfect_session" -> "Perfect Session" to "Complete a session with 100% accuracy! 🎯"
            "excellent_accuracy" -> "Excellent Accuracy" to "Achieve 90%+ accuracy in a session! ⭐"
            "good_accuracy" -> "Good Accuracy" to "Achieve 80%+ accuracy in a session! 👍"
            "quick_learner" -> "Quick Learner" to "Complete a session in under 1 minute! 🏃"
            "tutorial_master" -> "Tutorial Master" to "Complete all 26 letters in tutorial! 📚"
            "tutorial_explorer" -> "Tutorial Explorer" to "Complete 10+ letters in tutorial! 🔍"
            "perfect_practice" -> "Perfect Practice" to "Get 10+ perfect signs in practice! ✨"
            "evaluation_warrior" -> "Evaluation Warrior" to "Complete 15+ letters in evaluation! ⚔️"
            "multiplayer_master" -> "Multiplayer Master" to "Score 80%+ in multiplayer! 🏆"
            "team_player" -> "Team Player" to "Complete 10+ letters in multiplayer! 🤝"
            "consistent_learner" -> "Consistent Learner" to "Complete 5+ letters in any session! 📈"
            else -> "Unknown Achievement" to "Mystery achievement unlocked! 🎉"
        }
    }
    
    /**
     * Listen to student progress changes
     */
    fun listenToProgress(username: String): Flow<com.example.signbuddy.data.StudentProfile?> = callbackFlow {
        val listener = firestore
            .collection("studentProfiles")
            .whereEqualTo("username", username)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val student = snapshot.documents.first().toObject(com.example.signbuddy.data.StudentProfile::class.java)
                    trySend(student)
                } else {
                    trySend(null)
                }
            }
        
        awaitClose { listener.remove() }
    }
}
