package com.vision202020.app.ui.breakui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BreakScreenContent(
    remainingSeconds: Int,
    totalSeconds: Int = 20,
    strictMode: Boolean = false,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Breathing relaxation animation (4s cycle: expand & contract)
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
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
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Header icon & guidance
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Rest Your Eyes",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Look at an object 20 feet (6m) away.\nBlink slowly and breathe deeply.",
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Central animated relaxation circle with progress ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // Outer breathing pulsing halo
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(scale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Circular countdown progress
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(180.dp),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF334155),
                    strokeWidth = 8.dp
                )

                // Seconds remaining display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$remainingSeconds",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "seconds",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Action buttons: Snooze & Skip
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(text = "Snooze 5m", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                if (strictMode) {
                    // Requires 2s long press to dismiss in strict mode
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .background(
                                color = if (isHoldingToSkip) Color(0xFFEF4444) else Color(0xFF334155),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isHoldingToSkip = true
                                        val released = tryAwaitRelease()
                                        isHoldingToSkip = false
                                    },
                                    onLongPress = {
                                        isHoldingToSkip = false
                                        onSkip()
                                    }
                                )
                            }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHoldingToSkip) "Keep holding..." else "Hold 2s to Skip",
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
                        Text(text = "Skip Break", fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
