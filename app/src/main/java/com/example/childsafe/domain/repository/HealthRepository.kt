package com.example.childsafe.domain.repository

import com.example.childsafe.data.model.DailyStepProgress
import com.example.childsafe.data.model.LeaderboardEntry
import kotlinx.coroutines.flow.StateFlow

interface HealthRepository {
    val dailySteps: StateFlow<Int>
    val weeklyProgress: StateFlow<List<DailyStepProgress>>
    val leaderboard: StateFlow<List<LeaderboardEntry>>

    suspend fun updateSteps(steps: Int, duration: Long)
    suspend fun fetchDailySteps(): Int
    suspend fun updateWeeklyProgress()
    suspend fun updateLeaderboard()
}
