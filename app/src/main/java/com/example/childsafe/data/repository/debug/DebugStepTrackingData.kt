package com.example.childsafe.data.repository.debug

import com.example.childsafe.data.model.DailyStepProgress
import com.example.childsafe.data.model.LeaderboardEntry
import java.time.LocalDate
import java.time.LocalDateTime

object DebugStepTrackingData {
    private val userNames = listOf(
        "Minh Anh", "Thanh", "Hương", "Phương", "Nam",
        "Linh", "Tuấn", "Mai", "Hải", "Trang"
    )
    
    private val dailyGoals = listOf(8000, 10000, 12000) // Different step goals
    private val userPhotos = listOf(
        "https://example.com/avatar1.jpg",
        "https://example.com/avatar2.jpg",
        null, // Some users don't have photos
        "https://example.com/avatar4.jpg",
        "https://example.com/avatar5.jpg"
    )

    fun generateDailySteps(): Int {
        return (3000..15000).random()
    }

    fun generateWeeklyProgress(): List<DailyStepProgress> {
        val today = LocalDate.now()
        val currentDayOfWeek = (today.dayOfWeek.value % 7) // Convert to 0-based (0 = Sunday)
        val goal = dailyGoals.random()
        
        return List(7) { index ->
            DailyStepProgress(
                dayOfWeek = index,
                steps = when {
                    index < currentDayOfWeek -> (5000..12000).random() // Past days have data
                    index == currentDayOfWeek -> generateDailySteps() // Current day
                    else -> 0 // Future days are empty
                },
                goal = goal,
                duration = when {
                    index < currentDayOfWeek -> (30..120).random() * 60L * 1000L // Past days in milliseconds
                    index == currentDayOfWeek -> {
                        val now = LocalDateTime.now()
                        val startOfDay = now.toLocalDate().atStartOfDay()
                        java.time.Duration.between(startOfDay, now).toMillis()
                    }
                    else -> 0L // Future days
                }
            )
        }
    }

    fun generateLeaderboard(): List<LeaderboardEntry> {
        val entries = mutableListOf<LeaderboardEntry>()
        val usedSteps = mutableSetOf<Int>()
        
        repeat(10) { index ->
            var steps: Int
            do {
                steps = (5000..15000).random()
            } while (steps in usedSteps)
            usedSteps.add(steps)
            
            entries.add(
                LeaderboardEntry(
                    userId = "user$index",
                    username = userNames.getOrElse(index) { "User ${index + 1}" },
                    steps = steps,
                    photoUrl = userPhotos.getOrNull(index)
                )
            )
        }
        
        return entries.sortedByDescending { it.steps }
    }
}
