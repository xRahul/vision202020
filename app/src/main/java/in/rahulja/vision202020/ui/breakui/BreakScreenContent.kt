package `in`.rahulja.vision202020.ui.breakui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.rahulja.vision202020.R
import kotlinx.coroutines.withTimeoutOrNull

/** Strict mode requires an actual two-second hold, not a platform long-press (~500 ms). */
const val HOLD_TO_SKIP_MS = 2000L

@Composable
fun BreakScreenContent(
    remainingSeconds: Int,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
    totalSeconds: Int = 20,
    strictMode: Boolean = false,
    snoozeMinutes: Int = 5
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val progress = (remainingSeconds.toFloat() / totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    var isHoldingToSkip by remember { mutableStateOf(false) }

    val backgroundModifier = Modifier.background(
        if (MaterialTheme.colorScheme.background == Color.Black) {
            SolidColor(Color.Black)
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A).copy(alpha = 0.95f),
                    Color(0xFF020617).copy(alpha = 0.98f)
                )
            )
        }
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(backgroundModifier),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            BreakScreenLandscapeContent(
                maxHeight = maxHeight,
                scale = scale,
                progress = progress,
                remainingSeconds = remainingSeconds,
                strictMode = strictMode,
                snoozeMinutes = snoozeMinutes,
                isHoldingToSkip = isHoldingToSkip,
                onHoldingChanged = { isHoldingToSkip = it },
                onSkip = onSkip,
                onSnooze = onSnooze
            )
        } else {
            BreakScreenPortraitContent(
                maxHeight = maxHeight,
                scale = scale,
                progress = progress,
                remainingSeconds = remainingSeconds,
                strictMode = strictMode,
                snoozeMinutes = snoozeMinutes,
                isHoldingToSkip = isHoldingToSkip,
                onHoldingChanged = { isHoldingToSkip = it },
                onSkip = onSkip,
                onSnooze = onSnooze
            )
        }
    }
}

@Composable
private fun BreakScreenLandscapeContent(
    maxHeight: Dp,
    scale: Float,
    progress: Float,
    remainingSeconds: Int,
    strictMode: Boolean,
    snoozeMinutes: Int,
    isHoldingToSkip: Boolean,
    onHoldingChanged: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .defaultMinSize(minHeight = maxHeight)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.1f)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.break_rest_eyes),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.break_guidance),
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            BreakActionButtons(
                strictMode = strictMode,
                snoozeMinutes = snoozeMinutes,
                isHoldingToSkip = isHoldingToSkip,
                onHoldingChanged = onHoldingChanged,
                onSkip = onSkip,
                onSnooze = onSnooze
            )
        }

        Column(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BreakCountdownCircle(
                scale = scale,
                progress = progress,
                remainingSeconds = remainingSeconds,
                size = 160.dp,
                haloSize = 140.dp,
                indicatorSize = 135.dp,
                numberTextSize = 38.sp
            )
        }
    }
}

@Composable
private fun BreakScreenPortraitContent(
    maxHeight: Dp,
    scale: Float,
    progress: Float,
    remainingSeconds: Int,
    strictMode: Boolean,
    snoozeMinutes: Int,
    isHoldingToSkip: Boolean,
    onHoldingChanged: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .defaultMinSize(minHeight = maxHeight)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.break_rest_eyes),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.break_guidance),
            fontSize = 16.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        BreakCountdownCircle(
            scale = scale,
            progress = progress,
            remainingSeconds = remainingSeconds,
            size = 220.dp,
            haloSize = 190.dp,
            indicatorSize = 180.dp,
            numberTextSize = 52.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        BreakActionButtons(
            strictMode = strictMode,
            snoozeMinutes = snoozeMinutes,
            isHoldingToSkip = isHoldingToSkip,
            onHoldingChanged = onHoldingChanged,
            onSkip = onSkip,
            onSnooze = onSnooze
        )
    }
}

@Composable
private fun BreakCountdownCircle(
    scale: Float,
    progress: Float,
    remainingSeconds: Int,
    size: Dp,
    haloSize: Dp,
    indicatorSize: Dp,
    numberTextSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .size(haloSize)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(indicatorSize),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color(0xFF334155),
            strokeWidth = 8.dp
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$remainingSeconds",
                fontSize = numberTextSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.break_seconds),
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun BreakActionButtons(
    strictMode: Boolean,
    snoozeMinutes: Int,
    isHoldingToSkip: Boolean,
    onHoldingChanged: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnSkip by rememberUpdatedState(onSkip)
    val currentOnHoldingChanged by rememberUpdatedState(onHoldingChanged)
    val holdSkipLabel = stringResource(R.string.break_hold_skip)
    var confirmingSkip by remember { mutableStateOf(false) }

    if (confirmingSkip) {
        // Inline instead of a Dialog: the overlay window has no activity token for a dialog window.
        SkipConfirmation(
            onConfirm = {
                confirmingSkip = false
                currentOnSkip()
            },
            onCancel = { confirmingSkip = false },
            modifier = modifier
        )
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onSnooze,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
            modifier = Modifier.height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.break_snooze, snoozeMinutes),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (strictMode) {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = holdSkipLabel
                        // A two-second hold is not possible with TalkBack; offer a confirmed skip instead.
                        customActions = listOf(
                            CustomAccessibilityAction(holdSkipLabel) {
                                confirmingSkip = true
                                true
                            }
                        )
                    }
                    .background(
                        color = if (isHoldingToSkip) Color(0xFFEF4444) else Color(0xFF334155),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            currentOnHoldingChanged(true)
                            val released = withTimeoutOrNull(HOLD_TO_SKIP_MS) {
                                waitForUpOrCancellation()
                                true
                            }
                            currentOnHoldingChanged(false)
                            // Timeout expired while still pressed: a real 2s hold.
                            if (released == null) currentOnSkip()
                        }
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isHoldingToSkip) {
                        stringResource(R.string.break_keep_holding)
                    } else {
                        holdSkipLabel
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Button(
                onClick = onSkip,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.break_skip),
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SkipConfirmation(onConfirm: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.break_skip_confirm_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.break_skip_confirm_message),
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                modifier = Modifier.height(48.dp)
            ) {
                Text(text = stringResource(R.string.break_skip_confirm_cancel), fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier.height(48.dp)
            ) {
                Text(text = stringResource(R.string.break_skip_confirm_action), fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
