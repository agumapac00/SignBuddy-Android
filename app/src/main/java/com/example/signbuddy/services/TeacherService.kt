package com.example.signbuddy.services

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.Date

class TeacherService {
    private val firestore = FirebaseFirestore.getInstance()
    
    data class ClassStatistics(
        val totalStudents: Int,
        val activeStudents: Int,
        val averageProgress: Float,
        val totalSessions: Int
    )
    
    data class StudentPerformance(
        val studentId: String,
        val studentName: String,
        val progress: Int,
        val isActive: Boolean,
        val lastActive: Date?,
        val totalScore: Int,
        val level: Int
    )
    
    data class LeaderboardEntry(
        val rank: Int,
        val studentName: String,
        val score: Int,
        val level: Int
    )
    
    data class ClassPerformanceStats(
        val totalStudents: Int,
        val averageAccuracy: Float,
        val averageSpeed: Float,
        val totalSessions: Int,
        val accuracyDistribution: Map<String, Int>,
        val commonMistakes: Map<String, Int>,
        val speedByLevel: Map<Int, Float>
    )
    
    data class StudentReport(
        val studentId: String,
        val studentName: String,
        val accuracy: Float,
        val level: Int,
        val totalSessions: Int,
        val totalScore: Int,
        val lettersLearned: Int,
        val lastActive: Date?
    )
    
    /**
     * Get detailed class performance statistics for a teacher
     */
    suspend fun getClassPerformanceStats(teacherId: String): ClassPerformanceStats {
        val studentsSnapshot = firestore
            .collection("studentProfiles")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        
        val students = studentsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
        }
        
        if (students.isEmpty()) {
            return ClassPerformanceStats(
                totalStudents = 0,
                averageAccuracy = 0f,
                averageSpeed = 0f,
                totalSessions = 0,
                accuracyDistribution = emptyMap(),
                commonMistakes = emptyMap(),
                speedByLevel = emptyMap()
            )
        }
        
        // Calculate average accuracy from actual student data
        val averageAccuracy = if (students.isNotEmpty()) {
            students.map { it.averageAccuracy }.average().toFloat()
        } else 0f

        // Calculate average progress (letters learned out of 26)
        val averageProgress = if (students.isNotEmpty()) {
            students.map { s -> (s.lettersLearned.toFloat() / 26f).coerceIn(0f, 1f) }.average().toFloat()
        } else 0f
        
        // Calculate average completion rate based on letters learned and practice sessions
        // This represents how quickly students are progressing through the alphabet
        val averageSpeed = if (students.isNotEmpty()) {
            val completionRates = students.map { student ->
                if (student.practiceSessions > 0) {
                    student.lettersLearned.toFloat() / student.practiceSessions
                } else 0f
            }
            completionRates.average().toFloat()
        } else 0f
        
        // Calculate total sessions from actual practice sessions
        val totalSessions = students.sumOf { it.practiceSessions }
        
        // Create accuracy distribution based on actual averageAccuracy
        val accuracyDistribution = mapOf(
            "0-20%" to students.count { it.averageAccuracy >= 0f && it.averageAccuracy < 0.2f },
            "20-40%" to students.count { it.averageAccuracy >= 0.2f && it.averageAccuracy < 0.4f },
            "40-60%" to students.count { it.averageAccuracy >= 0.4f && it.averageAccuracy < 0.6f },
            "60-80%" to students.count { it.averageAccuracy >= 0.6f && it.averageAccuracy < 0.8f },
            "80-100%" to students.count { it.averageAccuracy >= 0.8f && it.averageAccuracy <= 1.0f }
        )
        
        // Create challenge areas based on actual letters learned data
        // Letters at the end of the alphabet or complex letters are typically harder
        val alphabet = ('A'..'Z').toList()
        val commonMistakes = mutableMapOf<String, Int>()
        
        // Find which letters are most challenging based on letters learned
        val lettersByDifficulty = mutableMapOf<Char, Int>()
        
