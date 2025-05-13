package com.example.childsafe.data.repository

import com.example.childsafe.data.model.DailyStepProgress
import com.example.childsafe.data.model.LeaderboardEntry
import com.example.childsafe.domain.repository.HealthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FakeHealthRepository @Inject constructor() : HealthRepository {
    init {
        Timber.d("FakeHealthRepository initialized")
    }

    private val _dailySteps = MutableStateFlow(0)
    override val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    private val _weeklyProgress = MutableStateFlow<List<DailyStepProgress>>(emptyList())
    override val weeklyProgress: StateFlow<List<DailyStepProgress>> = _weeklyProgress.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    override val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    /**
     * Internal method for tests to directly set daily steps
     */
    internal fun setDailySteps(steps: Int) {
        Timber.d("Setting daily steps directly: $steps")
        _dailySteps.value = steps
    }

    /**
     * Internal method for tests to directly set weekly progress
     */
    internal fun setWeeklyProgress(progress: List<DailyStepProgress>) {
        Timber.d("Setting weekly progress directly: ${progress.size} entries")
        _weeklyProgress.value = progress
    }

    /**
     * Internal method for tests to directly set leaderboard entries
     */
    internal fun setLeaderboard(entries: List<LeaderboardEntry>) {
        Timber.d("Setting leaderboard directly: ${entries.size} entries")
        _leaderboard.value = entries
    }

    override suspend fun updateSteps(steps: Int, duration: Long) {
        Timber.d("Updating steps: $steps (duration: ${duration}ms)")
        _dailySteps.value = steps
    }

    override suspend fun fetchDailySteps(): Int {
        return _dailySteps.value.also { steps ->
            Timber.d("Fetched daily steps: $steps")
        }
    }

    override suspend fun updateWeeklyProgress() {
        Timber.d("Generating weekly progress with current steps: ${_dailySteps.value}")
        val progress = com.example.childsafe.test.SampleStepTrackingData
            .generateWeeklyProgress(_dailySteps.value)
        _weeklyProgress.value = progress
        Timber.d("Weekly progress updated: ${progress.size} entries")
    }

    override suspend fun updateLeaderboard() {
        Timber.d("Generating leaderboard data")
        val leaderboard = com.example.childsafe.test.SampleStepTrackingData
            .generateLeaderboard()
        _leaderboard.value = leaderboard
        Timber.d("Leaderboard updated: ${leaderboard.size} entries")
    }
}
