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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.rahulja.vision202020.R
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.domain.PauseReason
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
    showFullScreenIntentCard: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onStop: () -> Unit,
    onTriggerBreak: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestFullScreenIntent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            if (maxWidth > maxHeight) {
                TimerContent(
                    maxHeight = maxHeight,
                    isLandscape = true,
                    status = status,
                    preferences = preferences,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    showFullScreenIntentCard = showFullScreenIntentCard,
                    outerSize = 190.dp,
                    indicatorSize = 170.dp,
                    timeTextSize = 36.sp,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onReset = onReset,
                    onStop = onStop,
                    onTriggerBreak = onTriggerBreak,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestFullScreenIntent = onRequestFullScreenIntent
                )
            } else {
                TimerContent(
                    maxHeight = maxHeight,
                    isLandscape = false,
                    status = status,
                    preferences = preferences,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    showFullScreenIntentCard = showFullScreenIntentCard,
                    outerSize = 240.dp,
                    indicatorSize = 220.dp,
                    timeTextSize = 46.sp,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onReset = onReset,
                    onStop = onStop,
                    onTriggerBreak = onTriggerBreak,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestFullScreenIntent = onRequestFullScreenIntent
                )
            }
        }
    }
}

@Composable
private fun TimerContent(
    maxHeight: Dp,
    isLandscape: Boolean,
    status: TimerStatus,
    preferences: UserPreferences,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    showFullScreenIntentCard: Boolean,
    outerSize: Dp,
    indicatorSize: Dp,
    timeTextSize: TextUnit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onStop: () -> Unit,
    onTriggerBreak: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestFullScreenIntent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionGate: @Composable () -> Unit = {
        PermissionGateCard(
            hasOverlayPermission = hasOverlayPermission,
            showOverlayCard = preferences.enforcementMode == EnforcementMode.BLOCKER_OVERLAY,
            hasNotificationPermission = hasNotificationPermission,
            showFullScreenIntentCard = showFullScreenIntentCard,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestFullScreenIntent = onRequestFullScreenIntent
        )
    }
    val timerCircle: @Composable () -> Unit = {
        TimerProgressCircle(
            status = status,
            preferences = preferences,
            outerSize = outerSize,
            indicatorSize = indicatorSize,
            timeTextSize = timeTextSize
        )
    }
    val controls: @Composable () -> Unit = {
        TimerActionControls(
            status = status,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onReset = onReset,
            onStop = onStop,
            onTriggerBreak = onTriggerBreak
        )
    }

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .defaultMinSize(minHeight = maxHeight)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ModeBadge()
                Spacer(modifier = Modifier.height(12.dp))
                timerCircle()
            }
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .padding(start = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                permissionGate()
                Spacer(modifier = Modifier.height(12.dp))
                controls()
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .defaultMinSize(minHeight = maxHeight)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            permissionGate()
            Spacer(modifier = Modifier.height(8.dp))
            ModeBadge()
            Spacer(modifier = Modifier.height(16.dp))
            timerCircle()
            Spacer(modifier = Modifier.height(16.dp))
            controls()
        }
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
            text = stringResource(R.string.mode_badge),
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
    val pausedText = stringResource(R.string.subtitle_paused)
    val pausedScreenOffText = stringResource(R.string.subtitle_paused_screen_off)
    val (timeString, subtitleString, progress) = when (status) {
        is TimerStatus.Working -> {
            val formatted = NotificationHelper.formatTime(status.remainingSeconds)
            val prog = status.remainingSeconds.toFloat() / status.totalSeconds.coerceAtLeast(1).toFloat()
            val sub = when (status.pauseReason) {
                PauseReason.NONE -> stringResource(R.string.subtitle_working)
                PauseReason.MANUAL -> pausedText
                PauseReason.SCREEN_OFF -> pausedScreenOffText
            }
            Triple(formatted, sub, prog)
        }
        is TimerStatus.RestBreak -> Triple(
            stringResource(R.string.rest_seconds_value, status.remainingSeconds),
            stringResource(R.string.subtitle_break),
            status.remainingSeconds.toFloat() / status.totalSeconds.coerceAtLeast(1).toFloat()
        )
        is TimerStatus.Snoozed -> Triple(
            NotificationHelper.formatTime(status.remainingSeconds),
            stringResource(R.string.subtitle_snoozed),
            status.remainingSeconds.toFloat() / status.totalSeconds.coerceAtLeast(1).toFloat()
        )
        is TimerStatus.Idle -> Triple(
            stringResource(R.string.idle_time_value, preferences.workDurationMinutes),
            stringResource(R.string.subtitle_ready),
            1f
        )
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
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
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
    onReset: () -> Unit,
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
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_start_session),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            is TimerStatus.Working -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.action_resume),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
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
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.action_pause),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onTriggerBreak,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(
                            stringResource(R.string.action_take_break),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_reset),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cd_stop),
                            tint = Color(0xFFEF4444)
                        )
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
                    Text(
                        stringResource(R.string.action_stop_session),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