        students.forEach { student ->
            val lettersLearned = student.lettersLearned
            // Letters A-J are typically learned first
            if (lettersLearned <= 10) {
                alphabet.subList(10, alphabet.size).forEach { letter ->
                    lettersByDifficulty[letter] = (lettersByDifficulty[letter] ?: 0) + 1
                }
            } 
            // Letters K-P are learned next
            else if (lettersLearned <= 16) {
                alphabet.subList(16, alphabet.size).forEach { letter ->
                    lettersByDifficulty[letter] = (lettersByDifficulty[letter] ?: 0) + 1
                }
            }
            // Letters Q-Z are typically the most challenging
            else if (lettersLearned <= 22) {
                alphabet.subList(22, alphabet.size).forEach { letter ->
                    lettersByDifficulty[letter] = (lettersByDifficulty[letter] ?: 0) + 1
                }
            }
            // Letters X, Y, Z are usually the last to be mastered
            else if (lettersLearned <= 25) {
                listOf('X', 'Y', 'Z').forEach { letter ->
                    lettersByDifficulty[letter] = (lettersByDifficulty[letter] ?: 0) + 1
                }
            }
        }
        
        // Get top 5 most challenging letters
        commonMistakes.putAll(
            lettersByDifficulty.entries
                .sortedByDescending { it.value }
                .take(5)
                .associate { (letter, count) -> letter.toString() to count }
        )
        
        // Calculate average performance by level based on actual student data
        val speedByLevel = mutableMapOf<Int, Float>()
        
        val level1Students = students.filter { it.level == 1 }
        if (level1Students.isNotEmpty()) {
            // Lower letters learned per session for beginners
            val avgProgress = level1Students.mapNotNull { student ->
                if (student.practiceSessions > 0) student.lettersLearned.toFloat() / student.practiceSessions else null
            }
            speedByLevel[1] = if (avgProgress.isNotEmpty()) avgProgress.average().toFloat() else 0f
        }
        
        val level2Students = students.filter { it.level == 2 }
        if (level2Students.isNotEmpty()) {
            val avgProgress = level2Students.mapNotNull { student ->
                if (student.practiceSessions > 0) student.lettersLearned.toFloat() / student.practiceSessions else null
            }
            speedByLevel[2] = if (avgProgress.isNotEmpty()) avgProgress.average().toFloat() else 0f
        }
        
        val level3Students = students.filter { it.level == 3 }
        if (level3Students.isNotEmpty()) {
            val avgProgress = level3Students.mapNotNull { student ->
                if (student.practiceSessions > 0) student.lettersLearned.toFloat() / student.practiceSessions else null
            }
            speedByLevel[3] = if (avgProgress.isNotEmpty()) avgProgress.average().toFloat() else 0f
        }
        
        val level4Students = students.filter { it.level == 4 }
        if (level4Students.isNotEmpty()) {
            val avgProgress = level4Students.mapNotNull { student ->
                if (student.practiceSessions > 0) student.lettersLearned.toFloat() / student.practiceSessions else null
            }
            speedByLevel[4] = if (avgProgress.isNotEmpty()) avgProgress.average().toFloat() else 0f
        }
        
        val level5PlusStudents = students.filter { it.level >= 5 }
        if (level5PlusStudents.isNotEmpty()) {
            val avgProgress = level5PlusStudents.mapNotNull { student ->
                if (student.practiceSessions > 0) student.lettersLearned.toFloat() / student.practiceSessions else null
            }
            speedByLevel[5] = if (avgProgress.isNotEmpty()) avgProgress.average().toFloat() else 0f
        }
        
        android.util.Log.d("TeacherService", "Class stats: total=${students.size}, accuracy=$averageAccuracy, speed=$averageSpeed, sessions=$totalSessions")
        
