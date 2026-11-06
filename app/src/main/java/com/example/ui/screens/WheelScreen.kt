package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.CircularWheelView
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WheelScreen(
    gameState: GameState,
    isHost: Boolean,
    onSpinClick: () -> Unit,
    onWheelFinished: () -> Unit,
    onTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasWheelLanded by remember { mutableStateOf(false) }
    val chosenPlayer = gameState.players.find { it.id == gameState.wheelTargetPlayerId }
    val isSpinning = gameState.wheelTargetAngle > 0f && !hasWheelLanded

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Title Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    tint = BuzzerGold,
                    modifier = Modifier.size(24.dp)
                )
                val title = if (gameState.gameMode == com.example.model.GameMode.DUO)
                    "تبادل أدوار السائل"
                else
                    "عجلة اختيار السائل"

                Text(
                    text = title,
                    color = BuzzerGoldBright,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            val subtitle = when (gameState.gameMode) {
                com.example.model.GameMode.DUO -> "النظام الثنائي: كل شخص مرة يسأل ثم مرة يجاوب بالتناوب التام"
                com.example.model.GameMode.TRIO -> "تدور العجلة عشوائياً بين اللاعبين الثلاثة لتحديد السائل القادم"
                com.example.model.GameMode.SQUAD -> "تدور العجلة عشوائياً بين اللاعبين الأربعة لتحديد السائل القادم"
            }
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Circular Wheel Component
        CircularWheelView(
            players = gameState.players,
            targetAngle = gameState.wheelTargetAngle,
            isSpinning = gameState.wheelTargetAngle > 0f,
            onSpinFinished = {
                hasWheelLanded = true
                onWheelFinished()
            },
            onTick = onTick,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Result / Trigger Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (hasWheelLanded && chosenPlayer != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GlowGreen.copy(alpha = 0.18f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(GlowGreen, GlowGreenBright)
                        )
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("wheel_result_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "السائل الجديد هو:",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = chosenPlayer.name,
                            color = GlowGreenBright,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "جارٍ الانتقال لشاشة إعداد السؤال…",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else if (gameState.wheelTargetAngle == 0f) {
                // If not spinning yet
                Button(
                    onClick = onSpinClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuzzerGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("spin_wheel_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تدوير العجلة لاختيار السائل",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "العجلة تدور الآن…",
                    color = BuzzerGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}
