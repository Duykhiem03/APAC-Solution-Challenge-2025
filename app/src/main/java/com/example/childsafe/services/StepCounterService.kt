package com.example.childsafe.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.childsafe.MainActivity
import com.example.childsafe.R
import com.example.childsafe.domain.repository.HealthRepository
import com.example.childsafe.domain.repository.strategy.HealthRepositoryStrategy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    @Inject
    lateinit var healthRepositoryStrategy: HealthRepositoryStrategy
    
    private val healthRepository: HealthRepository by lazy {
        healthRepositoryStrategy.provideHealthRepository().also {
            Timber.d("Health repository initialized: ${it.javaClass.simpleName}")
        }
    }

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var currentSteps = 0
    private var initialSteps = -1
    private var serviceJob: Job? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "StepCounterChannel"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("StepCounterService created")
        setupStepSensor()
        createNotificationChannel()
        // Start daily reset service
        DailyResetService.start(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("StepCounterService started")
        startForeground(NOTIFICATION_ID, createNotification())
        registerStepSensor()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupStepSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepSensor == null) {
            Timber.w("No step sensor available on this device")
        } else {
            Timber.d("Step sensor type: ${stepSensor?.stringType}, vendor: ${stepSensor?.vendor}")
        }
    }

    private fun registerStepSensor() {
        stepSensor?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Timber.d("Step sensor registered successfully")
        } ?: run {
            Timber.e("Failed to register step sensor - sensor is null")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (initialSteps == -1) {
                initialSteps = it.values[0].toInt()
                Timber.d("Initial steps set to: $initialSteps")
            }

            val steps = it.values[0].toInt() - initialSteps
            if (steps != currentSteps) {
                val oldSteps = currentSteps
                currentSteps = steps
                Timber.d("Steps changed: $oldSteps -> $currentSteps (raw value: ${it.values[0]})")
                updateSteps(currentSteps)
            }
        } ?: run {
            Timber.w("Received null sensor event")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    private fun updateSteps(steps: Int) {
        serviceScope.launch {
            try {
                Timber.d("Updating steps in repository: $steps")
                val startTime = System.currentTimeMillis()
                healthRepository.updateSteps(steps, startTime)
                updateNotification(steps)
                val duration = System.currentTimeMillis() - startTime
                Timber.d("Steps updated successfully in ${duration}ms")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update steps in repository: $steps")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your steps in the background"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(steps: Int = 0) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.step_counter_active))
        .setContentText(getString(R.string.steps_counted, steps))
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(createPendingIntent())
        .build()

    private fun updateNotification(steps: Int) {
        val notification = createNotification(steps)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("StepCounterService destroyed")
        sensorManager?.unregisterListener(this)
        serviceJob?.cancel()
        // Stop daily reset service
        DailyResetService.stop(this)
        Timber.d("StepCounterService destroyed")
    }
}
