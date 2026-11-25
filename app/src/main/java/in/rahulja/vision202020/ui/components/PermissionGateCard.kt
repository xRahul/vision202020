package `in`.rahulja.vision202020.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import `in`.rahulja.vision202020.R

@SuppressLint("InlinedApi") // ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT only runs on API 34+.
@Composable
fun PermissionGateCard(
    hasOverlayPermission: Boolean,
    showOverlayCard: Boolean,
    hasNotificationPermission: Boolean,
    showFullScreenIntentCard: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestFullScreenIntent: () -> Unit,
    modifier: Modifier = Modifier,
    showBreakChannelCard: Boolean = false,
    showBatteryCard: Boolean = false,
    onOpenBreakChannelSettings: () -> Unit = {},
    onRequestBatteryExemption: () -> Unit = {}
) {
    Column(modifier = modifier) {
        if (!hasNotificationPermission) {
            PermissionCard(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.permission_notification_title),
                body = stringResource(R.string.permission_notification_body),
                buttonLabel = stringResource(R.string.permission_enable_notifications),
                onClick = onRequestNotificationPermission
            )
        }

        if (hasNotificationPermission && showBreakChannelCard) {
            PermissionCard(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.permission_break_channel_title),
                body = stringResource(R.string.permission_break_channel_body),
                buttonLabel = stringResource(R.string.permission_open_alert_settings),
                onClick = onOpenBreakChannelSettings
            )
        }

        if (showOverlayCard && !hasOverlayPermission) {
            val context = LocalContext.current
            PermissionCard(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.permission_overlay_title),
                body = stringResource(R.string.permission_overlay_body),
                buttonLabel = stringResource(R.string.permission_grant_overlay),
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    runCatching { context.startActivity(intent) }
                    onRequestOverlayPermission()
                }
            )
        }

        if (showFullScreenIntentCard) {
            val context = LocalContext.current
            PermissionCard(
                icon = Icons.Default.Fullscreen,
                title = stringResource(R.string.permission_fullscreen_title),
                body = stringResource(R.string.permission_fullscreen_body),
                buttonLabel = stringResource(R.string.permission_grant_fullscreen),
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        "package:${context.packageName}".toUri()
                    )
                    runCatching { context.startActivity(intent) }
                    onRequestFullScreenIntent()
                }
            )
        }

        if (showBatteryCard) {
            PermissionCard(
                icon = Icons.Default.BatteryAlert,
                title = stringResource(R.string.permission_battery_title),
                body = stringResource(R.string.permission_battery_body),
                buttonLabel = stringResource(R.string.permission_open_battery_settings),
                onClick = onRequestBatteryExemption
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    buttonLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = body,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = buttonLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