        return ClassPerformanceStats(
            totalStudents = students.size,
            averageAccuracy = averageAccuracy,
            averageSpeed = averageSpeed,
            totalSessions = totalSessions,
            accuracyDistribution = accuracyDistribution,
            commonMistakes = commonMistakes,
            speedByLevel = speedByLevel
        )
    }
    
    /**
     * Get individual student reports for a teacher
     */
    suspend fun getStudentReports(teacherId: String): List<StudentReport> {
        val studentsSnapshot = firestore
            .collection("studentProfiles")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        
        return studentsSnapshot.documents.mapNotNull { doc ->
            val student = doc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
            student?.let {
                StudentReport(
                    studentId = doc.id,
                    studentName = it.username,
                    accuracy = it.averageAccuracy,
                    level = it.level,
                    totalSessions = it.practiceSessions,
                    totalScore = it.totalScore,
                    lettersLearned = it.lettersLearned,
                    lastActive = it.lastActive
                )
            }
        }
    }
    
    /**
     * Get class statistics for a teacher
     */
    suspend fun getClassStatistics(teacherId: String): ClassStatistics {
        val studentsSnapshot = firestore
            .collection("studentProfiles")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        
        val students = studentsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
        }
        
        val totalStudents = students.size
        val activeStudents = students.count { student ->
            student.lastActive?.let { lastActive ->
                val timeDiff = System.currentTimeMillis() - lastActive.time
                timeDiff < 24 * 60 * 60 * 1000 // Active within last 24 hours
            } ?: false
        }
        
        val averageProgress = if (students.isNotEmpty()) {
            students.map { s -> (s.lettersLearned.toFloat() / 26f).coerceIn(0f, 1f) }.average().toFloat()
        } else 0f
        
        val totalSessions = students.sumOf { it.practiceSessions }
        
        return ClassStatistics(
            totalStudents = totalStudents,
            activeStudents = activeStudents,
            averageProgress = averageProgress,
            totalSessions = totalSessions
        )
    }
    
    /**
     * Get all students for a teacher
     */
    suspend fun getStudents(teacherId: String): List<StudentPerformance> {
        val studentsSnapshot = firestore
            .collection("studentProfiles")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        
        return studentsSnapshot.documents.mapNotNull { doc ->
            val student = doc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
            student?.let {
                val isActive = it.lastActive?.let { lastActive ->
                    val timeDiff = System.currentTimeMillis() - lastActive.time
                    timeDiff < 24 * 60 * 60 * 1000 // Active within last 24 hours
                } ?: false
                
                StudentPerformance(
                    studentId = it.uid,
                    studentName = it.displayName,
                    progress = ((it.lettersLearned.toFloat() / 26f) * 100f).toInt().coerceIn(0, 100),
                    isActive = isActive,
                    lastActive = it.lastActive,
                    totalScore = it.totalScore,
                    level = it.level
                )
            }
        }
    }
    
    /**
     * Get leaderboard for a teacher's class
     */
    suspend fun getClassLeaderboard(teacherId: String, limit: Int = 10): List<LeaderboardEntry> {
        try {
            android.util.Log.d("TeacherService", "getClassLeaderboard called with teacherId: $teacherId")
            
            // Fetch all students for this teacher (without orderBy to avoid index requirement)
        val studentsSnapshot = firestore
                .collection("studentProfiles")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        
            android.util.Log.d("TeacherService", "Found ${studentsSnapshot.size()} students for teacher: $teacherId")
            
            // Convert to StudentProfile objects
            val students = studentsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(com.example.signbuddy.data.StudentProfile::class.java)?.also { student ->
                    android.util.Log.d("TeacherService", "Student: ${student.displayName} - Score: ${student.totalScore}")
                }
            }
            
            // Sort by totalScore descending (highest first)
            val sortedStudents = students.sortedByDescending { it.totalScore }
            
            // Take only the limit and convert to LeaderboardEntry
            val leaderboard = sortedStudents.take(limit).mapIndexedNotNull { index, student ->
                LeaderboardEntry(
                    rank = index + 1,
                    studentName = student.displayName,
                    score = student.totalScore,
                    level = student.level
                )
            }
            
            android.util.Log.d("TeacherService", "Returning ${leaderboard.size} leaderboard entries")
            leaderboard.forEachIndexed { index, entry ->
                android.util.Log.d("TeacherService", "Entry $index: ${entry.studentName} - Score: ${entry.score}")
            }
            
            return leaderboard
        } catch (e: Exception) {
            android.util.Log.e("TeacherService", "Error getting class leaderboard", e)
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Add a student to a teacher's class
     */
    suspend fun addStudentToClass(
        teacherId: String,
        studentName: String,
        grade: String,
        emoji: String
    ): Result<String> {
        return try {
            // Generate username from student name (lowercase, replace spaces with underscores)
            val username = studentName.trim().replace(" ", "_").lowercase()
            
            // Check if a student already exists by multiple username variants
            // 1) exact teacher-entered (trimmed)
            val enteredExact = studentName.trim()
            val enteredNormalized = enteredExact.replace(" ", "_")

            var existingStudent = firestore
                .collection("studentProfiles")
                .whereEqualTo("username", enteredExact)
                .limit(1)
                .get()
                .await()

            if (existingStudent.isEmpty) {
                // 2) normalized (spaces to underscores), original casing
                existingStudent = firestore
                    .collection("studentProfiles")
                    .whereEqualTo("username", enteredNormalized)
                    .limit(1)
                    .get()
                    .await()
            }

            if (existingStudent.isEmpty) {
                // 3) lowercased normalized (matches how we generate new usernames)
                existingStudent = firestore
                    .collection("studentProfiles")
                    .whereEqualTo("username", username)
                    .limit(1)
                    .get()
                    .await()
            }

            if (!existingStudent.isEmpty) {
                // Student already exists - update their teacherId to enroll them
                val doc = existingStudent.documents.first()
                val existingStudentProfile = doc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
                
                if (existingStudentProfile != null) {
                    android.util.Log.d("TeacherService", "Student $username already exists, updating teacherId")
                    
                    // Update the existing student's teacherId
                    val updatedProfile = existingStudentProfile.copy(
                        teacherId = teacherId,
                        grade = grade,
                        emoji = emoji,
                        enrolledAt = Date() // Update enrollment date
                    )
                    
                    doc.reference.set(updatedProfile).await()
                    android.util.Log.d("TeacherService", "Successfully enrolled existing student: $username to teacher: $teacherId")
                    return Result.success(existingStudentProfile.uid)
                }
                
                return Result.failure(Exception("Username '$username' already exists but could not be enrolled."))
            }
            
            // Try to find existing user account by username in `users` collection
            val usersSnapshot = firestore
                .collection("users")
                .whereEqualTo("username", enteredExact)
                .limit(1)
                .get()
                .await()
            val usersSnapshot2 = if (usersSnapshot.isEmpty) {
                firestore.collection("users")
                    .whereEqualTo("username", enteredNormalized)
                    .limit(1)
                    .get()
                    .await()
            } else usersSnapshot
            val userDoc = if (usersSnapshot2.isEmpty) {
                firestore.collection("users")
                    .whereEqualTo("username", username)
                    .limit(1)
                    .get()
                    .await()
                    .documents.firstOrNull()
            } else usersSnapshot2.documents.firstOrNull()

            val canonicalUid = userDoc?.getString("uid")
            val studentUid = canonicalUid ?: "student_${System.currentTimeMillis()}_${username}"
            
            // Create/Update student profile using canonical UID if available
            val studentProfile = com.example.signbuddy.data.StudentProfile(
                uid = studentUid,
                username = username,
                displayName = studentName.trim(),
                totalScore = 0,
                totalXp = 0,
                level = 1,
                achievements = emptyList(),
                practiceSessions = 0,
                averageAccuracy = 0f,
                lettersLearned = 0,
                enrolledAt = Date(),
                teacherId = teacherId,
                grade = grade,
                emoji = emoji,
                email = null, // Email is optional
                streakDays = 0,
                lastStreakDate = null
            )
            
            // Save to Firestore (upsert by canonical UID)
            firestore.collection("studentProfiles").document(studentUid).set(studentProfile).await()

            // Optional cleanup: remove any duplicate profiles with the same username but different uid
            val dupes = firestore.collection("studentProfiles")
                .whereEqualTo("username", username)
                .get()
                .await()
            dupes.documents.forEach { d ->
                if (d.id != studentUid) {
                    try { d.reference.delete().await() } catch (_: Exception) {}
                }
            }
            
            android.util.Log.d("TeacherService", "Successfully added student: $username to teacher: $teacherId")
            
            Result.success(studentUid)
        } catch (e: Exception) {
            android.util.Log.e("TeacherService", "Error adding student", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remove a student from a teacher's class
     */
    suspend fun removeStudentFromClass(studentId: String): Result<Unit> {
        return try {
            firestore.collection("studentProfiles").document(studentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Listen to class statistics changes
     */
    fun listenToClassStatistics(teacherId: String): Flow<ClassStatistics> = callbackFlow {
        val listener = firestore
            .collection("studentProfiles")
            .whereEqualTo("teacherId", teacherId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val students = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.signbuddy.data.StudentProfile::class.java)
                    }
                    
                    val totalStudents = students.size
                    val activeStudents = students.count { student ->
                        student.lastActive?.let { lastActive ->
                            val timeDiff = System.currentTimeMillis() - lastActive.time
                            timeDiff < 24 * 60 * 60 * 1000
                        } ?: false
                    }
                    
                    val averageProgress = if (students.isNotEmpty()) {
                        students.map { it.averageAccuracy }.average().toFloat()
                    } else 0f
                    
                    val totalSessions = students.sumOf { it.practiceSessions }
                    
                    val statistics = ClassStatistics(
                        totalStudents = totalStudents,
                        activeStudents = activeStudents,
                        averageProgress = averageProgress,
                        totalSessions = totalSessions
                    )
                    
                    trySend(statistics)
                }
            }
        
        awaitClose { listener.remove() }
    }
}
