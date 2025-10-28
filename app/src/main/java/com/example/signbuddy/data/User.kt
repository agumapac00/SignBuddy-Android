package com.example.signbuddy.data

import java.util.Date

enum class UserType {
    STUDENT,
    TEACHER
}

data class User(
    val uid: String,
    val email: String,
    val username: String,
    val displayName: String,
    val userType: UserType,
    val createdAt: Date
)

