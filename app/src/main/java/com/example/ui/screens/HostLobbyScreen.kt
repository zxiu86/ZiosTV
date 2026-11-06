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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HostLobbyScreen(
    gameState: GameState?,
    hostIp: String,
    onStartGameClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val players = gameState?.players ?: emptyList()
    val isFull = players.size >= 4
    val roomCode = gameState?.roomCode ?: "----"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GameSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Text(
                text = "غرفة الانتظار (المضيف)",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Box(modifier = Modifier.size(40.dp))
        }

        // Room Code & Connection Info Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(BuzzerGold.copy(alpha = 0.5f), GameSurfaceBorder))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "رمز الغرفة",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = roomCode,
                    color = BuzzerGoldBright,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    modifier = Modifier.testTag("lobby_room_code")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(GameSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi IP",
                        tint = BuzzerGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "عنوان المضيف (IP): $hostIp",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "يتم بث رمز الغرفة تلقائياً للأجهزة المتصلة بنفس شبكة Wi-Fi",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Joined Players Section (4 slots)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اللاعبون المنضمون",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${players.size} / 4",
                        color = if (isFull) GlowGreen else BuzzerGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                for (slotIndex in 0 until 4) {
                    val player = players.getOrNull(slotIndex)
                    val isSlotFilled = player != null
                    val slotColor = PlayerColors.getOrElse(slotIndex) { BuzzerGold }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSlotFilled) GameSurfaceElevated else Color(0xFF0F1116))
                            .border(
                                width = 1.dp,
                                color = if (isSlotFilled) slotColor.copy(alpha = 0.5f) else GameSurfaceBorder.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isSlotFilled) slotColor else TextMuted.copy(alpha = 0.4f))
                            )
                            Text(
                                text = player?.name ?: "في انتظار انضمام لاعب ${slotIndex + 1}…",
                                color = if (isSlotFilled) TextPrimary else TextMuted,
                                fontSize = 14.sp,
                                fontWeight = if (isSlotFilled) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        if (isSlotFilled) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Joined",
                                tint = GlowGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = BuzzerGold.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartGameClick,
                enabled = isFull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlowGreen,
                    disabledContainerColor = GameSurfaceElevated,
                    contentColor = Color.Black,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_game_button")
            ) {
                Icon(
                    imageVector = if (isFull) Icons.Default.PlayArrow else Icons.Default.HourglassTop,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFull) "بدء اللعبة واختيار أول سائل بالعجلة" else "في انتظار اكتمال 4 لاعبين…",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
