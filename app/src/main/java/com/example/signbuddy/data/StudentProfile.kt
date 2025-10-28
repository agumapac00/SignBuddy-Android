package com.example.signbuddy.data

import java.util.Date

data class StudentProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val totalScore: Int = 0,
    val totalXp: Int = 0,
    val level: Int = 1,
    val achievements: List<String> = emptyList(),
    val practiceSessions: Int = 0,
    val averageAccuracy: Float = 0f,
    val lettersLearned: Int = 0,
    val enrolledAt: Date = Date(),
    val teacherId: String? = null,
    val grade: String? = null,
    val emoji: String? = null,
    val email: String? = null,
    val streakDays: Int = 0,
    val lastStreakDate: Date? = null,
    val lastActive: Date? = null
) {
    // ✅ Firestore requires a no-argument constructor
    constructor() : this(
        uid = "",
        username = "",
        displayName = "",
        totalScore = 0,
        totalXp = 0,
        level = 1,
        achievements = emptyList(),
        practiceSessions = 0,
        averageAccuracy = 0f,
        lettersLearned = 0,
        enrolledAt = Date(),
        teacherId = null,
        grade = null,
        emoji = null,
        email = null,
        streakDays = 0,
        lastStreakDate = null,
        lastActive = null
    )
}
