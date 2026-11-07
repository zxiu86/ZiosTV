package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.FeedbackManager
import com.example.audio.FeedbackType
import com.example.engine.GameEngine
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.model.QuestionDifficulty
import com.example.model.QuestionDuration
import com.example.network.ClientConnectionStatus
import com.example.network.DiscoveredRoom
import com.example.network.GameClient
import com.example.network.GameServer
import com.example.network.UdpDiscoveryManager
import com.example.ui.components.GlowType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.update.AppUpdateManager
import com.example.update.UpdateInfo
import com.example.update.UpdateStatus
import java.io.File
import java.util.UUID

enum class AppScreen {
    HOME,
    HOST_LOBBY,
    JOIN,
    GAME_PLAY
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val feedbackManager = FeedbackManager(application.applicationContext)
    private val discoveryManager = UdpDiscoveryManager(application.applicationContext)
    val updateManager = AppUpdateManager(application.applicationContext)
    val updateStatus: StateFlow<UpdateStatus> = updateManager.status

    val myPlayerId: String = UUID.randomUUID().toString()

    private val _playerName = MutableStateFlow("لاعب")
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _hostLocalIp = MutableStateFlow("127.0.0.1")
    val hostLocalIp: StateFlow<String> = _hostLocalIp.asStateFlow()

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    private val _activeGlow = MutableStateFlow<GlowType?>(null)
    val activeGlow: StateFlow<GlowType?> = _activeGlow.asStateFlow()

    private val _selectedGameMode = MutableStateFlow(com.example.model.GameMode.SQUAD)
    val selectedGameMode: StateFlow<com.example.model.GameMode> = _selectedGameMode.asStateFlow()

    private val _isWheelSpinning = MutableStateFlow(false)
    val isWheelSpinning: StateFlow<Boolean> = _isWheelSpinning.asStateFlow()

    private var gameEngine: GameEngine? = null
    private var gameServer: GameServer? = null
    private val gameClient = GameClient(viewModelScope)

    val clientStatus: StateFlow<ClientConnectionStatus> = gameClient.connectionStatus
    val rejectionReason: StateFlow<String?> = gameClient.rejectionReason

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private var lastObservedPhase: GamePhase? = null
    private var lastObservedAnswererId: String? = null

    init {
        // Collect client state updates and trigger feedbacks
        viewModelScope.launch {
            gameClient.gameState.collectLatest { state ->
                if (state != null) {
                    _gameState.value = state
                    handleStateTransitions(state)
                }
            }
        }

        // Automatic background update check from GitHub repository zxiu86/ZiosTV
        viewModelScope.launch {
            updateManager.checkForUpdates(isManualCheck = false)
        }
    }

    private fun handleStateTransitions(state: GameState) {
        val prevPhase = lastObservedPhase
        val prevAnswerer = lastObservedAnswererId

        lastObservedPhase = state.phase
        lastObservedAnswererId = state.currentAnswererId

        when (state.phase) {
            GamePhase.ANSWERING -> {
                if (prevPhase == GamePhase.BUZZER_OPEN) {
                    // Buzzer race just decided
                    if (state.buzzerWinnerId == myPlayerId) {
                        _activeGlow.value = GlowType.WINNER_GREEN
                        feedbackManager.playFeedback(FeedbackType.BUZZER_WINNER, viewModelScope)
                    } else if (state.currentQuestionerId != myPlayerId) {
                        _activeGlow.value = GlowType.LOSER_RED
                        feedbackManager.playFeedback(FeedbackType.BUZZER_LOSER, viewModelScope)
                    } else {
                        // Host / Questioner notification
                        feedbackManager.playFeedback(FeedbackType.BUZZER_HOST_NOTIFY, viewModelScope)
                    }
                } else if (prevPhase == GamePhase.JUDGING || (prevAnswerer != null && prevAnswerer != state.currentAnswererId)) {
                    // Transferred to a new answerer!
                    if (state.currentAnswererId == myPlayerId) {
                        _activeGlow.value = GlowType.WINNER_GREEN
                        feedbackManager.playFeedback(FeedbackType.ANSWER_TRANSFERRED, viewModelScope)
                    } else {
                        feedbackManager.playFeedback(FeedbackType.ANSWER_TRANSFERRED, viewModelScope)
                    }
                }
            }

            GamePhase.ROUND_SUCCESS -> {
                if (prevPhase != GamePhase.ROUND_SUCCESS) {
                    feedbackManager.playFeedback(FeedbackType.CORRECT_ANSWER, viewModelScope)
                }
            }

            GamePhase.ROUND_FAILED -> {
                if (prevPhase != GamePhase.ROUND_FAILED) {
                    feedbackManager.playFeedback(FeedbackType.WRONG_ANSWER, viewModelScope)
                }
            }

            GamePhase.SPINNING_WHEEL -> {
                if (!_isWheelSpinning.value) {
                    _isWheelSpinning.value = true
                }
            }

            else -> {}
        }
    }

