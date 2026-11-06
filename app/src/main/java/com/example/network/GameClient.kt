package com.example.network

import android.util.Log
import com.example.model.GamePacket
import com.example.model.GameState
import com.example.model.QuestionDifficulty
import com.example.model.QuestionDuration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ClientConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    REJECTED
}

class GameClient(private val scope: CoroutineScope) {

    private val tag = "GameClient"
    private var client: HttpClient? = null
    private var activeSession: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var pingJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _connectionStatus = MutableStateFlow(ClientConnectionStatus.IDLE)
    val connectionStatus: StateFlow<ClientConnectionStatus> = _connectionStatus.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _rejectionReason = MutableStateFlow<String?>(null)
    val rejectionReason: StateFlow<String?> = _rejectionReason.asStateFlow()

    fun connect(
        hostIp: String,
        hostPort: Int = 8080,
        playerId: String,
        playerName: String
    ) {
        disconnect()

        _connectionStatus.value = ClientConnectionStatus.CONNECTING
        _rejectionReason.value = null

        client = HttpClient(CIO) {
            install(WebSockets) {
                pingIntervalMillis = 15_000
            }
        }

        connectionJob = scope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "Connecting to ws://$hostIp:$hostPort/game as $playerName...")
                client?.webSocket(host = hostIp, port = hostPort, path = "/game") {
                    activeSession = this
                    _connectionStatus.value = ClientConnectionStatus.CONNECTED

                    // Send Join Request immediately
                    val joinPacket = GamePacket.JoinRequest(playerId, playerName)
                    send(Frame.Text(json.encodeToString<GamePacket>(joinPacket)))

                    // Start background ping
                    startPing(playerId)

                    // Listen for incoming server packets
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val raw = frame.readText()
                            handleServerMessage(raw)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Connection failed or interrupted: ${e.message}")
                if (_connectionStatus.value != ClientConnectionStatus.REJECTED) {
                    _connectionStatus.value = ClientConnectionStatus.DISCONNECTED
                }
            } finally {
                activeSession = null
                pingJob?.cancel()
                if (_connectionStatus.value != ClientConnectionStatus.REJECTED) {
                    _connectionStatus.value = ClientConnectionStatus.DISCONNECTED
                }
            }
        }
    }

    private fun handleServerMessage(raw: String) {
        try {
            val packet = json.decodeFromString<GamePacket>(raw)
            when (packet) {
                is GamePacket.JoinResponse -> {
                    if (packet.accepted) {
                        _connectionStatus.value = ClientConnectionStatus.CONNECTED
                        packet.initialState?.let { _gameState.value = it }
                    } else {
                        _connectionStatus.value = ClientConnectionStatus.REJECTED
                        _rejectionReason.value = packet.reason ?: "تم رفض الانضمام للغرفة"
                        disconnect()
                    }
                }

                is GamePacket.StateSync -> {
                    _gameState.value = packet.state
                }

                is GamePacket.Pong -> {
                    // Pong received
                }

                else -> {}
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing server message: ${e.message}")
        }
    }

    private fun startPing(playerId: String) {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(10_000)
                sendPacket(GamePacket.Ping(playerId))
            }
        }
    }

    fun sendBuzzerPress(playerId: String) {
        val nanos = System.nanoTime()
        sendPacket(GamePacket.BuzzerPress(playerId, nanos))
    }

    fun sendAnswerDone(playerId: String) {
        sendPacket(GamePacket.AnswerDone(playerId))
    }

    fun sendSetGameMode(mode: com.example.model.GameMode) {
        sendPacket(GamePacket.SetGameMode(mode))
    }

    fun sendStartQuestion(difficulty: QuestionDifficulty, duration: QuestionDuration) {
        sendPacket(GamePacket.StartQuestion(difficulty, duration))
    }

    fun sendJudgeVerdict(isCorrect: Boolean) {
        sendPacket(GamePacket.JudgeVerdict(isCorrect))
    }

    fun sendSpinWheel() {
        sendPacket(GamePacket.SpinWheel())
    }

    fun sendWheelDone() {
        sendPacket(GamePacket.WheelDone())
    }

    private fun sendPacket(packet: GamePacket) {
        scope.launch(Dispatchers.IO) {
            try {
                val session = activeSession
                if (session != null && session.isActive) {
                    val text = json.encodeToString<GamePacket>(packet)
                    session.send(Frame.Text(text))
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to send packet: ${e.message}")
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        connectionJob?.cancel()
        connectionJob = null
        scope.launch(Dispatchers.IO) {
            try {
                activeSession?.close()
            } catch (_: Exception) {}
            activeSession = null
            try {
                client?.close()
            } catch (_: Exception) {}
            client = null
        }
        _connectionStatus.value = ClientConnectionStatus.IDLE
    }
}
