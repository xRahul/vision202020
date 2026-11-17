package `in`.rahulja.vision202020.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.rahulja.vision202020.data.AppThemeMode
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onWorkDurationChanged: (Int) -> Unit,
    onRestDurationChanged: (Int) -> Unit,
    onNaturalBreakChanged: (Int) -> Unit,
    onEnforcementModeChanged: (EnforcementMode) -> Unit,
    onStrictModeChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onAppThemeChanged: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Card
            SettingsCard(title = "App Theme") {
                Text(
                    text = "OLED Black turns off pixels completely on OLED screens to maximize battery life.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.appTheme == AppThemeMode.OLED_BLACK,
                        onClick = { onAppThemeChanged(AppThemeMode.OLED_BLACK) },
                        label = { Text("⚡ OLED Black") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = preferences.appTheme == AppThemeMode.DARK,
                        onClick = { onAppThemeChanged(AppThemeMode.DARK) },
                        label = { Text("🌙 Dark Slate") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = preferences.appTheme == AppThemeMode.LIGHT,
                        onClick = { onAppThemeChanged(AppThemeMode.LIGHT) },
                        label = { Text("☀️ Light") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Mobile Screen-Aware Info Card
            SettingsCard(title = "📱 Mobile Screen-Aware Protection") {
                Text(
                    text = "Vision 20-20-20 is dedicated to mobile eye protection. It counts active screen time while unlocked, sleeps with zero battery impact while locked, and auto-resets when you take a natural break.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // Section 1: Intervals
            SettingsCard(title = "Work & Rest Durations") {
                Text("Work Duration (Standard 20m):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20, 25, 30).forEach { mins ->
                        FilterChip(
                            selected = preferences.workDurationMinutes == mins,
                            onClick = { onWorkDurationChanged(mins) },
                            label = { Text("${mins}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Rest Duration (Standard 20s):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20, 30, 45, 60).forEach { secs ->
                        FilterChip(
                            selected = preferences.restDurationSeconds == secs,
                            onClick = { onRestDurationChanged(secs) },
                            label = { Text("${secs}s") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Section 2: Natural Break
            SettingsCard(title = "Natural Break Reset Threshold") {
                Text("Locking your phone for this long resets the 20m work timer:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 5, 10, 15).forEach { mins ->
                        FilterChip(
                            selected = preferences.naturalBreakMinutes == mins,
                            onClick = { onNaturalBreakChanged(mins) },
                            label = { Text("${mins}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Section 3: Enforcement Mode
            SettingsCard(title = "Break Enforcement") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.enforcementMode == EnforcementMode.BLOCKER_OVERLAY,
                        onClick = { onEnforcementModeChanged(EnforcementMode.BLOCKER_OVERLAY) },
                        label = { Text("Full-Screen Blocker (20s)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = preferences.enforcementMode == EnforcementMode.NOTIFICATION_ONLY,
                        onClick = { onEnforcementModeChanged(EnforcementMode.NOTIFICATION_ONLY) },
                        label = { Text("Notification Only") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ToggleRow(
                    label = "Strict Mode (Hold 2s to skip)",
                    checked = preferences.strictMode,
                    onCheckedChange = onStrictModeChanged
                )
            }

            // Section 4: Sound & Haptics
            SettingsCard(title = "Alerts & Feedback") {
                ToggleRow(
                    label = "Sound Alerts (Soothing Chime)",
                    checked = preferences.soundEnabled,
                    onCheckedChange = onSoundChanged
                )
                Spacer(modifier = Modifier.height(8.dp))
                ToggleRow(
                    label = "Vibration Feedback",
                    checked = preferences.vibrationEnabled,
                    onCheckedChange = onVibrationChanged
                )
            }

            // Section 5: About
            val uriHandler = LocalUriHandler.current
            val context = LocalContext.current
            val versionName = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1.0"
                } catch (e: Exception) {
                    "1.1.0"
                }
            }

            SettingsCard(title = "ℹ️ About Vision 20-20-20") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Version: v$versionName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Author: Rahul Jain",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "License: GNU General Public License v3.0 (GPL-3.0-or-later)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { uriHandler.openUri("https://github.com/xRahul/vision202020") },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "🌐 Source Code on GitHub",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
