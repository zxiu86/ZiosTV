package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ThumbUp
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
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.ui.components.ScoreboardView
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenBright
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RoundResultScreen(
    gameState: GameState,
    myPlayerId: String,
    onNavigateToWheel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSuccess = gameState.phase == GamePhase.ROUND_SUCCESS
    val winnerName = gameState.lastRoundWinnerName ?: "الفائز"
    val points = gameState.lastRoundPoints
    val bannerColor = if (isSuccess) GlowGreen else BuzzerGold

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Result Announcement Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.15f)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(bannerColor, bannerColor.copy(alpha = 0.4f)))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(bannerColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.EmojiEvents else Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = if (isSuccess) GlowGreenBright else BuzzerGoldBright,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (isSuccess) "إجابة صحيحة!" else "انتهت الجولة!",
                    color = if (isSuccess) GlowGreenBright else BuzzerGoldBright,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = gameState.lastRoundSummary.ifBlank {
                        if (isSuccess)
                            "أجاب $winnerName إجابة صحيحة وحصل على $points ${if (points == 1) "نقطة" else "نقاط"}!"
                        else
                            "فشل جميع المتنافسين، ونقاط السؤال تذهب للسائل ($winnerName)!"
                    },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Live Scoreboard
        ScoreboardView(
            players = gameState.players,
            currentQuestionerId = gameState.currentQuestionerId,
            currentAnswererId = gameState.currentAnswererId,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Action Button: Go to Wheel
        Button(
            onClick = onNavigateToWheel,
            colors = ButtonDefaults.buttonColors(
                containerColor = BuzzerGold,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("goto_wheel_button")
        ) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "الانتقال إلى عجلة اختيار السائل التالي",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
