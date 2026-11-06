package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.model.Player
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.PlayerColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularWheelView(
    players: List<Player>,
    targetAngle: Float,
    isSpinning: Boolean,
    onSpinFinished: () -> Unit = {},
    onTick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    var needleAngle by remember { mutableFloatStateOf(0f) }

    // Pre-allocate Paint object once to avoid GC pauses during 60/120fps animation
    val textPaint = remember {
        Paint().apply {
            color = Color.White.toArgb()
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(5f, 2f, 2f, Color.Black.toArgb())
        }
    }

    LaunchedEffect(isSpinning, targetAngle) {
        if (isSpinning && targetAngle > 0f) {
            val count = if (players.isNotEmpty()) players.size else 4
            val sweep = 360f / count
            var lastPegIndex = (rotation.value / sweep).toInt()

            rotation.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(
                    durationMillis = 4400,
                    easing = CubicBezierEasing(0.08f, 0.82f, 0.18f, 1.0f)
                )
            ) {
                val currentPegIndex = (value / sweep).toInt()
                if (currentPegIndex != lastPegIndex) {
                    lastPegIndex = currentPegIndex
                    needleAngle = -18f
                    onTick()
                } else if (needleAngle != 0f) {
                    needleAngle *= 0.7f
                    if (Math.abs(needleAngle) < 0.5f) needleAngle = 0f
                }
            }
            needleAngle = 0f
            onSpinFinished()
        }
    }

    val animatedNeedleAngle by animateFloatAsState(
        targetValue = needleAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "needle_spring"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2f - 16f
            val center = Offset(size.width / 2f, size.height / 2f)

            val count = if (players.isNotEmpty()) players.size else 4
            val sweepAngle = 360f / count

            // Outer metallic rim
            drawCircle(
                color = BuzzerGoldBright,
                radius = radius + 8f,
                center = center,
                style = Stroke(width = 9f)
            )
            drawCircle(
                color = GameSurfaceBorder,
                radius = radius + 13f,
                center = center,
                style = Stroke(width = 3f)
            )

            val currentRot = rotation.value

            // Draw sectors
            for (i in 0 until count) {
                val startAngle = (i * sweepAngle) + currentRot
                val sectorColor = PlayerColors.getOrElse(i) { BuzzerGold }

                drawArc(
                    color = sectorColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )

                // Divider line with peg at outer rim
                val rad = Math.toRadians(startAngle.toDouble())
                val endX = center.x + radius * cos(rad).toFloat()
                val endY = center.y + radius * sin(rad).toFloat()
                drawLine(
                    color = GameSurface,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 4f
                )

                // Golden peg on wheel rim
                drawCircle(
                    color = BuzzerGoldBright,
                    radius = 5f,
                    center = Offset(endX, endY)
                )

                // Player name inside sector
                val player = players.getOrNull(i)
                val playerName = player?.name ?: "لاعب ${i + 1}"
                val textRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                val textRadius = radius * 0.65f
                val textX = center.x + textRadius * cos(textRad).toFloat()
                val textY = center.y + textRadius * sin(textRad).toFloat()

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.rotate(
                        (startAngle + sweepAngle / 2f + 90f),
                        textX,
                        textY
                    )
                    canvas.nativeCanvas.drawText(playerName.take(10), textX, textY, textPaint)
                    canvas.nativeCanvas.restore()
                }
            }

            // Center metallic hub
            drawCircle(
                color = BuzzerGold,
                radius = radius * 0.22f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.22f,
                center = center,
                style = Stroke(width = 4f)
            )
            drawCircle(
                color = GameSurface,
                radius = radius * 0.12f,
                center = center
            )
        }

        // Top pointer indicator with physics needle deflection
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Pointer",
            tint = BuzzerGoldBright,
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
                .graphicsLayer {
                    rotationZ = animatedNeedleAngle
                }
        )
    }
}
