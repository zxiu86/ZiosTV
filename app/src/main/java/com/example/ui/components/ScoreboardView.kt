package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ScoreboardView(
    players: List<Player>,
    currentQuestionerId: String? = null,
    currentAnswererId: String? = null,
    modifier: Modifier = Modifier
) {
    val sorted = players.sortedByDescending { it.score }
    val rankTitles = listOf("الأول", "الثاني", "الثالث", "الرابع")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GameSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(com.example.ui.theme.GameSurfaceBorder, Color.Transparent)
        )),
        modifier = modifier
            .fillMaxWidth()
            .testTag("scoreboard_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Scoreboard",
                    tint = BuzzerGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "لوحة الترتيب والنقاط",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sorted.forEachIndexed { index, player ->
                    val isQuestioner = player.id == currentQuestionerId
                    val isAnswerer = player.id == currentAnswererId
                    val playerColor = PlayerColors.getOrElse(player.colorIndex) { BuzzerGold }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAnswerer) BuzzerGold.copy(alpha = 0.12f) else Color(0xFF11141A))
                            .border(
                                width = 1.dp,
                                color = if (isAnswerer) BuzzerGold else if (isQuestioner) playerColor.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("player_row_${player.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Rank badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0) BuzzerGold else Color(0xFF222834)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (index == 0) Color.Black else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Player color dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(playerColor)
                            )

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = player.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isQuestioner) {
                                        Text(
                                            text = "(السائل)",
                                            color = BuzzerGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (isAnswerer) {
                                        Text(
                                            text = "(المجيب)",
                                            color = com.example.ui.theme.GlowGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (player.isConnected) StatusConnected else StatusDisconnected)
                                    )
                                    Text(
                                        text = if (player.isConnected) "متصل" else "منقطع",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Score
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${player.score}",
                                color = BuzzerGold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "نقطة",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
