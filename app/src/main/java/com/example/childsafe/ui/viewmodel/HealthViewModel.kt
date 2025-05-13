package com.example.childsafe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.data.model.DailyStepProgress
import com.example.childsafe.data.model.LeaderboardEntry
import com.example.childsafe.domain.repository.HealthRepository
import com.example.childsafe.domain.repository.strategy.HealthRepositoryStrategy
import com.example.childsafe.utils.buildconfig.BuildConfigStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class HealthViewModel @Inject constructor(
    healthRepositoryStrategy: HealthRepositoryStrategy,
    buildConfig: BuildConfigStrategy
) : ViewModel() {

    internal val healthRepository: HealthRepository = healthRepositoryStrategy.provideHealthRepository()
    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    // Track debug mode
    val isDebugMode: Boolean = buildConfig.isDebug

    private var startTime: LocalDateTime? = null
    private var isTracking = false

    init {
        viewModelScope.launch {
            loadInitialData()
        }
    }

    private suspend fun loadInitialData() {
        try {
            val steps = healthRepository.fetchDailySteps()
            healthRepository.updateWeeklyProgress()
            healthRepository.updateLeaderboard()
            
            _uiState.value = _uiState.value.copy(
                currentSteps = steps,
                isLoading = false
            )
        } catch (e: Exception) {
            Timber.e(e, "Error loading initial health data")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load health data"
            )
        }
    }

    fun startTracking() {
        if (!isTracking) {
            startTime = LocalDateTime.now()
            isTracking = true
            _uiState.value = _uiState.value.copy(
                isTracking = true,
                startTime = startTime
            )
        }
    }

    fun stopTracking() {
        if (isTracking) {
            isTracking = false
            startTime = null
            _uiState.value = _uiState.value.copy(
                isTracking = false,
                startTime = null
            )
        }
    }

    fun updateSteps(newSteps: Int) {
        viewModelScope.launch {
            val duration = startTime?.let {
                Duration.between(it, LocalDateTime.now()).toMillis()
            } ?: 0L

            healthRepository.updateSteps(newSteps, duration)
            _uiState.value = _uiState.value.copy(
                currentSteps = newSteps,
                duration = duration
            )
        }
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            try {
                healthRepository.updateLeaderboard()
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing leaderboard")
            }
        }
    }
}

data class HealthUiState(
    val currentSteps: Int = 0,
    val weeklyProgress: List<DailyStepProgress> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isTracking: Boolean = false,
    val startTime: LocalDateTime? = null,
    val duration: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null
)
