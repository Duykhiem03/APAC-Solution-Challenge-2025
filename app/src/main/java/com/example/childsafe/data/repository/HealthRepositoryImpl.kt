package com.example.childsafe.data.repository

import com.example.childsafe.data.model.DailyStepProgress
import com.example.childsafe.data.model.LeaderboardEntry
import com.example.childsafe.domain.repository.HealthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : HealthRepository {

    private val stepsCollection = firestore.collection("steps")

    private val _dailySteps = MutableStateFlow(0)
    override val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    private val _weeklyProgress = MutableStateFlow<List<DailyStepProgress>>(emptyList())
    override val weeklyProgress: StateFlow<List<DailyStepProgress>> = _weeklyProgress.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    override val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    init {
        // Initialize with current data using a coroutine scope
        kotlinx.coroutines.MainScope().launch {
            auth.currentUser?.let { user ->
                fetchAndUpdateSteps(user.uid)
            }
        }
    }

    private suspend fun fetchAndUpdateSteps(userId: String) {
        try {
            val today = LocalDate.now()
            val docId = "${userId}_${today}"
            
            val doc = stepsCollection.document(docId).get().await()
            if (doc.exists()) {
                val data = doc.data
                _dailySteps.value = (data?.get("steps") as? Number)?.toInt() ?: 0
                Timber.d("Fetched daily steps: ${_dailySteps.value}")
            } else {
                _dailySteps.value = 0
                Timber.d("No steps recorded today")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching steps")
        }
    }

    override suspend fun updateSteps(steps: Int, duration: Long) {
        auth.currentUser?.let { user ->
            try {
                val today = LocalDate.now()
                val docId = "${user.uid}_${today}"
                
                stepsCollection.document(docId).set(
                    hashMapOf(
                        "userId" to user.uid,
                        "date" to Date(),
                        "steps" to steps,
                        "duration" to duration,
                        "lastUpdated" to com.google.firebase.Timestamp.now(),
                        "isPublic" to true,
                        "goal" to 10000
                    )
                ).await()
                
                _dailySteps.value = steps
                Timber.d("Updated steps: $steps")
                
                // Update weekly progress and leaderboard after updating steps
                updateWeeklyProgress()
                updateLeaderboard()
                
            } catch (e: Exception) {
                Timber.e(e, "Error updating steps")
            }
        }
    }

    override suspend fun fetchDailySteps(): Int {
        auth.currentUser?.let { user ->
            fetchAndUpdateSteps(user.uid)
        }
        return _dailySteps.value
    }

    override suspend fun updateWeeklyProgress() {
        auth.currentUser?.let { user ->
            try {
                val weekAgo = LocalDate.now().minusDays(7)
                val snapshot = stepsCollection
                    .whereEqualTo("userId", user.uid)
                    .whereGreaterThanOrEqualTo("date", Date.from(weekAgo.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)))
                    .get()
                    .await()

                val progress = snapshot.documents.map { doc ->
                    val data = doc.data ?: return@map null
                    DailyStepProgress(
                        dayOfWeek = (data["date"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.dayOfWeek?.value ?: 0,
                        steps = (data["steps"] as? Number)?.toInt() ?: 0,
                        goal = (data["goal"] as? Number)?.toInt() ?: 10000,
                        duration = (data["duration"] as? Number)?.toLong() ?: 0L
                    )
                }.filterNotNull()

                _weeklyProgress.value = progress
                Timber.d("Weekly progress updated: ${progress.size} entries")
            } catch (e: Exception) {
                Timber.e(e, "Error updating weekly progress")
            }
        }
    }

    override suspend fun updateLeaderboard() {
        try {
            val today = LocalDate.now()
            val snapshot = stepsCollection
                .whereEqualTo("date", Date.from(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)))
                .whereEqualTo("isPublic", true)
                .orderBy("steps", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val currentUserId = auth.currentUser?.uid
            val entries = mutableListOf<LeaderboardEntry>()

            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val userId = data["userId"] as? String ?: continue
                val steps = (data["steps"] as? Number)?.toInt() ?: continue

                // Fetch user profile for name and photo
                val userDoc = firestore.collection("users").document(userId).get().await()
                val userData = userDoc.data

                entries.add(LeaderboardEntry(
                    userId = userId,
                    username = if (userId == currentUserId) "Me" else (userData?.get("displayName") as? String ?: "Anonymous"),
                    steps = steps,
                    photoUrl = userData?.get("photoUrl") as? String
                ))
            }

            _leaderboard.value = entries.sortedByDescending { it.steps }
            Timber.d("Leaderboard updated: ${entries.size} entries")
        } catch (e: Exception) {
            Timber.e(e, "Error updating leaderboard")
        }
    }
}
