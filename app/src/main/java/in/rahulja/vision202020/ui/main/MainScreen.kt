package `in`.rahulja.vision202020.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.service.NotificationHelper
import `in`.rahulja.vision202020.ui.components.PermissionGateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    status: TimerStatus,
    preferences: UserPreferences,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onTriggerBreak: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vision 20-20-20",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                MainScreenLandscapeContent(
                    maxHeight = maxHeight,
                    status = status,
                    preferences = preferences,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop,
                    onTriggerBreak = onTriggerBreak,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
            } else {
                MainScreenPortraitContent(
                    maxHeight = maxHeight,
                    status = status,
                    preferences = preferences,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop,
                    onTriggerBreak = onTriggerBreak,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
            }
        }
    }
}

@Composable
private fun MainScreenLandscapeContent(
    maxHeight: Dp,
    status: TimerStatus,
    preferences: UserPreferences,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onTriggerBreak: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .defaultMinSize(minHeight = maxHeight)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Timer Progress & Mode Badge
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ModeBadge()
            Spacer(modifier = Modifier.height(12.dp))
            TimerProgressCircle(
                status = status,
                preferences = preferences,
                outerSize = 190.dp,
                indicatorSize = 170.dp,
                timeTextSize = 36.sp
            )
        }

        // Right Column: Permission Gate & Action Buttons
        Column(
            modifier = Modifier
                .weight(1.2f)
                .padding(start = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PermissionGateCard(
                hasOverlayPermission = hasOverlayPermission,
                hasNotificationPermission = hasNotificationPermission,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestNotificationPermission = onRequestNotificationPermission
            )

            Spacer(modifier = Modifier.height(12.dp))

            TimerActionControls(
                status = status,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onTriggerBreak = onTriggerBreak
            )
        }
    }
}

@Composable
private fun MainScreenPortraitContent(
    maxHeight: Dp,
    status: TimerStatus,
    preferences: UserPreferences,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onTriggerBreak: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .defaultMinSize(minHeight = maxHeight)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PermissionGateCard(
            hasOverlayPermission = hasOverlayPermission,
            hasNotificationPermission = hasNotificationPermission,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onRequestNotificationPermission = onRequestNotificationPermission
        )

        Spacer(modifier = Modifier.height(8.dp))

        ModeBadge()

        Spacer(modifier = Modifier.height(16.dp))

        TimerProgressCircle(
            status = status,
            preferences = preferences,
            outerSize = 240.dp,
            indicatorSize = 220.dp,
            timeTextSize = 46.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        TimerActionControls(
            status = status,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onStop = onStop,
            onTriggerBreak = onTriggerBreak
        )
    }
}

@Composable
private fun ModeBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "📱 Mobile Screen Guard Active",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TimerProgressCircle(
    status: TimerStatus,
    preferences: UserPreferences,
    outerSize: Dp,
    indicatorSize: Dp,
    timeTextSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val (timeString, subtitleString, progress) = when (status) {
        is TimerStatus.Working -> {
            val formatted = NotificationHelper.formatTime(status.remainingSeconds)
            val prog = (status.remainingSeconds.toFloat() / status.totalSeconds.coerceAtLeast(1).toFloat())
            val sub = if (status.isScreenPaused) "Paused (Screen Off)" else "Working Session"
            Triple(formatted, sub, prog)
        }
        is TimerStatus.RestBreak -> {
            val formatted = "${status.remainingSeconds}s"
            val prog = (status.remainingSeconds.toFloat() / status.totalSeconds.coerceAtLeast(1).toFloat())
            Triple(formatted, "Eye Rest Break", prog)
        }
        is TimerStatus.Snoozed -> {
            val formatted = NotificationHelper.formatTime(status.remainingSeconds)
            val prog = (status.remainingSeconds.toFloat() / status.totalSeconds.coerceAtLeast(1).toFloat())
            Triple(formatted, "Break Snoozed", prog)
        }
        is TimerStatus.Idle -> {
            val formatted = "${preferences.workDurationMinutes}:00"
            Triple(formatted, "Ready to Start", 1f)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(outerSize)
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(indicatorSize),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 10.dp
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeString,
                fontSize = timeTextSize,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitleString,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerActionControls(
    status: TimerStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onTriggerBreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (status) {
            is TimerStatus.Idle -> {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Start Session", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            is TimerStatus.Working -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (status.isScreenPaused) {
                        Button(
                            onClick = onResume,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onPause,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onTriggerBreak,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Rest Now", fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444))
                    }
                }
            }
            else -> {
                Button(
                    onClick = onStop,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Stop Session", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
