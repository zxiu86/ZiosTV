package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.GameState
import com.example.model.Player
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenBright
import com.example.ui.theme.GlowRed
import com.example.ui.theme.GlowRedBright
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AnsweringScreen(
    gameState: GameState,
    myPlayerId: String,
    onAnswerDoneClick: () -> Unit,
    onAnswerGiveUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMeAnswerer = gameState.currentAnswererId == myPlayerId
    val isQuestioner = gameState.currentQuestionerId == myPlayerId
    val answerer = gameState.currentAnswerer
    val answererColor = PlayerColors.getOrElse(answerer?.colorIndex ?: 0) { GlowGreenBright }

    val remainingSec = gameState.remainingSeconds
    val totalSec = gameState.duration.totalSeconds.coerceAtLeast(1)
    val progress = (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
    val isUrgent = remainingSec in 1..10

    val timerColor by animateColorAsState(
        targetValue = if (isUrgent) GlowRedBright else GlowGreenBright,
        label = "timer_color"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: Question difficulty badge & points
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = BuzzerGoldBright,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "مرحلة الإجابة المباشرة",
                    color = BuzzerGoldBright,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BuzzerGold.copy(alpha = 0.16f))
                    .border(1.dp, BuzzerGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${gameState.difficulty.labelArabic}",
                    color = BuzzerGoldBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2x2 Players Distribution Grid:
        // Top row: Player A (index 0) | Player C (index 2)
        // Bottom row: Player B (index 1) | Player D (index 3)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val players = gameState.players
            val playerA = players.getOrNull(0)
            val playerB = players.getOrNull(1)
            val playerC = players.getOrNull(2)
            val playerD = players.getOrNull(3)

            // Row 1: Player A (left) | Player C (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerGridCard(
                    player = playerA,
                    labelSlot = "لاعب أ",
                    isCurrentAnswerer = playerA?.id == gameState.currentAnswererId,
                    isQuestioner = playerA?.id == gameState.currentQuestionerId,
                    isDisqualified = playerA?.id in gameState.disqualifiedIds,
                    remainingSeconds = remainingSec,
                    progress = progress,
                    timerColor = timerColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )

                PlayerGridCard(
                    player = playerC,
                    labelSlot = "لاعب ج",
                    isCurrentAnswerer = playerC?.id == gameState.currentAnswererId,
                    isQuestioner = playerC?.id == gameState.currentQuestionerId,
                    isDisqualified = playerC?.id in gameState.disqualifiedIds,
                    remainingSeconds = remainingSec,
                    progress = progress,
                    timerColor = timerColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }

            // Row 2: Player B (left) | Player D (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerGridCard(
                    player = playerB,
                    labelSlot = "لاعب ب",
                    isCurrentAnswerer = playerB?.id == gameState.currentAnswererId,
                    isQuestioner = playerB?.id == gameState.currentQuestionerId,
                    isDisqualified = playerB?.id in gameState.disqualifiedIds,
                    remainingSeconds = remainingSec,
                    progress = progress,
                    timerColor = timerColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )

                PlayerGridCard(
                    player = playerD,
                    labelSlot = "لاعب د",
                    isCurrentAnswerer = playerD?.id == gameState.currentAnswererId,
                    isQuestioner = playerD?.id == gameState.currentQuestionerId,
                    isDisqualified = playerD?.id in gameState.disqualifiedIds,
                    remainingSeconds = remainingSec,
                    progress = progress,
                    timerColor = timerColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        }

        // Action Buttons Section
        if (isMeAnswerer) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: تم الجواب (Done answering)
                    Button(
                        onClick = onAnswerDoneClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("answer_done_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تم الجواب",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Button 2: لا أعلم (Give up / transfer answer)
                    Button(
                        onClick = onAnswerGiveUpClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("answer_giveup_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "لا أعلم (تمرير)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "إذا لم تعرف الإجابة، اضغط على «لا أعلم» لتمرير السؤال فوراً للمنافس التالي",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GameSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isQuestioner) Icons.Default.Hearing else Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = if (isQuestioner) BuzzerGold else GlowGreenBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isQuestioner)
                            "أنت السائل: استمع لإجابة ${answerer?.name ?: "المجيب"} شفهياً، ثم ستظهر لك أزرار التحكيم."
                        else
                            "المجيب الحالي هو ${answerer?.name ?: "المنافس"}. إذا قال «لا أعلم» أو انتهى وقته، ستنتقل الفرصة لمن يليه!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerGridCard(
    player: Player?,
    labelSlot: String,
    isCurrentAnswerer: Boolean,
    isQuestioner: Boolean,
    isDisqualified: Boolean,
    remainingSeconds: Int,
    progress: Float,
    timerColor: Color,
    modifier: Modifier = Modifier
) {
    if (player == null) {
        // Empty slot placeholder
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GameSurface.copy(alpha = 0.4f))
                .border(1.dp, GameSurfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "شاغر",
                color = TextMuted.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
        return
    }

    val playerColor = PlayerColors.getOrElse(player.colorIndex) { BuzzerGold }

    val infiniteTransition = rememberInfiniteTransition(label = "answerer_pulse")
    val pulseAlpha = if (isCurrentAnswerer) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        ).value
    } else 1.0f

    val borderColor = when {
        isCurrentAnswerer -> GlowGreenBright.copy(alpha = pulseAlpha)
        isQuestioner -> BuzzerGoldBright
        isDisqualified -> GlowRed.copy(alpha = 0.45f)
        else -> GameSurfaceBorder
    }

    val borderWidth = if (isCurrentAnswerer || isQuestioner) 2.5.dp else 1.dp

    val containerBg = when {
        isCurrentAnswerer -> GlowGreen.copy(alpha = 0.12f)
        isQuestioner -> BuzzerGold.copy(alpha = 0.08f)
        isDisqualified -> Color(0xFF1E1315)
        else -> GameSurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(borderColor, borderColor.copy(alpha = 0.3f)))
        ),
        modifier = modifier
            .shadow(
                elevation = if (isCurrentAnswerer) 12.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isCurrentAnswerer) GlowGreen else Color.Black
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Status badge & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role tag
                val tagText = when {
                    isCurrentAnswerer -> "يجيب الآن"
                    isQuestioner -> "السائل"
                    isDisqualified -> "استُبعد"
                    else -> "ينتظر"
                }
                val tagColor = when {
                    isCurrentAnswerer -> GlowGreenBright
                    isQuestioner -> BuzzerGoldBright
                    isDisqualified -> GlowRedBright
                    else -> TextMuted
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagColor.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tagText,
                        color = tagColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${player.score} ن",
                    color = BuzzerGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Center: Avatar & Player Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(playerColor.copy(alpha = 0.25f))
                        .border(1.5.dp, playerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = player.name,
                    color = if (isDisqualified) TextMuted else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // Bottom: Remaining Time & Mini Progress Bar for Answerer
            if (isCurrentAnswerer) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المهلة",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                        val min = remainingSeconds / 60
                        val sec = remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", min, sec),
                            color = timerColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = timerColor,
                        trackColor = Color(0xFF222834)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
