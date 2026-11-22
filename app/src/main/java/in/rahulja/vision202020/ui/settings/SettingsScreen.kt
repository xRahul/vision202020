package `in`.rahulja.vision202020.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.rahulja.vision202020.R
import `in`.rahulja.vision202020.data.AppThemeMode
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.ui.about.AboutLibrariesActivity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onWorkDurationChanged: (Int) -> Unit,
    onRestDurationChanged: (Int) -> Unit,
    onNaturalBreakChanged: (Int) -> Unit,
    onSnoozeChanged: (Int) -> Unit,
    onEnforcementModeChanged: (EnforcementMode) -> Unit,
    onStrictModeChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onStartOnBootChanged: (Boolean) -> Unit,
    onAppThemeChanged: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
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
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(title = stringResource(R.string.theme_card_title)) {
                Text(
                    text = stringResource(R.string.theme_card_body),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChip(
                        selected = preferences.appTheme == AppThemeMode.OLED_BLACK,
                        label = stringResource(R.string.theme_oled),
                        onClick = { onAppThemeChanged(AppThemeMode.OLED_BLACK) }
                    )
                    ThemeChip(
                        selected = preferences.appTheme == AppThemeMode.DARK,
                        label = stringResource(R.string.theme_dark),
                        onClick = { onAppThemeChanged(AppThemeMode.DARK) }
                    )
                    ThemeChip(
                        selected = preferences.appTheme == AppThemeMode.LIGHT,
                        label = stringResource(R.string.theme_light),
                        onClick = { onAppThemeChanged(AppThemeMode.LIGHT) }
                    )
                }
            }

            SettingsCard(title = stringResource(R.string.mobile_card_title)) {
                Text(
                    text = stringResource(R.string.mobile_card_body),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            SettingsCard(title = stringResource(R.string.durations_title)) {
                Text(
                    stringResource(R.string.work_duration_label),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 20, 25, 30).forEach { mins ->
                        ThemeChip(
                            selected = preferences.workDurationMinutes == mins,
                            label = stringResource(R.string.duration_minutes_short, mins),
                            onClick = { onWorkDurationChanged(mins) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.rest_duration_label),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 20, 30, 45, 60).forEach { secs ->
                        ThemeChip(
                            selected = preferences.restDurationSeconds == secs,
                            label = stringResource(R.string.duration_seconds_short, secs),
                            onClick = { onRestDurationChanged(secs) }
                        )
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.natural_break_title)) {
                Text(
                    stringResource(R.string.natural_break_body),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 5, 10, 15).forEach { mins ->
                        ThemeChip(
                            selected = preferences.naturalBreakMinutes == mins,
                            label = stringResource(R.string.duration_minutes_short, mins),
                            onClick = { onNaturalBreakChanged(mins) }
                        )
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.enforcement_title)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChip(
                        selected = preferences.enforcementMode == EnforcementMode.BLOCKER_OVERLAY,
                        label = stringResource(R.string.enforcement_blocker),
                        onClick = { onEnforcementModeChanged(EnforcementMode.BLOCKER_OVERLAY) }
                    )
                    ThemeChip(
                        selected = preferences.enforcementMode == EnforcementMode.NOTIFICATION_ONLY,
                        label = stringResource(R.string.enforcement_notification),
                        onClick = { onEnforcementModeChanged(EnforcementMode.NOTIFICATION_ONLY) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ToggleRow(
                    label = stringResource(R.string.strict_mode_label),
                    checked = preferences.strictMode,
                    onCheckedChange = onStrictModeChanged
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.snooze_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 5, 10).forEach { mins ->
                        ThemeChip(
                            selected = preferences.snoozeMinutes == mins,
                            label = stringResource(R.string.duration_minutes_short, mins),
                            onClick = { onSnoozeChanged(mins) }
                        )
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.alerts_title)) {
                ToggleRow(
                    label = stringResource(R.string.sound_label),
                    checked = preferences.soundEnabled,
                    onCheckedChange = onSoundChanged
                )
                Spacer(modifier = Modifier.height(8.dp))
                ToggleRow(
                    label = stringResource(R.string.vibration_label),
                    checked = preferences.vibrationEnabled,
                    onCheckedChange = onVibrationChanged
                )
            }

            SettingsCard(title = stringResource(R.string.startup_title)) {
                ToggleRow(
                    label = stringResource(R.string.start_on_boot_label),
                    checked = preferences.startOnBoot,
                    onCheckedChange = onStartOnBootChanged
                )
            }

            SettingsCard(title = stringResource(R.string.battery_title)) {
                Text(
                    text = stringResource(R.string.battery_body),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.battery_button))
                }
            }

            val versionName = remember {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            PackageManager.PackageInfoFlags.of(0)
                        ).versionName
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } ?: "1.1.8"
                } catch (e: Exception) {
                    "1.1.8"
                }
            }
            val uriHandler = LocalUriHandler.current

            SettingsCard(title = stringResource(R.string.about_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.about_version, versionName),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.about_author),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.about_license),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { uriHandler.openUri("https://github.com/xRahul/vision202020") },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = stringResource(R.string.about_source),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { uriHandler.openUri("https://github.com/xRahul/vision202020/releases/latest") }
                        ) {
                            Text(
                                text = stringResource(R.string.about_updates),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(context, AboutLibrariesActivity::class.java).apply {
                                        putExtra(AboutLibrariesActivity.EXTRA_THEME, preferences.appTheme.name)
                                    }
                                )
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.about_licenses),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
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
