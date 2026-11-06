package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.ui.components.TimerCountdownView
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenBright
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AnsweringScreen(
    gameState: GameState,
    myPlayerId: String,
    onAnswerDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMeAnswerer = gameState.currentAnswererId == myPlayerId
    val isQuestioner = gameState.currentQuestionerId == myPlayerId
    val answerer = gameState.currentAnswerer
    val answererColor = PlayerColors.getOrElse(answerer?.colorIndex ?: 0) { BuzzerGold }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Answerer Pill Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMeAnswerer) GlowGreen.copy(alpha = 0.15f) else GameSurface
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(
                        if (isMeAnswerer) GlowGreen else answererColor,
                        GameSurfaceBorder
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isMeAnswerer) GlowGreen.copy(alpha = 0.25f) else answererColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMeAnswerer) Icons.Default.RecordVoiceOver else Icons.Default.Hearing,
                        contentDescription = null,
                        tint = if (isMeAnswerer) GlowGreenBright else answererColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isMeAnswerer) "دورك في الإجابة الآن!" else "المجيب الحالي: ${answerer?.name ?: "اللاعب"}",
                        color = if (isMeAnswerer) GlowGreenBright else TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isMeAnswerer)
                            "أجب شفهياً على السائل ثم اضغط على زر «تم الجواب» فور انتهائك."
                        else
                            "استمع للإجابة شفهياً، وعند الانتهاء سينتقل التحكيم إلى السائل.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Live Countdown Timer
        TimerCountdownView(
            remainingSeconds = gameState.remainingSeconds,
            totalSeconds = gameState.duration.totalSeconds,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Action Section
        if (isMeAnswerer) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onAnswerDoneClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("answer_done_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "تم الجواب (إيقاف المؤقت)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = "اضغط على هذا الزر فور انتهائك شفهياً لتسليم التحكيم للسائل",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GameSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isQuestioner)
                            "أنت السائل: استمع لإجابة ${answerer?.name ?: "المتسابق"}، ستظهر لك أزرار التحكيم (صحيح/خطأ) فور ضغطه على «تم الجواب» أو انتهاء وقته."
                        else
                            "في انتظار انتهاء ${answerer?.name ?: "المتسابق"} من الإجابة الشفهية…",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
