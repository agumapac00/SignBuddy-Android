package com.example.signbuddy.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreService {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            Result.success(querySnapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findStudentByUsername(username: String): Result<StudentProfile?> {
        return try {
            val querySnapshot = firestore.collection("studentProfiles")
                .whereEqualTo("username", username)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                Result.success(null)
            } else {
                val document = querySnapshot.documents.first()
                val studentProfile = StudentProfile(
                    uid = document.getString("uid") ?: "",
                    username = document.getString("username") ?: "",
                    displayName = document.getString("displayName") ?: "",
                    totalScore = document.getLong("totalScore")?.toInt() ?: 0,
                    totalXp = document.getLong("totalXp")?.toInt() ?: 0,
                    level = document.getLong("level")?.toInt() ?: 1,
                    achievements = document.get("achievements") as? List<String> ?: emptyList(),
                    practiceSessions = document.getLong("practiceSessions")?.toInt() ?: 0,
                    averageAccuracy = (document.getDouble("averageAccuracy") ?: 0.0).toFloat(),
                    lettersLearned = document.getLong("lettersLearned")?.toInt() ?: 0,
                    enrolledAt = (document.getDate("enrolledAt") ?: Date()),
                    teacherId = document.getString("teacherId"),
                    grade = document.getString("grade"),
                    emoji = document.getString("emoji"),
                    email = document.getString("email"),
                    lastActive = document.getDate("lastActive"),
                    streakDays = document.getLong("streakDays")?.toInt() ?: 0,
                    lastStreakDate = document.getDate("lastStreakDate")
                )
                Result.success(studentProfile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(user: User): Result<Unit> {
        return try {
            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "username" to user.username,
                "displayName" to user.displayName,
                "userType" to user.userType.name,
                "createdAt" to user.createdAt
            )
            
            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createStudentProfile(studentProfile: StudentProfile): Result<Unit> {
        return try {
            val profileData = hashMapOf(
                "uid" to studentProfile.uid,
                "username" to studentProfile.username,
                "displayName" to studentProfile.displayName,
                "totalScore" to studentProfile.totalScore,
                "totalXp" to studentProfile.totalXp,
                "level" to studentProfile.level,
                "achievements" to studentProfile.achievements,
                "practiceSessions" to studentProfile.practiceSessions,
                "averageAccuracy" to studentProfile.averageAccuracy,
                "lettersLearned" to studentProfile.lettersLearned,
                "enrolledAt" to studentProfile.enrolledAt,
                "teacherId" to studentProfile.teacherId,
                "grade" to studentProfile.grade,
                "emoji" to studentProfile.emoji,
                "email" to studentProfile.email,
                "lastActive" to studentProfile.lastActive,
                "streakDays" to studentProfile.streakDays,
                "lastStreakDate" to studentProfile.lastStreakDate
            )
            
            firestore.collection("studentProfiles")
                .document(studentProfile.uid)
                .set(profileData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findTeacherByUsername(username: String): Map<String, Any>? {
        return try {
            val querySnapshot = firestore.collection("teacherProfiles")
                .whereEqualTo("username", username)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                null
            } else {
                querySnapshot.documents.first().data
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun findTeacherByEmail(email: String): Map<String, Any>? {
        return try {
            val querySnapshot = firestore.collection("teacherProfiles")
                .whereEqualTo("email", email)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                null
            } else {
                querySnapshot.documents.first().data
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createTeacherProfile(teacherProfile: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("teacherProfiles")
                .document(teacherProfile["uid"] as String)
                .set(teacherProfile)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findTeacherByUid(uid: String): Map<String, Any>? {
        return try {
            val document = firestore.collection("teacherProfiles")
                .document(uid)
                .get()
                .await()
            
            if (document.exists()) {
                document.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateLoginStreak(username: String): Result<Unit> {
        return try {
            val studentSnapshot = firestore.collection("studentProfiles")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            
            if (studentSnapshot.isEmpty) {
                Result.failure(Exception("Student not found"))
            } else {
                val studentDoc = studentSnapshot.documents.first()
                val student = studentDoc.toObject(StudentProfile::class.java)
                
                student?.let {
                    val now = Date()
                    val calendar = java.util.Calendar.getInstance().apply {
                        time = now
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val today = calendar.time
                    
                    val lastStreakCalendar = it.lastStreakDate?.let { lastDate ->
                        java.util.Calendar.getInstance().apply {
                            time = lastDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                    }
                    val lastStreakDay = lastStreakCalendar?.time
                    
                    val daysDiff = lastStreakDay?.let { lastDay ->
                        (calendar.timeInMillis - lastStreakCalendar.timeInMillis) / (24 * 60 * 60 * 1000)
                    } ?: -1
                    
                    android.util.Log.d("FirestoreService", "=== UPDATING LOGIN STREAK ===")
                    android.util.Log.d("FirestoreService", "Username: $username")
                    android.util.Log.d("FirestoreService", "Current streak: ${it.streakDays}")
                    android.util.Log.d("FirestoreService", "Last streak date: ${it.lastStreakDate}")
                    android.util.Log.d("FirestoreService", "Current date: $today")
                    android.util.Log.d("FirestoreService", "Days since last streak: $daysDiff")
                    
                    val newStreakDays = when {
                        // First time logging in (never logged in before)
                        it.lastStreakDate == null -> {
                            android.util.Log.d("FirestoreService", "First login ever -> Starting streak at 1")
                            1
                        }
                        // Login on the same day - keep current streak
                        daysDiff == 0L -> {
                            android.util.Log.d("FirestoreService", "Same day login -> Keeping streak at ${it.streakDays}")
                            it.streakDays
                        }
                        // Login exactly one day after - increment streak
                        daysDiff == 1L -> {
                            val newStreak = it.streakDays + 1
                            android.util.Log.d("FirestoreService", "Consecutive day login -> Incrementing streak from ${it.streakDays} to $newStreak")
                            newStreak
                        }
                        // More than one day gap - reset streak to 1
                        else -> {
                            android.util.Log.d("FirestoreService", "Gap in login -> Resetting streak to 1")
                            1
                        }
                    }
                    
                    android.util.Log.d("FirestoreService", "New streak days: $newStreakDays")
                    
                    // IMPORTANT: Copy ALL fields to prevent data loss when using .set()
                    val updatedStudent = it.copy(
                        uid = it.uid,
                        username = it.username,
                        displayName = it.displayName,
                        totalScore = it.totalScore,
                        totalXp = it.totalXp,
                        level = it.level,
                        achievements = it.achievements,
                        practiceSessions = it.practiceSessions,
                        averageAccuracy = it.averageAccuracy,
                        lettersLearned = it.lettersLearned,
                        enrolledAt = it.enrolledAt,
                        teacherId = it.teacherId,
                        grade = it.grade,
                        emoji = it.emoji,
                        email = it.email,
                        lastActive = now,
                        streakDays = newStreakDays,
                        lastStreakDate = today
                    )
                    
                    studentDoc.reference.set(updatedStudent).await()
                    android.util.Log.d("FirestoreService", "Streak updated successfully")
                    Result.success(Unit)
                } ?: Result.failure(Exception("Student data not found"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService", "Error updating login streak", e)
            Result.failure(e)
        }
    }
}
