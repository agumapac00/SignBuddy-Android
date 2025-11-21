package com.example.signbuddy.services

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.Date

class StudentService {
    private val firestore = FirebaseFirestore.getInstance()
    
    data class StudentStats(
        val totalScore: Int,
        val totalXp: Int,
        val level: Int,
        val practiceSessions: Int,
        val averageAccuracy: Float,
        val lettersLearned: Int,
        val perfectSigns: Int,
        val streakDays: Int,
        val achievements: List<String>
    )
    
    data class LeaderboardEntry(
        val rank: Int,
        val studentName: String,
        val score: Int,
        val level: Int
    )
    
    /**
     * Get student statistics by username
     */
    suspend fun getStudentStats(username: String): StudentStats? {
        return try {
            android.util.Log.d("StudentService", "=== GET STUDENT STATS START ===")
            android.util.Log.d("StudentService", "Username: $username")
            
            val studentSnapshot = firestore
                .collection("studentProfiles")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            
            android.util.Log.d("StudentService", "Student snapshot size: ${studentSnapshot.size()}")
            
            if (studentSnapshot.isEmpty) {
                android.util.Log.w("StudentService", "No student found for username: $username")
                null
            } else {
                val student = studentSnapshot.documents.first().toObject(com.example.signbuddy.data.StudentProfile::class.java)
                android.util.Log.d("StudentService", "Found student: $student")
                android.util.Log.d("StudentService", "Student doc: ${studentSnapshot.documents.first().data}")
                android.util.Log.d("StudentService", "Achievements from doc: ${studentSnapshot.documents.first().get("achievements")}")
                
                student?.let {
                    // Handle potential null achievements field from Firestore
                    val studentAchievements = it.achievements ?: emptyList()
                    
                    android.util.Log.d("StudentService", "Student's achievements list: $studentAchievements")
                    android.util.Log.d("StudentService", "Checking for beginner_badge: ${studentAchievements.contains("beginner_badge")}")
                    
                    val stats = StudentStats(
                        totalScore = it.totalScore,
                        totalXp = it.totalXp,
                        level = it.level,
                        practiceSessions = it.practiceSessions,
                        averageAccuracy = it.averageAccuracy,
                        lettersLearned = it.lettersLearned, // Use actual letters learned
                        perfectSigns = (it.averageAccuracy * it.practiceSessions).toInt(), // Estimate
                        streakDays = it.streakDays, // Use the stored streak value
                        achievements = studentAchievements
                    )
                    
                    android.util.Log.d("StudentService", "Created stats with achievements: ${stats.achievements}")
                    android.util.Log.d("StudentService", "Size of achievements: ${stats.achievements.size}")
                    android.util.Log.d("StudentService", "Checking beginner_badge in stats: ${stats.achievements.contains("beginner_badge")}")
                    android.util.Log.d("StudentService", "=== GET STUDENT STATS END ===")
                    stats
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentService", "Error getting student stats", e)
            null
        }
    }
    
    /**
     * Get global leaderboard
     */
    suspend fun getGlobalLeaderboard(limit: Int = 10): List<LeaderboardEntry> {
        return try {
            val studentsSnapshot = firestore
                .collection("studentProfiles")
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            studentsSnapshot.documents.mapIndexedNotNull { index, doc ->
                val student = doc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
                student?.let {
                    LeaderboardEntry(
                        rank = index + 1,
                        studentName = it.displayName,
                        score = it.totalScore,
                        level = it.level
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Update student progress after a practice session
     */
    suspend fun updateStudentProgress(
        username: String,
        score: Int,
        accuracy: Float,
        sessionCompleted: Boolean = true
    ): Result<Unit> {
        return try {
            val studentSnapshot = firestore
                .collection("studentProfiles")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            
            if (studentSnapshot.isEmpty) {
                Result.failure(Exception("Student not found"))
            } else {
                val studentDoc = studentSnapshot.documents.first()
                val student = studentDoc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
                
                student?.let {
                    // IMPORTANT: Copy ALL fields to prevent data loss when using .set()
                    val updatedStudent = it.copy(
                        uid = it.uid,
                        username = it.username,
                        displayName = it.displayName,
                        totalScore = it.totalScore + score,
                        totalXp = it.totalXp,
                        level = it.level,
                        achievements = it.achievements,
                        practiceSessions = if (sessionCompleted) it.practiceSessions + 1 else it.practiceSessions,
                        averageAccuracy = (it.averageAccuracy + accuracy) / 2, // Simple average
                        lettersLearned = it.lettersLearned,
                        enrolledAt = it.enrolledAt,
                        teacherId = it.teacherId,
                        grade = it.grade,
                        emoji = it.emoji,
                        email = it.email,
                        lastActive = Date(),
                        streakDays = it.streakDays,
                        lastStreakDate = it.lastStreakDate
                    )
                    
                    studentDoc.reference.set(updatedStudent).await()
                    Result.success(Unit)
                } ?: Result.failure(Exception("Student data not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add achievement to student
     */
    suspend fun addAchievement(username: String, achievementId: String): Result<Unit> {
        return try {
            val studentSnapshot = firestore
                .collection("studentProfiles")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            
            if (studentSnapshot.isEmpty) {
                Result.failure(Exception("Student not found"))
            } else {
                val studentDoc = studentSnapshot.documents.first()
                val student = studentDoc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
                
                student?.let {
                    val updatedAchievements = if (achievementId in it.achievements) {
                        it.achievements
                    } else {
                        it.achievements + achievementId
                    }
                    
                    // IMPORTANT: Copy ALL fields to prevent data loss when using .set()
                    val updatedStudent = it.copy(
                        uid = it.uid,
                        username = it.username,
                        displayName = it.displayName,
                        totalScore = it.totalScore,
                        totalXp = it.totalXp,
                        level = it.level,
                        achievements = updatedAchievements,
                        practiceSessions = it.practiceSessions,
                        averageAccuracy = it.averageAccuracy,
                        lettersLearned = it.lettersLearned,
                        enrolledAt = it.enrolledAt,
                        teacherId = it.teacherId,
                        grade = it.grade,
                        emoji = it.emoji,
                        email = it.email,
                        lastActive = Date(),
                        streakDays = it.streakDays,
                        lastStreakDate = it.lastStreakDate
                    )
                    
                    studentDoc.reference.set(updatedStudent).await()
                    Result.success(Unit)
                } ?: Result.failure(Exception("Student data not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Listen to student stats changes
     */
    fun listenToStudentStats(username: String): Flow<StudentStats?> = callbackFlow {
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
                    val stats = student?.let {
                        StudentStats(
                            totalScore = it.totalScore,
                            totalXp = it.totalXp,
                            level = it.level,
                            practiceSessions = it.practiceSessions,
                            averageAccuracy = it.averageAccuracy,
                            lettersLearned = it.lettersLearned, // Use actual letters learned
                            perfectSigns = (it.averageAccuracy * it.practiceSessions).toInt(),
                            streakDays = it.streakDays, // Use the stored streak value
                            achievements = it.achievements
                        )
                    }
                    trySend(stats)
                } else {
                    trySend(null)
                }
            }
        
        awaitClose { listener.remove() }
    }
}
