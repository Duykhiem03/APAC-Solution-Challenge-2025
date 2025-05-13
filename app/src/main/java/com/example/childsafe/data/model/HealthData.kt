package com.example.childsafe.data.model

import java.time.LocalDate
import java.time.LocalDateTime

data class UserStepData(
    val userId: String,
    val date: LocalDate,
    val steps: Int,
    val duration: Long, // Duration in milliseconds
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

data class DailyStepProgress(
    val dayOfWeek: Int, // 0 = Sunday, 1-6 = Monday-Saturday
    val steps: Int,
    val goal: Int = 10000,
    val duration: Long // Duration in milliseconds
)

data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val steps: Int,
    val multiplier: Double = 1.0,
    val photoUrl: String? = null
)