    fun setPlayerName(name: String) {
        _playerName.value = name.trim()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectGameMode(mode: com.example.model.GameMode) {
        _selectedGameMode.value = mode
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.setGameMode(mode)
            }
        }
    }

    fun createRoom(hostName: String, mode: com.example.model.GameMode = _selectedGameMode.value) {
        val finalName = hostName.ifBlank { "المضيف" }
        _playerName.value = finalName
        _isHost.value = true
        _selectedGameMode.value = mode

        val ip = UdpDiscoveryManager.getLocalIpAddress()
        _hostLocalIp.value = ip
        val roomCode = (1000..9999).random().toString()

        val engine = GameEngine(viewModelScope, initialRoomCode = roomCode, hostIp = ip, initialGameMode = mode)
        gameEngine = engine

        val server = GameServer(viewModelScope, engine, port = 8080)
        gameServer = server
        server.start()

        // Start UDP broadcast for auto-discovery
        discoveryManager.startBroadcasting(viewModelScope, roomCode, 8080)

        // Connect host as local client
        gameClient.connect("127.0.0.1", 8080, myPlayerId, finalName)

        _currentScreen.value = AppScreen.HOST_LOBBY
    }

    fun startListeningForRooms() {
        _discoveredRooms.value = emptyList()
        discoveryManager.startListening(viewModelScope) { room ->
            val list = _discoveredRooms.value
            if (list.none { it.roomCode == room.roomCode && it.hostIp == room.hostIp }) {
                _discoveredRooms.value = list + room
            }
        }
    }

    fun stopListeningForRooms() {
        discoveryManager.stopListening()
    }

    fun joinRoom(hostIp: String, port: Int = 8080, joinerName: String) {
        val finalName = joinerName.ifBlank { "لاعب" }
        _playerName.value = finalName
        _isHost.value = false

        gameClient.connect(hostIp.trim(), port, myPlayerId, finalName)
        _currentScreen.value = AppScreen.GAME_PLAY
    }

    fun startGameFromLobby() {
        val engine = gameEngine ?: return
        viewModelScope.launch {
            engine.resetSession()
            // Random initial questioner via wheel
            engine.startWheelSpin()
        }
        _currentScreen.value = AppScreen.GAME_PLAY
    }

    fun startQuestion(difficulty: QuestionDifficulty, duration: QuestionDuration) {
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.startQuestion(difficulty, duration)
            }
        } else {
            gameClient.sendStartQuestion(difficulty, duration)
        }
    }

    fun pressBuzzer() {
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.onBuzzerPressed(myPlayerId)
            }
        } else {
            gameClient.sendBuzzerPress(myPlayerId)
        }
    }

    fun answerDone() {
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.onAnswerDone(myPlayerId)
            }
        } else {
            gameClient.sendAnswerDone(myPlayerId)
        }
    }

    fun answerGiveUp() {
        feedbackManager.playFeedback(FeedbackType.WRONG_ANSWER, viewModelScope)
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.onAnswerGiveUp(myPlayerId)
            }
        } else {
            gameClient.sendAnswerGiveUp(myPlayerId)
        }
    }

    fun judgeVerdict(isCorrect: Boolean) {
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.onJudgeVerdict(isCorrect)
            }
        } else {
            gameClient.sendJudgeVerdict(isCorrect)
        }
    }

    fun spinWheel() {
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.startWheelSpin()
            }
        } else {
            gameClient.sendSpinWheel()
        }
    }

    fun onWheelSpinCompleted() {
        _isWheelSpinning.value = false
        feedbackManager.playFeedback(FeedbackType.WHEEL_SELECTED, viewModelScope)
        // Only host initiates the phase transition to CARD_SELECTION, preventing client duplicate race condition
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.completeWheelSpin()
            }
        } else {
            gameClient.sendWheelDone()
        }
    }

    fun onWheelTick() {
        feedbackManager.playFeedback(FeedbackType.WHEEL_TICK, viewModelScope)
    }

    fun selectCard(cardIndex: Int) {
        feedbackManager.playFeedback(FeedbackType.CARD_SELECTED, viewModelScope)
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.onCardSelected(cardIndex, myPlayerId)
            }
        } else {
            gameClient.sendSelectCard(cardIndex, myPlayerId)
        }
    }

    fun notifyCardsFlipped() {
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.onCardsFlippedToBack()
            }
        } else {
            gameClient.sendCardsFlipDone()
        }
    }

    fun proceedFromCards() {
        if (_isHost.value) {
            viewModelScope.launch {
                gameEngine?.proceedFromCards()
            }
        } else {
            gameClient.sendProceedFromCards()
        }
    }

    fun dismissGlow() {
        _activeGlow.value = null
    }

    fun checkForAppUpdates(isManualCheck: Boolean = true) {
        viewModelScope.launch {
            updateManager.checkForUpdates(isManualCheck)
        }
    }

    fun startAppDownload(updateInfo: UpdateInfo) {
        viewModelScope.launch {
            updateManager.startDownload(updateInfo)
        }
    }

    fun cancelAppDownload() {
        updateManager.cancelDownload()
    }

    fun dismissAppUpdate() {
        updateManager.dismissUpdate()
    }

    fun installAppApk(file: File) {
        updateManager.installApk(file)
    }

    fun leaveGame() {
        discoveryManager.stopBroadcasting()
        discoveryManager.stopListening()
        gameClient.disconnect()
        gameServer?.stop()
        gameServer = null
        gameEngine = null
        _gameState.value = null
        _isHost.value = false
        _currentScreen.value = AppScreen.HOME
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopBroadcasting()
        discoveryManager.stopListening()
        gameClient.disconnect()
        gameServer?.stop()
        feedbackManager.release()
    }
}
