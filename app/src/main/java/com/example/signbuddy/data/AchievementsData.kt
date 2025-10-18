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
            unlocked = true,
            category = AchievementCategory.LEARNING
        ),
        Achievement(
            id = "hot_streak",
            title = "🔥 Hot Streak",
            description = "You practiced for 3 days in a row! Keep it up!",
            emoji = "🔥",
            icon = Icons.Default.EmojiEvents,
            unlocked = true,
            category = AchievementCategory.PROGRESS
        ),
        Achievement(
            id = "perfect_score",
            title = "⭐ Perfect Score",
            description = "You got 5 signs perfect in a row! Amazing!",
            emoji = "⭐",
            icon = Icons.Default.Star,
            unlocked = false,
            category = AchievementCategory.SKILL
        ),
        Achievement(
            id = "goal_getter",
            title = "🎯 Goal Getter",
            description = "You learned 10 letters! You're on fire!",
            emoji = "🎯",
            icon = Icons.Default.EmojiEvents,
            unlocked = true,
            category = AchievementCategory.PROGRESS
        ),
        Achievement(
            id = "alphabet_master",
            title = "🏆 Alphabet Master",
            description = "You learned all 26 letters! You're a superstar!",
            emoji = "🏆",
            icon = Icons.Default.EmojiEvents,
            unlocked = false,
            category = AchievementCategory.LEARNING
        ),
        Achievement(
            id = "practice_champion",
            title = "💪 Practice Champion",
            description = "You practiced for 7 days straight! Incredible!",
            emoji = "💪",
            icon = Icons.Default.EmojiEvents,
            unlocked = false,
            category = AchievementCategory.PROGRESS
        ),
        Achievement(
            id = "creative_learner",
            title = "🎨 Creative Learner",
            description = "You tried 5 different practice modes! So creative!",
            emoji = "🎨",
            icon = Icons.Default.EmojiEvents,
            unlocked = false,
            category = AchievementCategory.SKILL
        ),
        Achievement(
            id = "sign_language_royalty",
            title = "👑 Sign Language King/Queen",
            description = "You mastered the entire alphabet! You're the best!",
            emoji = "👑",
            icon = Icons.Default.EmojiEvents,
            unlocked = false,
            category = AchievementCategory.SPECIAL
        ),
        Achievement(
            id = "evaluation_expert",
            title = "📝 Evaluation Expert",
            description = "You completed your first evaluation test! Well done!",
            emoji = "📝",
            icon = Icons.Default.EmojiEvents,
            unlocked = true,
            category = AchievementCategory.SKILL
        ),
        Achievement(
            id = "speed_demon",
            title = "⚡ Speed Demon",
            description = "You completed an evaluation in under 2 minutes! Lightning fast!",
            emoji = "⚡",
            icon = Icons.Default.EmojiEvents,
            unlocked = false,
            category = AchievementCategory.SKILL
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





