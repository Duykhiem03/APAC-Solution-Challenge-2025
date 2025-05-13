package com.example.childsafe.services

import android.content.Context
import androidx.work.*
import androidx.hilt.work.HiltWorker
import com.example.childsafe.concurrency.ConsistencyMonitorService
import com.example.childsafe.data.local.OfflineMessageDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker for retrying failed messages
 * This worker will attempt to send any messages that failed to send
 * It uses exponential backoff to retry at increasing intervals
 */
@HiltWorker
class MessageRetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageSyncService: MessageSyncService,
    private val consistencyMonitorService: ConsistencyMonitorService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("MessageRetryWorker started")
            
            withContext(Dispatchers.IO) {
                // Retry the failed messages
                messageSyncService.retryFailedMessages()
                
                // Log the retry attempt
                consistencyMonitorService.logBackgroundOperation(
                    "MessageRetryWorker",
                    "Attempted to retry failed messages"
                )
            }
            
            Timber.d("MessageRetryWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in MessageRetryWorker")
            
            // Only retry if this is a retryable error
            if (isRetryable(e)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    private fun isRetryable(e: Exception): Boolean {
        // Network errors are retryable, but auth errors are not
        return e !is SecurityException
    }
    
    companion object {
        private const val UNIQUE_WORK_NAME = "message_retry_worker"
        
        /**
         * Schedule this worker to run periodically
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val workRequest = PeriodicWorkRequestBuilder<MessageRetryWorker>(
                15, TimeUnit.MINUTES, // Run every 15 minutes
                10, TimeUnit.MINUTES  // Flex period of 10 minutes
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if there's already one scheduled
                workRequest
            )
            
            Timber.d("MessageRetryWorker scheduled to run every 15 minutes")
        }
        
        /**
         * Request an immediate one-time execution
         */
        fun runNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val workRequest = OneTimeWorkRequestBuilder<MessageRetryWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Timber.d("MessageRetryWorker one-time execution requested")
        }
    }
}
