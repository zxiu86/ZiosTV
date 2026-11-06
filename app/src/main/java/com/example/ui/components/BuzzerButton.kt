package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.BuzzerGoldDark
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenBright
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted

enum class BuzzerState {
    IDLE_OPEN,
    WON_BY_ME,
    LOCKED_BY_OTHER,
    DISQUALIFIED
}

@Composable
fun BuzzerButton(
    buzzerState: BuzzerState,
    onBuzzerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buzzer_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (buzzerState == BuzzerState.IDLE_OPEN) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isClickable = buzzerState == BuzzerState.IDLE_OPEN

    val buttonColors = when (buzzerState) {
        BuzzerState.IDLE_OPEN -> listOf(BuzzerGoldBright, BuzzerGold, BuzzerGoldDark)
        BuzzerState.WON_BY_ME -> listOf(GlowGreenBright, GlowGreen, Color(0xFF047857))
        BuzzerState.LOCKED_BY_OTHER -> listOf(Color(0xFF2D1619), Color(0xFF1F1113), Color(0xFF120A0B))
        BuzzerState.DISQUALIFIED -> listOf(GameSurfaceElevated, Color(0xFF151820), Color(0xFF0F1116))
    }

    val borderColor = when (buzzerState) {
        BuzzerState.IDLE_OPEN -> Color.White.copy(alpha = 0.6f)
        BuzzerState.WON_BY_ME -> Color.White.copy(alpha = 0.8f)
        BuzzerState.LOCKED_BY_OTHER -> GlowRed.copy(alpha = 0.5f)
        BuzzerState.DISQUALIFIED -> TextMuted.copy(alpha = 0.3f)
    }

    val icon = when (buzzerState) {
        BuzzerState.IDLE_OPEN -> Icons.Default.TouchApp
        BuzzerState.WON_BY_ME -> Icons.Default.CheckCircle
        BuzzerState.LOCKED_BY_OTHER -> Icons.Default.Lock
        BuzzerState.DISQUALIFIED -> Icons.Default.Lock
    }

    val labelText = when (buzzerState) {
        BuzzerState.IDLE_OPEN -> "اضغط للحصول على\nحق الإجابة"
        BuzzerState.WON_BY_ME -> "أنت الفائز بالبوزير!\nدورك في الإجابة"
        BuzzerState.LOCKED_BY_OTHER -> "فاز لاعب آخر بالبوزير\n(مغلق)"
        BuzzerState.DISQUALIFIED -> "تم استبعادك من\nهذا السؤال"
    }

    val contentColor = when (buzzerState) {
        BuzzerState.IDLE_OPEN -> Color.Black
        BuzzerState.WON_BY_ME -> Color.White
        BuzzerState.LOCKED_BY_OTHER -> GlowRed.copy(alpha = 0.8f)
        BuzzerState.DISQUALIFIED -> TextMuted
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .shadow(
                    elevation = if (isClickable) 24.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = if (isClickable) BuzzerGold else Color.Black
                )
                .clip(CircleShape)
                .background(Brush.radialGradient(buttonColors))
                .border(width = 6.dp, color = borderColor, shape = CircleShape)
                .clickable(
                    enabled = isClickable,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onBuzzerClick
                )
                .padding(24.dp)
                .testTag("buzzer_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = labelText,
                    tint = contentColor,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = labelText,
                    color = contentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
