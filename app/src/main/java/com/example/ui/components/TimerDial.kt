package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TimerState

@Composable
fun TimerDial(
    secondsRemaining: Int,
    totalSeconds: Int,
    timerState: TimerState,
    modifier: Modifier = Modifier,
    dialSize: Dp = 260.dp
) {
    val progress = if (totalSeconds > 0 && timerState != TimerState.IDLE) {
        (secondsRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    // Infinite breathing glow when active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val hours = secondsRemaining / 3600
    val minutes = (secondsRemaining % 3600) / 60
    val seconds = secondsRemaining % 60

    val timeString = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = modifier
            .size(dialSize)
            .testTag("timer_dial"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc
            val sweepAngle = 360f * progress
            val activeColor = when (timerState) {
                TimerState.RUNNING -> {
                    if (secondsRemaining <= 60) {
                        Color(0xFFEF4444) // Red for final 60 seconds
                    } else {
                        Color(0xFF38BDF8) // Cyan blue
                    }
                }
                TimerState.PAUSED -> Color(0xFFF59E0B) // Amber
                TimerState.LOCKED -> Color(0xFFEF4444)
                TimerState.IDLE -> Color(0xFF0284C7)
            }

            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.7f),
                    activeColor,
                    activeColor.copy(alpha = 0.9f)
                ),
                center = center
            )

            drawArc(
                brush = gradientBrush,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Status tag
            val statusText = when (timerState) {
                TimerState.IDLE -> "READY"
                TimerState.RUNNING -> if (secondsRemaining <= 60) "LOCKING SOON" else "ACTIVE"
                TimerState.PAUSED -> "PAUSED"
                TimerState.LOCKED -> "LOCKED"
            }

            val statusColor = when (timerState) {
                TimerState.IDLE -> Color(0xFF94A3B8)
                TimerState.RUNNING -> if (secondsRemaining <= 60) Color(0xFFEF4444) else Color(0xFF38BDF8)
                TimerState.PAUSED -> Color(0xFFF59E0B)
                TimerState.LOCKED -> Color(0xFFEF4444)
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = if (timerState == TimerState.RUNNING) pulseAlpha * 0.25f else 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large digital countdown text
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (hours > 0) 36.sp else 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (timerState == TimerState.RUNNING) "Auto-locks Honor Tab" else "Set lock timer",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
