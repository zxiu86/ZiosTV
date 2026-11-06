package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.BuzzerGoldDark
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
import kotlinx.coroutines.delay

@Composable
fun CardSelectionScreen(
    gameState: GameState,
    myPlayerId: String,
    isHost: Boolean,
    onSelectCard: (Int) -> Unit,
    onCardsFlipped: () -> Unit,
    onProceedToQuestion: () -> Unit,
    onPlayFlipSound: () -> Unit,
    onPlaySelectedSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isQuestioner = gameState.currentQuestionerId == myPlayerId
    val questioner = gameState.currentQuestioner
    val candidateIds = gameState.cardPlayerIds
    val candidatePlayers = candidateIds.mapNotNull { id -> gameState.players.find { it.id == id } }

    var isFlippedToBack by remember { mutableStateOf(gameState.cardsFlippedToBack) }
    val isRevealed = gameState.selectedCardIndex != null
    val chosenPlayer = gameState.players.find { it.id == gameState.selectedCardPlayerId }

    // Initial sequence: Display face-up for 1.8 seconds, then flip to back
    LaunchedEffect(candidateIds) {
        if (!gameState.cardsFlippedToBack) {
            delay(1800)
            isFlippedToBack = true
            onPlayFlipSound()
            onCardsFlipped()
        } else {
            isFlippedToBack = true
        }
    }

    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            onPlaySelectedSound()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "اختيار أول من يجاوب",
                color = BuzzerGoldBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            val subtitleText = when {
                isRevealed && chosenPlayer != null -> "تم الاختيار! أول من يجاوب هو: ${chosenPlayer.name}"
                !isFlippedToBack -> "تذكر مواقع اللاعبين قبل أن تدور البطاقات لظهرها!"
                isQuestioner -> "اختر إحدى البطاقات لتحديد أول من ينال فرصة الإجابة!"
                else -> "السائل (${questioner?.name ?: "السائل"}) يختار بطاقة سرية الآن…"
            }
            Text(
                text = subtitleText,
                color = if (isRevealed) GlowGreenBright else TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontWeight = if (isRevealed) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // Cards Container - dynamically sized to fill the screen space beautifully
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val totalCards = candidatePlayers.size
            val availableWidth = maxWidth
            val availableHeight = maxHeight

            if (totalCards <= 3) {
                // Horizontal Row for 1, 2, or 3 cards
                val cardWidth = ((availableWidth - (16.dp * (totalCards - 1))) / totalCards).coerceIn(95.dp, 160.dp)
                val cardHeight = (availableHeight * 0.78f).coerceIn(170.dp, 310.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    candidatePlayers.forEachIndexed { index, player ->
                        val isThisCardSelected = gameState.selectedCardIndex == index
                        val cardFlippedState = when {
                            isThisCardSelected -> false // Revealed back to face up!
                            isRevealed -> true // Other unselected cards remain hidden
                            else -> isFlippedToBack
                        }

                        MysteryCard(
                            player = player,
                            cardIndex = index,
                            isFlippedToBack = cardFlippedState,
                            isSelectable = isFlippedToBack && !isRevealed && isQuestioner,
                            isSelected = isThisCardSelected,
                            onCardClick = { onSelectCard(index) },
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .testTag("mystery_card_$index")
                        )
                    }
                }
            } else {
                // Grid layout for 4+ cards if needed
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val chunked = candidatePlayers.chunked(2)
                    chunked.forEachIndexed { rowIndex, rowPlayers ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowPlayers.forEachIndexed { colIndex, player ->
                                val index = rowIndex * 2 + colIndex
                                val isThisCardSelected = gameState.selectedCardIndex == index
                                val cardFlippedState = when {
                                    isThisCardSelected -> false
                                    isRevealed -> true
                                    else -> isFlippedToBack
                                }

                                MysteryCard(
                                    player = player,
                                    cardIndex = index,
                                    isFlippedToBack = cardFlippedState,
                                    isSelectable = isFlippedToBack && !isRevealed && isQuestioner,
                                    isSelected = isThisCardSelected,
                                    onCardClick = { onSelectCard(index) },
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(180.dp)
                                        .testTag("mystery_card_$index")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = isRevealed && chosenPlayer != null,
                enter = fadeIn()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GlowGreen.copy(alpha = 0.16f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GlowGreen, GlowGreenBright))),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = GlowGreenBright, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "وقع الاختيار على: ${chosenPlayer?.name}",
                            color = GlowGreenBright,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isRevealed && (isHost || isQuestioner)) {
                Button(
                    onClick = onProceedToQuestion,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuzzerGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("proceed_to_question_btn")
                ) {
                    Text(
                        text = "الانتقال إلى تحديد السؤال",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            } else if (!isFlippedToBack) {
                Text(
                    text = "جارٍ تجهيز البطاقات وقلبها…",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            } else if (isQuestioner && !isRevealed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = BuzzerGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "المس أي بطاقة أعلاه لتحديد اللاعب",
                        color = BuzzerGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (!isRevealed) {
                Text(
                    text = "في انتظار اختيار السائل للبطاقة…",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun MysteryCard(
    player: Player,
    cardIndex: Int,
    isFlippedToBack: Boolean,
    isSelectable: Boolean,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerColor = PlayerColors.getOrElse(player.colorIndex) { BuzzerGold }

    // 3D rotation animation
    val animatedRotationY by animateFloatAsState(
        targetValue = if (isFlippedToBack) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "card_flip_$cardIndex"
    )

    val isBackShowing = animatedRotationY > 90f

    val borderColor = when {
        isSelected -> GlowGreenBright
        isSelectable -> BuzzerGoldBright
        else -> GameSurfaceBorder
    }

    val borderWidth = if (isSelected || isSelectable) 3.dp else 1.5.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = animatedRotationY
                cameraDistance = 16f * density
            }
            .shadow(
                elevation = if (isSelected) 20.dp else if (isSelectable) 10.dp else 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = if (isSelected) GlowGreen else BuzzerGold
            )
            .clip(RoundedCornerShape(18.dp))
            .background(GameSurface)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .clickable(
                enabled = isSelectable,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onCardClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isBackShowing) {
            // CARD BACK (Rotated 180deg to display correctly)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E2330),
                                Color(0xFF151822),
                                Color(0xFF0F1219)
                            )
                        )
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Decorative inner golden frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, BuzzerGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BuzzerGold.copy(alpha = 0.15f))
                                .border(1.5.dp, BuzzerGoldBright.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "؟",
                                color = BuzzerGoldBright,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "بطاقة سرية",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // CARD FRONT (Shows Player Identity)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                playerColor.copy(alpha = 0.28f),
                                GameSurfaceElevated,
                                GameSurface
                            )
                        )
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Player Avatar / Badge
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(playerColor.copy(alpha = 0.35f))
                            .border(2.5.dp, playerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = player.name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${player.score} نقطة",
                        color = BuzzerGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
