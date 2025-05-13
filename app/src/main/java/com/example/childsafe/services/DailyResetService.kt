package com.example.childsafe.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.childsafe.domain.repository.HealthRepository
import com.example.childsafe.domain.repository.strategy.HealthRepositoryStrategy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class DailyResetService : Service() {

    @Inject
    lateinit var healthRepositoryStrategy: HealthRepositoryStrategy
    
    private val healthRepository: HealthRepository by lazy {
        healthRepositoryStrategy.provideHealthRepository().also {
            Timber.d("Health repository initialized for DailyResetService: ${it.javaClass.simpleName}")
        }
    }

    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, DailyResetService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DailyResetService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("DailyResetService created")
        startCountdown()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("DailyResetService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCountdown() {
        countdownJob?.cancel() // Cancel any existing countdown
        countdownJob = serviceScope.launch {
            while (isActive) {
                val now = LocalDateTime.now(ZoneId.systemDefault())
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val millisecondsUntilMidnight = ChronoUnit.MILLIS.between(now, nextMidnight)

                Timber.d("Time until midnight: ${millisecondsUntilMidnight}ms")

                try {
                    delay(millisecondsUntilMidnight)
                    resetDailySteps()
                } catch (e: CancellationException) {
                    Timber.d("Countdown cancelled")
                    break
                } catch (e: Exception) {
                    Timber.e(e, "Error in countdown loop")
                    delay(60000) // Wait a minute before retrying
                }
            }
        }
    }

    private suspend fun resetDailySteps() {
        try {
            // Get current steps before resetting
            val currentSteps = healthRepository.dailySteps.value

            // Update weekly progress before resetting steps
            healthRepository.updateWeeklyProgress()

            // Reset steps to 0
            healthRepository.updateSteps(0, System.currentTimeMillis())

            Timber.d("Daily steps reset successful. Previous steps: $currentSteps")

            // Update leaderboard after reset
            healthRepository.updateLeaderboard()
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset daily steps")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("DailyResetService destroyed")
        countdownJob?.cancel()
        serviceJob?.cancel()
    }
}
