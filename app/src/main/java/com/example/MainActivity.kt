package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.GamePhase
import com.example.ui.AppScreen
import com.example.audio.FeedbackType
import com.example.ui.GameViewModel
import com.example.ui.components.EdgeGlowEffect
import com.example.ui.components.ScoreboardView
import com.example.ui.screens.AnsweringScreen
import com.example.ui.screens.BuzzerCompetitionScreen
import com.example.ui.screens.CardSelectionScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HostLobbyScreen
import com.example.ui.screens.JoinScreen
import com.example.ui.screens.JudgingScreen
import com.example.ui.screens.QuestionSetupScreen
import com.example.ui.screens.RoundResultScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.WheelScreen
import com.example.ui.theme.GameBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SoualJawabApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoualJawabApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val playerName by viewModel.playerName.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    val hostIp by viewModel.hostLocalIp.collectAsState()
    val discoveredRooms by viewModel.discoveredRooms.collectAsState()
    val connectionStatus by viewModel.clientStatus.collectAsState()
    val rejectionReason by viewModel.rejectionReason.collectAsState()
    val activeGlow by viewModel.activeGlow.collectAsState()
    val selectedGameMode by viewModel.selectedGameMode.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showScoreboardSheet by remember { mutableStateOf(false) }

    // Handle back button per screen
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.HOST_LOBBY -> viewModel.leaveGame()
            AppScreen.JOIN -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.GAME_PLAY -> viewModel.leaveGame()
            else -> {}
        }
    }

    Scaffold(
        containerColor = GameBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        playerName = playerName,
                        selectedMode = selectedGameMode,
                        onPlayerNameChange = { viewModel.setPlayerName(it) },
                        onModeSelected = { viewModel.selectGameMode(it) },
                        onCreateRoomClick = { viewModel.createRoom(playerName, selectedGameMode) },
                        onJoinRoomClick = { viewModel.navigateTo(AppScreen.JOIN) },
                        onSettingsClick = { showSettingsDialog = true }
                    )
                }

                AppScreen.HOST_LOBBY -> {
                    HostLobbyScreen(
                        gameState = gameState,
                        hostIp = hostIp,
                        onStartGameClick = { viewModel.startGameFromLobby() },
                        onBackClick = { viewModel.leaveGame() }
                    )
                }

                AppScreen.JOIN -> {
                    JoinScreen(
                        discoveredRooms = discoveredRooms,
                        connectionStatus = connectionStatus,
                        rejectionReason = rejectionReason,
                        playerName = playerName,
                        onStartScan = { viewModel.startListeningForRooms() },
                        onStopScan = { viewModel.stopListeningForRooms() },
                        onJoinRoom = { ip, port -> viewModel.joinRoom(ip, port, playerName) },
                        onBackClick = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }

                AppScreen.GAME_PLAY -> {
                    val state = gameState
                    if (state == null || state.phase == GamePhase.WAITING_FOR_PLAYERS || state.phase == GamePhase.READY) {
                        // Lobby or waiting phase on client
                        HostLobbyScreen(
                            gameState = state,
                            hostIp = hostIp,
                            onStartGameClick = { viewModel.startGameFromLobby() },
                            onBackClick = { viewModel.leaveGame() }
                        )
                    } else {
                        when (state.phase) {
                            GamePhase.QUESTION_SETUP -> {
                                QuestionSetupScreen(
                                    gameState = state,
                                    isMeQuestioner = state.currentQuestionerId == viewModel.myPlayerId,
                                    onStartQuestion = { diff, dur -> viewModel.startQuestion(diff, dur) },
                                    onViewScoreboard = { showScoreboardSheet = true }
                                )
                            }

                            GamePhase.BUZZER_OPEN -> {
                                BuzzerCompetitionScreen(
                                    gameState = state,
                                    myPlayerId = viewModel.myPlayerId,
                                    onBuzzerClick = { viewModel.pressBuzzer() }
                                )
                            }

                            GamePhase.ANSWERING -> {
                                AnsweringScreen(
                                    gameState = state,
                                    myPlayerId = viewModel.myPlayerId,
                                    onAnswerDoneClick = { viewModel.answerDone() }
                                )
                            }

                            GamePhase.JUDGING -> {
                                JudgingScreen(
                                    gameState = state,
                                    myPlayerId = viewModel.myPlayerId,
                                    onVerdictClick = { isCorrect -> viewModel.judgeVerdict(isCorrect) }
                                )
                            }

                            GamePhase.ROUND_SUCCESS,
                            GamePhase.ROUND_FAILED -> {
                                RoundResultScreen(
                                    gameState = state,
                                    myPlayerId = viewModel.myPlayerId,
                                    onNavigateToWheel = { viewModel.spinWheel() }
                                )
                            }

                            GamePhase.SPINNING_WHEEL -> {
                                WheelScreen(
                                    gameState = state,
                                    isHost = isHost,
                                    onSpinClick = { viewModel.spinWheel() },
                                    onWheelFinished = { viewModel.onWheelSpinCompleted() },
                                    onTick = { viewModel.onWheelTick() }
                                )
                            }

                            GamePhase.CARD_SELECTION -> {
                                CardSelectionScreen(
                                    gameState = state,
                                    myPlayerId = viewModel.myPlayerId,
                                    isHost = isHost,
                                    onSelectCard = { cardIdx -> viewModel.selectCard(cardIdx) },
                                    onCardsFlipped = { viewModel.notifyCardsFlipped() },
                                    onProceedToQuestion = { viewModel.proceedFromCards() },
                                    onPlayFlipSound = { viewModel.feedbackManager.playFeedback(FeedbackType.CARD_FLIP) },
                                    onPlaySelectedSound = { viewModel.feedbackManager.playFeedback(FeedbackType.CARD_SELECTED) }
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }

            // Screen Edge Glow Effect (Green for winner, Red for losers)
            EdgeGlowEffect(
                glowType = activeGlow,
                onFinish = { viewModel.dismissGlow() }
            )

            // Settings Dialog
            if (showSettingsDialog) {
                SettingsDialog(
                    feedbackManager = viewModel.feedbackManager,
                    onDismiss = { showSettingsDialog = false }
                )
            }

            // Scoreboard Bottom Sheet
            if (showScoreboardSheet && gameState != null) {
                ModalBottomSheet(
                    onDismissRequest = { showScoreboardSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = GameBackground
                ) {
                    ScoreboardView(
                        players = gameState?.players ?: emptyList(),
                        currentQuestionerId = gameState?.currentQuestionerId,
                        currentAnswererId = gameState?.currentAnswererId,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
