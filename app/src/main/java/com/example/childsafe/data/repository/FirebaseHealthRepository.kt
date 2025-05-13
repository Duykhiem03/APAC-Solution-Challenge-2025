package com.example.childsafe.data.repository

import com.example.childsafe.BuildConfig
import com.example.childsafe.data.model.DailyStepProgress
import com.example.childsafe.data.model.LeaderboardEntry
import com.example.childsafe.domain.repository.HealthRepository
import com.example.childsafe.data.repository.debug.DebugStepTrackingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FirebaseHealthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : HealthRepository {
    
    init {
        Timber.d("FirebaseHealthRepository initialized")
    }

    private val _dailySteps = MutableStateFlow(0)
    override val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    private val _weeklyProgress = MutableStateFlow<List<DailyStepProgress>>(emptyList())
    override val weeklyProgress: StateFlow<List<DailyStepProgress>> = _weeklyProgress.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    override val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    override suspend fun updateSteps(steps: Int, duration: Long) {
        if (BuildConfig.DEBUG) {
            Timber.d("Debug mode: Steps update skipped. Steps: $steps, Duration: ${duration}ms")
            _dailySteps.value = steps
            return
        }

        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Timber.w("Cannot update steps: No authenticated user")
                return
            }

            val today = LocalDate.now()
            val date = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())

            Timber.d("Updating steps for user $userId: $steps steps on $today")
            firestore.collection("steps")
                .document(userId)
                .collection("daily")
                .document(today.toString())
                .set(mapOf(
                    "steps" to steps,
                    "duration" to duration,
                    "date" to date,
                    "lastUpdated" to System.currentTimeMillis()
                )).await()

            _dailySteps.value = steps
            Timber.d("Steps updated successfully")

        } catch (e: Exception) {
            Timber.e(e, "Failed to update steps in Firestore")
            throw e
        }
    }

    override suspend fun fetchDailySteps(): Int {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Timber.w("Cannot fetch steps: No authenticated user")
                return _dailySteps.value
            }

            val today = LocalDate.now()
            Timber.d("Fetching daily steps for user $userId on $today")

            val snapshot = firestore.collection("steps")
                .document(userId)
                .collection("daily")
                .document(today.toString())
                .get()
                .await()

            val steps = snapshot.getLong("steps")?.toInt() ?: 0
            _dailySteps.value = steps
            Timber.d("Fetched daily steps: $steps")
            steps

        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch daily steps from Firestore")
            _dailySteps.value
        }
    }

    override suspend fun updateWeeklyProgress() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Timber.w("Cannot update weekly progress: No authenticated user")
                return
            }

            val today = LocalDate.now()
            Timber.d("Updating weekly progress for user $userId, week of $today")

            val weeklyData = mutableListOf<DailyStepProgress>()
            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                val snapshot = firestore.collection("steps")
                    .document(userId)
                    .collection("daily")
                    .document(date.toString())
                    .get()
                    .await()

                val steps = snapshot.getLong("steps")?.toInt() ?: 0
                val duration = snapshot.getLong("duration") ?: 0L
                val dayOfWeek = date.dayOfWeek.value % 7 // Convert to 0-6 where 0 is Sunday
                weeklyData.add(DailyStepProgress(dayOfWeek, steps, duration = duration))
            }

            _weeklyProgress.value = weeklyData
            Timber.d("Weekly progress updated: ${weeklyData.size} entries")

        } catch (e: Exception) {
            Timber.e(e, "Failed to update weekly progress")
        }
    }

    override suspend fun updateLeaderboard() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Timber.w("Cannot update leaderboard: No authenticated user")
                return
            }

            Timber.d("Updating leaderboard data")
            val today = LocalDate.now()
            val leaderboardEntries = mutableListOf<LeaderboardEntry>()

            // Get the user's friends list
            val friendshipsDoc = firestore.collection("friendships")
                .document(userId)
                .get()
                .await()

            if (!friendshipsDoc.exists()) {
                Timber.d("No friends found, returning empty leaderboard")
                _leaderboard.value = emptyList()
                return
            }

            @Suppress("UNCHECKED_CAST")
            val friendIds = friendshipsDoc.get("friends") as? List<String> ?: emptyList()
            if (friendIds.isEmpty()) {
                Timber.d("Friends list is empty, returning empty leaderboard")
                _leaderboard.value = emptyList()
                return
            }

            // Add current user to the list so they appear in leaderboard too
            val allUserIds = friendIds + userId

            // Get steps data for friends and current user
            val snapshots = firestore.collection("steps")
                .whereIn("userId", allUserIds)  // This assumes you have a userId field in steps documents
                .get()
                .await()

            for (userDoc in snapshots.documents) {
                val todaySteps = userDoc.reference
                    .collection("daily")
                    .document(today.toString())
                    .get()
                    .await()

                val steps = todaySteps.getLong("steps")?.toInt() ?: 0
                
                // Get user profile data
                val userProfile = userDoc.reference.collection("profile").document("info").get().await()
                val username = userProfile.getString("username") ?: userDoc.id
                val photoUrl = userProfile.getString("photoUrl")
                val multiplier = userProfile.getDouble("stepMultiplier") ?: 1.0
                
                leaderboardEntries.add(LeaderboardEntry(userDoc.id, username, steps, multiplier, photoUrl))
            }

            _leaderboard.value = leaderboardEntries.sortedByDescending { it.steps * it.multiplier }
            Timber.d("Leaderboard updated: ${leaderboardEntries.size} entries")

        } catch (e: Exception) {
            Timber.e(e, "Failed to update leaderboard")
        }
    }
}
