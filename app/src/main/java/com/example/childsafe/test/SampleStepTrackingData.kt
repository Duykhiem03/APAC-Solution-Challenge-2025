package com.example.childsafe.test

import com.example.childsafe.data.model.DailyStepProgress
import com.example.childsafe.data.model.LeaderboardEntry
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Sample data provider for step tracking features in debug mode
 */
object SampleStepTrackingData {
    val mockUsers = listOf(
        Pair("user1", "Mẹ"),
        Pair("user2", "Bố"),
        Pair("user3", "Chị Hải"),
        Pair("user4", "Anh Minh"),
        Pair("user5", "Bạn Trang"),
        Pair("user6", "Cô Mai"),
        Pair("user7", "Em Linh"),
        Pair("user8", "Anh Tuấn"),
        Pair("user9", "Em Phương"),
        Pair("user10", "Bạn Nam")
    )

    val mockPhotos = listOf(
        "https://example.com/avatar1.jpg",
        "https://example.com/avatar2.jpg",
        null,  // Some users don't have photos
        "https://example.com/avatar4.jpg"
    )

    val stepGoals = listOf(8000, 10000, 12000)
    
    fun generateDailyProgress(): DailyStepProgress {
        val goal = stepGoals.random()
        val steps = (goal * Random.nextDouble(0.3, 1.2)).toInt()
        return DailyStepProgress(
            dayOfWeek = LocalDateTime.now().dayOfWeek.value % 7,
            steps = steps,
            goal = goal,
            duration = (steps / 100L) * 60L * 1000L // Roughly 1 minute per 100 steps
        )
    }

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

    fun generateLeaderboard(): List<LeaderboardEntry> {
        // Generate steps with some variation but keep relative positions
        val baseSteps = (5000..12000).random()
        
        return mockUsers.mapIndexed { index, (userId, name) ->
            val variation = Random.nextDouble(0.7, 1.3)
            val steps = (baseSteps * (1.2 - (index * 0.1)) * variation).toInt()
            
            LeaderboardEntry(
                userId = userId,
                username = name,
                steps = steps,
                multiplier = 1.0,
                photoUrl = mockPhotos.getOrNull(index % mockPhotos.size)
            )
        }.sortedByDescending { it.steps }
    }
}
