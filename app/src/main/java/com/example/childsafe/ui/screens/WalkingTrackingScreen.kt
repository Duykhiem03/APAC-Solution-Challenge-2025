package com.example.childsafe.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.childsafe.ui.theme.ChildSafeTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.childsafe.CharacterMonitorWithRoad
import com.example.childsafe.R
import com.example.childsafe.data.model.LeaderboardEntry
import com.example.childsafe.data.repository.FakeHealthRepository
import com.example.childsafe.services.StepCounterService
import com.example.childsafe.ui.components.StepCountingPermissionHandler
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.viewmodel.HealthViewModel
import timber.log.Timber
import java.time.Duration
import java.time.LocalDate


@Composable
fun WalkingTrackingScreen(
    onBackClick: () -> Unit,
    viewModel: HealthViewModel = hiltViewModel()
) {
    // Collect all StateFlow data from the ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val currentSteps by viewModel.healthRepository.dailySteps.collectAsState()
    val weeklyProgress by viewModel.healthRepository.weeklyProgress.collectAsState()
    val leaderboardEntries by viewModel.healthRepository.leaderboard.collectAsState()
    
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(false) }

    // Log for debugging
    LaunchedEffect(leaderboardEntries) {
        Timber.d("Leaderboard updated: ${leaderboardEntries.size} entries")
        leaderboardEntries.forEachIndexed { index, entry ->
            Timber.d("Entry #${index + 1}: User ${entry.username}, Steps: ${entry.steps}")
        }
    }

    // Skip permission check in preview or debug mode
    val isPreview = LocalInspectionMode.current
    val isDebugMode = viewModel.isDebugMode

    if (!isPreview && !isDebugMode) {
        StepCountingPermissionHandler { granted ->
            permissionsGranted = granted
            if (granted) {
                // Start step counting service only when permissions are granted in production mode
                val serviceIntent = Intent(context, StepCounterService::class.java)
                context.startForegroundService(serviceIntent)
                viewModel.startTracking()
            }
        }
    } else {
        // In preview or debug mode, simulate permissions granted
        SideEffect {
            permissionsGranted = true
            viewModel.startTracking()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Stop step counting service when screen is disposed (only in production)
            viewModel.stopTracking()
            if (!isDebugMode) {
                val serviceIntent = Intent(context, StepCounterService::class.java)
                context.stopService(serviceIntent)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Top Bar with Debug Mode Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.walking_tracking),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Empty box for alignment
            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step Counter Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step count in yellow bubble
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFFD700),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 36.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.currentSteps.toString(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.footstep),
                                contentDescription = "Step Icon",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Display countdown to midnight
                        val timeUntilMidnight by viewModel.timeUntilMidnight.collectAsState()
                        Text(
                            text = "Reset in: ${formatDuration(timeUntilMidnight)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Đếm ngược ${formatDuration(uiState.duration)}",
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }

        // Weekly progress milestones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weeklyProgress.take(7).forEach { progress ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Day milestone badge
                    val today = LocalDate.now()
                    val isCurrentDay = progress.dayOfWeek == (today.dayOfWeek.value % 7)
                    val isPastDay = progress.dayOfWeek < (today.dayOfWeek.value % 7)

                    CircularProgressDay(
                        dayText = getDayAbbreviation(progress.dayOfWeek),
                        progress = (progress.steps.toFloat() / progress.goal).coerceIn(
                            0f,
                            1f
                        ),
                        isCurrentDay = isCurrentDay,
                        isPastDay = isPastDay,
                        modifier = Modifier.size(36.dp)
                    )

                    // Step count
//                    Text(
//                        text = "${progress.steps}",
//                        fontSize = 12.sp,
//                        color = Color.Black,
//                        fontWeight = FontWeight.Medium
//                    )
//
//                    // Progress indicator with milestones
//                    val progressPercent =
//                        (progress.steps.toFloat() / progress.goal).coerceIn(0f, 1f)
//                    Box(
//                        modifier = Modifier
//                            .width(4.dp)
//                            .height(24.dp)
//                            .background(Color(0xFFEEEEEE), RoundedCornerShape(2.dp))
//                    ) {
//                        if (progress.steps > 0) {
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .fillMaxHeight(progressPercent)
//                                    .align(Alignment.BottomCenter)
//                                    .background(
//                                        when {
//                                            progressPercent >= 1f -> Color(0xFF4CAF50) // Goal achieved
//                                            progressPercent >= 0.7f -> Color(0xFFFFD700) // Good progress
//                                            else -> Color(0xFFFFA000) // Some progress
//                                        },
//                                        RoundedCornerShape(2.dp)
//                                    )
//                            )
//
//                            // Milestone markers
//                            if (progressPercent >= 0.25f) {
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(1.dp)
//                                        .align(Alignment.BottomCenter)
//                                        .offset(y = -(24.dp * 0.25f))
//                                        .background(Color.White.copy(alpha = 0.5f))
//                                )
//                            }
//                            if (progressPercent >= 0.5f) {
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(1.dp)
//                                        .align(Alignment.BottomCenter)
//                                        .offset(y = -(24.dp * 0.5f))
//                                        .background(Color.White.copy(alpha = 0.5f))
//                                )
//                            }
//                            if (progressPercent >= 0.75f) {
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(1.dp)
//                                        .align(Alignment.BottomCenter)
//                                        .offset(y = -(24.dp * 0.75f))
//                                        .background(Color.White.copy(alpha = 0.5f))
//                                )
//                            }
//                        }
//                    }
                }
            }
        }

        // Character monitor with road
        CharacterMonitorWithRoad()

        Spacer(modifier = Modifier.height(24.dp))

