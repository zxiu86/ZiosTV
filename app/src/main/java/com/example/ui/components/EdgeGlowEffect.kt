package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenBright
import com.example.ui.theme.GlowRed
import com.example.ui.theme.GlowRedBright

enum class GlowType {
    WINNER_GREEN,
    LOSER_RED
}

@Composable
fun EdgeGlowEffect(
    glowType: GlowType?,
    onFinish: () -> Unit = {}
) {
    if (glowType == null) return

    val alpha = remember(glowType) { Animatable(0f) }

    LaunchedEffect(glowType) {
        // Repeat smooth pulse 3 times as specified in clarifying answer
        repeat(3) {
            alpha.animateTo(
                targetValue = 0.85f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            alpha.animateTo(
                targetValue = 0.95f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            )
            alpha.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        onFinish()
    }

    val currentAlpha = alpha.value
    if (currentAlpha <= 0.01f) return

    val baseColor = when (glowType) {
        GlowType.WINNER_GREEN -> GlowGreenBright
        GlowType.LOSER_RED -> GlowRedBright
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 28f * density
        val w = size.width
        val h = size.height

        // Top edge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(baseColor.copy(alpha = currentAlpha), Color.Transparent),
                startY = 0f,
                endY = strokeWidth
            ),
            topLeft = Offset.Zero,
            size = Size(w, strokeWidth)
        )

        // Bottom edge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, baseColor.copy(alpha = currentAlpha)),
                startY = h - strokeWidth,
                endY = h
            ),
            topLeft = Offset(0f, h - strokeWidth),
            size = Size(w, strokeWidth)
        )

        // Left edge
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(baseColor.copy(alpha = currentAlpha), Color.Transparent),
                startX = 0f,
                endX = strokeWidth
            ),
            topLeft = Offset.Zero,
            size = Size(strokeWidth, h)
        )

        // Right edge
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, baseColor.copy(alpha = currentAlpha)),
                startX = w - strokeWidth,
                endX = w
            ),
            topLeft = Offset(w - strokeWidth, 0f),
            size = Size(strokeWidth, h)
        )
    }
}
