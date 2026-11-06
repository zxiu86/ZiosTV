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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.ui.components.BuzzerButton
import com.example.ui.components.BuzzerState
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BuzzerCompetitionScreen(
    gameState: GameState,
    myPlayerId: String,
    onBuzzerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isQuestioner = gameState.currentQuestionerId == myPlayerId
    val isDisqualified = myPlayerId in gameState.disqualifiedIds
    val questioner = gameState.currentQuestioner

    val buzzerState = when {
        isDisqualified -> BuzzerState.DISQUALIFIED
        gameState.buzzerWinnerId == myPlayerId -> BuzzerState.WON_BY_ME
        gameState.buzzerWinnerId != null -> BuzzerState.LOCKED_BY_OTHER
        else -> BuzzerState.IDLE_OPEN
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Question Info Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = BuzzerGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "السائل: ${questioner?.name ?: "اللاعب"}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = gameState.difficulty.labelArabic,
                            color = BuzzerGold,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GameSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = gameState.duration.labelArabic,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Center Content: Buzzer for Competitors OR Status for Questioner
        if (isQuestioner) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GameSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = BuzzerGold,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "البوزير مفتوح الآن للمتنافسين الثلاثة!",
                        color = BuzzerGoldBright,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "أول متسابق يضغط على زر البوزير سيحصل تلقائياً على حق الإجابة، وسيبدأ مؤقت السؤال على جهازه فوراً.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                BuzzerButton(
                    buzzerState = buzzerState,
                    onBuzzerClick = onBuzzerClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isDisqualified)
                        "تم استبعادك من المحاولات لهذا السؤال"
                    else
                        "كن الأسرع في الضغط على البوزير للحصول على فرصة الإجابة!",
                    color = if (isDisqualified) TextMuted else TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Footer Note
        Text(
            text = "تحديد الفائز يتم بدقة النانوثانية على جهاز المضيف لضمان العدالة التامة",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}
