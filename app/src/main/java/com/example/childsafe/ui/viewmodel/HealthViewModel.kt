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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    
    // Track time until midnight
    private val _timeUntilMidnight = MutableStateFlow(0L)
    val timeUntilMidnight: StateFlow<Long> = _timeUntilMidnight.asStateFlow()
    
    // Track debug mode
    val isDebugMode: Boolean = buildConfig.isDebug

    private var isTracking = false
    private var updateJob: Job? = null

    init {
        viewModelScope.launch {
            loadInitialData()
            startMidnightCountdown()
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

    private fun startMidnightCountdown() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val duration = Duration.between(now, midnight)
                
                _timeUntilMidnight.value = duration.toMillis()
                _uiState.value = _uiState.value.copy(
                    duration = duration.toMillis()
                )
                
                delay(1000) // Update every second
            }
        }
    }

    fun startTracking() {
        if (!isTracking) {
            isTracking = true
            _uiState.value = _uiState.value.copy(
                isTracking = true
            )
        }
    }

    fun stopTracking() {
        if (isTracking) {
            isTracking = false
            _uiState.value = _uiState.value.copy(
                isTracking = false
            )
        }
    }

    fun updateSteps(newSteps: Int) {
        viewModelScope.launch {
            healthRepository.updateSteps(newSteps, _uiState.value.duration)
            _uiState.value = _uiState.value.copy(
                currentSteps = newSteps
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

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }
}

data class HealthUiState(
    val currentSteps: Int = 0,
    val weeklyProgress: List<DailyStepProgress> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isTracking: Boolean = false,
    val duration: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null
)
