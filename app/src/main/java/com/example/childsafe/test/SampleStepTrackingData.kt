package com.example.childsafe.test

import com.example.childsafe.data.model.DailyStepProgress
import com.example.childsafe.data.model.LeaderboardEntry
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Sample data provider for step tracking features in debug mode.
 * 
 * This class provides consistent sample data for:
 * - Daily progress with realistic step counts
 * - Weekly progress with historical data
 * - Leaderboard entries with sample users
 */
object SampleStepTrackingData {
    private val mockUsers = listOf(
        Pair("user1", "Emma"),
        Pair("user2", "James"),
        Pair("user3", "Olivia"),
        Pair("user4", "William"),
        Pair("user5", "Sophia"),
        Pair("user6", "Lucas"),
        Pair("user7", "Mia"),
        Pair("user8", "Henry"),
        Pair("user9", "Ava"),
        Pair("user10", "Noah")
    )

    private val mockPhotos = listOf(
        "https://example.com/avatars/avatar1.jpg",
        "https://example.com/avatars/avatar2.jpg",
        null,  // Some users don't have photos
        "https://example.com/avatars/avatar4.jpg"
    )

    private val stepGoals = listOf(8000, 10000, 12000)

    /**
     * Generates a daily progress entry with realistic step data.
     * For debug mode, it ensures steps are always between 5000-8000
     * to provide meaningful test data.
     */
    fun generateDailyProgress(): DailyStepProgress {
        val goal = stepGoals.random()
        // Generate steps between 5000-8000 for consistent testing
        val steps = (5000..8000).random()
        return DailyStepProgress(
            dayOfWeek = LocalDateTime.now().dayOfWeek.value % 7,
            steps = steps,
            goal = goal,
            duration = (steps / 100L) * 60L * 1000L // About 1 minute per 100 steps
        )
    }

    /**
     * Generates a week's worth of progress, with decreasing step counts
     * towards earlier days and future days empty.
     *
     * @param currentSteps Today's step count
     * @return List of daily progress entries
     */
    fun generateWeeklyProgress(currentSteps: Int = 0): List<DailyStepProgress> {
        val today = LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value % 7
        val goal = stepGoals.random()
        
        return List(7) { dayIndex ->
            when {
                dayIndex < currentDayOfWeek -> {
                    // Past days - random completion between 60-120%
                    val completion = Random.nextDouble(0.6, 1.2)
                    val steps = (goal * completion).toInt()
                    DailyStepProgress(
                        dayOfWeek = dayIndex,
                        steps = steps,
                        goal = goal,
                        duration = (steps / 100L) * 60L * 1000L
                    )
                }
                dayIndex == currentDayOfWeek -> {
                    // Current day - use actual steps
                    DailyStepProgress(
                        dayOfWeek = dayIndex,
                        steps = currentSteps,
                        goal = goal,
                        duration = (currentSteps / 100L) * 60L * 1000L
                    )
                }
                else -> {
                    // Future days - empty
                    DailyStepProgress(
                        dayOfWeek = dayIndex,
                        steps = 0,
                        goal = goal,
                        duration = 0L
                    )
                }
            }
        }
    }

    /**
     * Generates a randomized but consistently ordered leaderboard of users,
     * including the current user with their actual step count.
     *
     * @param currentSteps The current user's step count
     * @return List of leaderboard entries, sorted by step count
     */
    fun generateLeaderboard(currentSteps: Int = 0): List<LeaderboardEntry> {
        // Base steps between 8000-12000
        val baseSteps = (8000..12000).random()
        
        // Generate other users' entries
        val otherUsers = mockUsers.mapIndexed { index, (userId, name) ->
            // Each subsequent user has 90% of previous user's steps on average
            // with some random variation (+/- 30%)
            val position = index.toDouble()
            val decay = Math.pow(0.9, position)
            val variation = Random.nextDouble(0.7, 1.3)
            val steps = (baseSteps * decay * variation).toInt()
            
            LeaderboardEntry(
                userId = userId,
                username = name,
                steps = steps,
                photoUrl = mockPhotos.getOrNull(index % mockPhotos.size)
            )
        }

        // Add current user with actual steps
        val currentUser = LeaderboardEntry(
            userId = "current_user",
            username = "Me",  // Display as "Me" in the leaderboard
            steps = currentSteps,
            photoUrl = mockPhotos.firstOrNull()  // Give current user an avatar
        )

        // Combine and sort all entries by steps
        return (otherUsers + currentUser).sortedByDescending { it.steps }
    }
}
