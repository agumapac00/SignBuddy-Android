package com.example.signbuddy.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

// Data class for achievements
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val unlocked: Boolean,
    val category: AchievementCategory
)

enum class AchievementCategory {
    LEARNING, PROGRESS, SKILL, SOCIAL, SPECIAL
}

object AchievementsData {
    val allAchievements = listOf(
        Achievement(
            id = "first_steps",
            title = "🌟 First Steps",
            description = "You learned your first letter! Great job!",
            emoji = "🌟",
            icon = Icons.Default.MilitaryTech,
            unlocked = false, // Will be determined by user's achievement list
            category = AchievementCategory.LEARNING
        ),
        Achievement(
            id = "hot_streak",
            title = "🔥 Hot Streak",
            description = "You practiced for 3 days in a row! Keep it up!",
            emoji = "🔥",
            icon = Icons.Default.EmojiEvents,
            unlocked = false, // Will be determined by user's achievement list
            category = AchievementCategory.PROGRESS
        ),
        Achievement(
            id = "goal_getter",
            title = "🎯 Goal Getter",
            description = "You learned 10 letters! You're on fire!",
            emoji = "🎯",
            icon = Icons.Default.EmojiEvents,
            unlocked = false, // Will be determined by user's achievement list
            category = AchievementCategory.PROGRESS
        ),
        Achievement(
            id = "alphabet_master",
            title = "🏆 Alphabet Master",
            description = "You learned all 26 letters! You're a superstar!",
            emoji = "🏆",
            icon = Icons.Default.EmojiEvents,
            unlocked = false, // Will be determined by user's achievement list
            category = AchievementCategory.LEARNING
        ),
        Achievement(
            id = "practice_champion",
            title = "💪 Practice Champion",
            description = "You practiced for 7 days straight! Incredible!",
            emoji = "💪",
            icon = Icons.Default.EmojiEvents,
            unlocked = false, // Will be determined by user's achievement list
            category = AchievementCategory.PROGRESS
        ),
        Achievement(
            id = "evaluation_expert",
            title = "📝 Evaluation Expert",
            description = "You scored 90%+ in an evaluation! Well done!",
            emoji = "📝",
            icon = Icons.Default.EmojiEvents,
            unlocked = false, // Will be determined by user's achievement list
            category = AchievementCategory.SKILL
        ),
        Achievement(
            id = "speed_demon",
            title = "⚡ Speed Demon",
            description = "You completed a session in under 30 seconds! Lightning fast!",
            emoji = "⚡",
            icon = Icons.Default.EmojiEvents,
            unlocked = false, // Will be determined by user's achievement list
            category = AchievementCategory.SKILL
        ),
        Achievement(
            id = "beginner_badge",
            title = "🥇 Beginner Badge",
            description = "You completed the tutorial! Welcome to SignBuddy!",
            emoji = "🥇",
            icon = Icons.Default.EmojiEvents,
            unlocked = false, // Will be determined by user's achievement list
            category = AchievementCategory.LEARNING
        )
    )

    fun getUnlockedAchievements(): List<Achievement> {
        return allAchievements.filter { it.unlocked }
    }

    fun getAchievementsByCategory(category: AchievementCategory): List<Achievement> {
        return allAchievements.filter { it.category == category }
    }

    fun getAchievementById(id: String): Achievement? {
        return allAchievements.find { it.id == id }
    }
}





