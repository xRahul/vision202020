package com.vision202020.app.ui.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vision202020.app.data.TimerProfile
import com.vision202020.app.data.UserPreferences
import com.vision202020.app.domain.TimerStatus
import com.vision202020.app.service.NotificationHelper
import com.vision202020.app.ui.components.PermissionGateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    status: TimerStatus,
    preferences: UserPreferences,
    hasOverlayPermission: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onTriggerBreak: () -> Unit,
    onOpenSettings: () -> Unit,
    onPermissionRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vision 20-20-20",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Permission card if needed
            PermissionGateCard(
                hasOverlayPermission = hasOverlayPermission,
                onPermissionRequested = onPermissionRequested
            )

            // Mode Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (preferences.timerProfile == TimerProfile.MOBILE_SCREEN_AWARE) {
                        "📱 Mobile Reading (Screen-Aware)"
                    } else {
                        "💻 Laptop / Desk Companion"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )
            }

            // Central Timer Progress Circle
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
                modifier = Modifier.size(240.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(220.dp),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B),
                    strokeWidth = 10.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeString,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitleString,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (status) {
                    is TimerStatus.Idle -> {
                        Button(
                            onClick = onStart,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Start Session", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onPause,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = onTriggerBreak,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("Rest Now", fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = onStop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF334155))
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
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
