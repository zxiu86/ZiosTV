package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun TimerCountdownView(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = (remainingSeconds.toFloat() / totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val isUrgent = remainingSeconds in 1..10

    val timerColor by animateColorAsState(
        targetValue = if (isUrgent) GlowRed else BuzzerGold,
        label = "timer_color"
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GameSurfaceElevated)
            .border(1.dp, timerColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                tint = timerColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "الوقت المتبقي للإجابة",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = timeFormatted,
            color = timerColor,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = timerColor,
            trackColor = Color(0xFF222834)
        )
    }
}
