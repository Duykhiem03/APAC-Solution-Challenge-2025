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
import androidx.compose.foundation.layout.Arrangement
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
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

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
                }
            }
        }

        // Character monitor with road
        CharacterMonitorWithRoad()

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
                color = Color(0xFFFFF3AF),
                radius = circleRadius,
                center = centerOffset
            )

//            // Draw border circle (lighter than background)
//            drawCircle(
//                color = Color(0xFFFFF3AF),
//                radius = circleRadius,
//                center = centerOffset,
//                style = Stroke(width = 1.dp.toPx())
//            )

            if (progress > 0f) {
                // Draw progress arc
                val sweepAngle = progress * 360f
                drawArc(
                    color = Color(0xFFFF6B6B),
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
            color = Color.Black,
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
