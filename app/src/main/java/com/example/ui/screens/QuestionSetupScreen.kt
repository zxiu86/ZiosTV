package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.QuestionDifficulty
import com.example.model.QuestionDuration
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenBright
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun QuestionSetupScreen(
    gameState: GameState,
    isMeQuestioner: Boolean,
    onStartQuestion: (QuestionDifficulty, QuestionDuration) -> Unit,
    onViewScoreboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDifficulty by remember { mutableStateOf(QuestionDifficulty.EASY) }
    var selectedDuration by remember { mutableStateOf(QuestionDuration.TWO_MINUTES) }

    val questioner = gameState.currentQuestioner
    val scrollState = rememberScrollState()

    // Enforce business rule: If Hard is chosen and ONE_MINUTE was selected, change to TWO_MINUTES
    if (selectedDifficulty == QuestionDifficulty.HARD && selectedDuration == QuestionDuration.ONE_MINUTE) {
        selectedDuration = QuestionDuration.TWO_MINUTES
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BuzzerGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = BuzzerGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "مرحلة طرح السؤال",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onViewScoreboard,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BuzzerGold),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(listOf(BuzzerGold.copy(alpha = 0.4f), BuzzerGold))
                )
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("النقاط", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Questioner Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMeQuestioner) BuzzerGold.copy(alpha = 0.14f) else GameSurface
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(
                        if (isMeQuestioner) BuzzerGold else GameSurfaceBorder,
                        Color.Transparent
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isMeQuestioner) "أنت السائل في هذه الجولة!" else "السائل الحالي: ${questioner?.name ?: "اللاعب"}",
                    color = if (isMeQuestioner) BuzzerGoldBright else TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                val answerer = gameState.currentAnswerer ?: gameState.players.find { it.id == gameState.selectedCardPlayerId }
                if (answerer != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlowGreen.copy(alpha = 0.15f))
                            .border(1.dp, GlowGreenBright.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "أول من ينال فرصة الإجابة (المختار بالبطاقة): ${answerer.name}",
                            color = GlowGreenBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                val instructions = if (isMeQuestioner) {
                    "اطرح سؤالك شفهياً، ثم حدد الصعوبة والمدة واضغط على زر البدء لتبدأ فرصة إجابة ${answerer?.name ?: "المجيب"}!"
                } else {
                    if (answerer?.id == gameState.players.find { it.id != gameState.currentQuestionerId }?.id)
                        "أنت المجيب المختار بالبطاقة! استمع للسؤال شفهياً واستعد لبدء مؤقت الإجابة."
                    else
                        "المجيب الأول هو ${answerer?.name ?: "المنافس"}. إذا أخطأ أو انتهى الوقت، ستنتقل الفرصة لبقية اللاعبين!"
                }
                Text(
                    text = instructions,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        if (isMeQuestioner) {
            // Difficulty Selector Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GameSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = BuzzerGold, modifier = Modifier.size(18.dp))
                        Text(
                            text = "حدد مستوى الصعوبة",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuestionDifficulty.values().forEach { diff ->
                            val isSelected = selectedDifficulty == diff
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BuzzerGold.copy(alpha = 0.16f) else GameSurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) BuzzerGold else GameSurfaceBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedDifficulty = diff }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("difficulty_${diff.name}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = diff.labelArabic,
                                    color = if (isSelected) BuzzerGoldBright else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "+${diff.points} ${if (diff.points == 1) "نقطة" else "نقاط"}",
                                    color = if (isSelected) BuzzerGold else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Duration Selector Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GameSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = BuzzerGold, modifier = Modifier.size(18.dp))
                        Text(
                            text = "حدد مدة الإجابة",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuestionDuration.values().forEach { dur ->
                            val isAllowed = dur.isAllowedFor(selectedDifficulty)
                            val isSelected = selectedDuration == dur && isAllowed

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (!isAllowed) Color(0xFF101217)
                                        else if (isSelected) GlowGreen.copy(alpha = 0.16f)
                                        else GameSurfaceElevated
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (!isAllowed) GameSurfaceBorder.copy(alpha = 0.3f)
                                        else if (isSelected) GlowGreen
                                        else GameSurfaceBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = isAllowed) {
                                        selectedDuration = dur
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("duration_${dur.name}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dur.labelArabic,
                                    color = if (!isAllowed) TextMuted.copy(alpha = 0.5f)
                                    else if (isSelected) GlowGreen
                                    else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (!isAllowed) {
                                    Text(
                                        text = "ممنوعة في الصعب",
                                        color = GlowRed.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "${dur.totalSeconds} ثانية",
                                        color = if (isSelected) GlowGreen else TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Start Question Button
            Button(
                onClick = { onStartQuestion(selectedDifficulty, selectedDuration) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BuzzerGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_question_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val startBtnText = if (gameState.gameMode == com.example.model.GameMode.DUO)
                    "بدء السؤال وتسليم مهلة الإجابة للمنافس"
                else
                    "بدء السؤال وفتح البوزير للمتنافسين"
                Text(
                    text = startBtnText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Competitors waiting card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GameSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "في انتظار قيام ${questioner?.name ?: "السائل"} بطرح السؤال وبدء المنافسة…",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