//        // Weekly Progress
//        Text(
//            text = stringResource(R.string.weekly_progress),
//            style = MaterialTheme.typography.titleMedium,
//            modifier = Modifier.padding(bottom = 8.dp)
//        )
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            // Add debug logging for weekly progress
//            LaunchedEffect(weeklyProgress) {
//                Timber.d("Weekly progress updated: ${weeklyProgress.size} entries")
//                weeklyProgress.forEachIndexed { index, progress ->
//                    Timber.d("Day ${index + 1}: ${progress.steps} steps (${(progress.steps.toFloat() / progress.goal * 100).toInt()}% of goal)")
//                }
//            }
//
//            weeklyProgress.take(7).forEach { progress ->
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = getDayAbbreviation(progress.dayOfWeek),
//                        fontSize = 12.sp,
//                        color = AppColors.OnSecondary
//                    )
//                    Text(
//                        text = progress.steps.toString(),
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//            }
//        }

        Spacer(modifier = Modifier.height(24.dp))

        // Leaderboard and Group sections
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Bảng xếp hạng button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFD700),
                border = BorderStroke(1.dp, Color.Black)
            ) {
                Text(
                    text = "Bảng xếp hạng",
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }

            // Tạo nhóm button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tạo nhóm",
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Group",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leaderboard list with loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(leaderboardEntries) { entry ->
                    LeaderboardItem(entry = entry, isDebugMode = isDebugMode)
                }
                
                // Show empty state if no entries
                if (leaderboardEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No leaderboard data available yet",
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        if (uiState.error != null) {
            // Show error dialog
            AlertDialog(
                onDismissRequest = { /* Dismiss dialog */ },
                title = { Text(stringResource(R.string.error)) },
                text = { Text(uiState.error!!) },
                confirmButton = {
                    TextButton(onClick = { /* Dismiss dialog */ }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}


@Composable
private fun LeaderboardItem(
    entry: LeaderboardEntry,
    isDebugMode: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Avatar and Name
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDebugMode) AppColors.Primary else AppColors.Secondary)
            ) {
                entry.photoUrl?.let { url ->
                    // AsyncImage would go here
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Username with optional debug indicator
            Column {
                Text(
                    text = entry.username,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                if (isDebugMode) {
                    Text(
                        text = "(Test Data)",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Right side: Steps count
        Text(
            text = "${entry.steps} bước",
            fontSize = 14.sp,
            color = if (isDebugMode) AppColors.Primary else Color.Gray
        )
    }
}


@Composable
private fun CircularProgressDay(
    dayText: String,
    progress: Float,
    isCurrentDay: Boolean,
    isPastDay: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val circleRadius = size.minDimension / 2
            val strokeWidth = 4.dp.toPx()
            val centerOffset = Offset(size.width / 2, size.height / 2)

            // Draw background circle
            drawCircle(
                color = when {
                    isCurrentDay -> Color(0xFFFFD700) // Bright yellow for current day
                    isPastDay -> Color(0xFFFFF4D6)   // Light yellow for past days
                    else -> Color(0xFFEEEEEE)        // Gray for future days
                },
                radius = circleRadius,
                center = centerOffset
            )

            // Draw border circle (lighter than background)
            drawCircle(
                color = when {
                    isCurrentDay -> Color(0xFFE6C200) // Darker yellow for current day
                    isPastDay -> Color(0xFFFFE7B3)   // Darker yellow for past days
                    else -> Color(0xFFDDDDDD)        // Darker gray for future days
                },
                radius = circleRadius,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            if (progress > 0f) {
                // Draw progress arc
                val sweepAngle = progress * 360f
                drawArc(
                    color = when {
                        progress >= 1f -> Color(0xFF4CAF50) // Goal achieved
                        progress >= 0.7f -> Color(0xFFFFD700) // Good progress
                        else -> Color(0xFFFFA000) // Some progress
                    },
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    ),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
            }
        }

        // Day text
        Text(
            text = dayText,
            fontSize = 12.sp,
            fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrentDay) Color(0xFF8B7355) else Color(0xFF666666),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}


private fun formatDuration(millis: Long): String {
    val duration = Duration.ofMillis(millis)
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

private fun getDayAbbreviation(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        0 -> "CN"
        else -> "T${dayOfWeek + 1}"
    }
}
//
//@Preview(
//    name = "Walking Tracking Screen",
//    showSystemUi = true,
//    device = "spec:width=411dp,height=891dp",
//)
//@Composable
//private fun WalkingTrackingScreenPreview() {
//    val fakeRepository = FakeHealthRepository()
//
//    // Setup initial data
//    val mockLeaderboard = listOf(
//        LeaderboardEntry(
//            userId = "1",
//            username = "Chi hai",
//            steps = 1490
//        ),
//        LeaderboardEntry(
//            userId = "2",
//            username = "Ban",
//            steps = 1256
//        ),
//        LeaderboardEntry(
//            userId = "3",
//            username = "Bạn thân",
//            steps = 1134
//        ),
//        LeaderboardEntry(
//            userId = "4",
//            username = "Mẹ",
//            steps = 852
//        ),
//        LeaderboardEntry(
//            userId = "5",
//            username = "Bố",
//            steps = 672
//        )
//    )
//
//    // Set up fake repository data
//    fakeRepository.setDailySteps(2000)
//    fakeRepository.setLeaderboard(mockLeaderboard)
//
//    val previewViewModel = object : HealthViewModel(fakeRepository) {
//        override val uiState = MutableStateFlow(
//            HealthUiState(
//                currentSteps = 2000,
//                duration = Duration.ofSeconds(16225).toMillis(), // 4:30:25
//                leaderboard = mockLeaderboard,
//                isTracking = true,
//                startTime = LocalDateTime.now().minusHours(4).minusMinutes(30).minusSeconds(25)
//            )
//        )
//    }
//
//    ChildSafeTheme {
//        Surface(
//            modifier = Modifier.fillMaxSize(),
//            color = MaterialTheme.colorScheme.background
//        ) {
//            WalkingTrackingScreen(
//                onBackClick = {},
//                viewModel = previewViewModel
//            )
//        }
//    }
//}
//
//
//        )
//        override val isDebugMode = true
//    }
//
//    ChildSafeTheme {
//        Surface(
//            modifier = Modifier.fillMaxSize(),
//            color = MaterialTheme.colorScheme.background
//        ) {
//            WalkingTrackingScreen(
//                onBackClick = {},
//                viewModel = debugViewModel
//            )
//        }
//    }
//}
//
//
