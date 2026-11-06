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
    RECONNECTING,
    DISCONNECTED,
    REJECTED
}

class GameClient(private val scope: CoroutineScope) {

    private val tag = "GameClient"
    private var client: HttpClient? = null
    private var activeSession: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var pingJob: Job? = null

    private var lastHostIp: String = ""
    private var lastHostPort: Int = 8080
    private var currentPid: String = ""
    private var currentPName: String = ""
    private var shouldAutoReconnect = true

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
        lastHostIp = hostIp.trim()
        lastHostPort = hostPort
        currentPid = playerId
        currentPName = playerName
        shouldAutoReconnect = true

        disconnect(resetAutoReconnect = false)

        _connectionStatus.value = ClientConnectionStatus.CONNECTING
        _rejectionReason.value = null

        client = HttpClient(CIO) {
            install(WebSockets) {
                pingIntervalMillis = 30_000
            }
        }

        connectionJob = scope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "Connecting to ws://$lastHostIp:$lastHostPort/game as $playerName...")
                client?.webSocket(host = lastHostIp, port = lastHostPort, path = "/game") {
                    activeSession = this
                    _connectionStatus.value = ClientConnectionStatus.CONNECTED

                    // Send Join Request immediately
                    val joinPacket = GamePacket.JoinRequest(currentPid, currentPName)
                    send(Frame.Text(json.encodeToString<GamePacket>(joinPacket)))

                    // Start background ping
                    startPing(currentPid)

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
            } finally {
                activeSession = null
                pingJob?.cancel()

                if (_connectionStatus.value != ClientConnectionStatus.REJECTED && shouldAutoReconnect) {
                    attemptReconnect()
                } else if (_connectionStatus.value != ClientConnectionStatus.REJECTED) {
                    _connectionStatus.value = ClientConnectionStatus.DISCONNECTED
                }
            }
        }
    }

    private fun attemptReconnect() {
        if (!shouldAutoReconnect || lastHostIp.isBlank()) {
            _connectionStatus.value = ClientConnectionStatus.DISCONNECTED
            return
        }

        scope.launch(Dispatchers.IO) {
            _connectionStatus.value = ClientConnectionStatus.RECONNECTING
            var attempts = 0
            val maxAttempts = 6
            var connected = false

            while (isActive && attempts < maxAttempts && shouldAutoReconnect && !connected) {
                attempts++
                Log.d(tag, "Reconnection attempt $attempts/$maxAttempts in 2s...")
                delay(2000)
                try {
                    client?.webSocket(host = lastHostIp, port = lastHostPort, path = "/game") {
                        activeSession = this
                        _connectionStatus.value = ClientConnectionStatus.CONNECTED
                        connected = true
                        Log.i(tag, "Reconnected successfully on attempt $attempts!")

                        val joinPacket = GamePacket.JoinRequest(currentPid, currentPName)
                        send(Frame.Text(json.encodeToString<GamePacket>(joinPacket)))
                        startPing(currentPid)

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleServerMessage(frame.readText())
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Reconnect attempt $attempts failed: ${e.message}")
                }
            }

            if (!connected && shouldAutoReconnect) {
                _connectionStatus.value = ClientConnectionStatus.DISCONNECTED
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
                        shouldAutoReconnect = false
                        disconnect(resetAutoReconnect = true)
                    }
                }

                is GamePacket.StateSync -> {
                    _gameState.value = packet.state
                }

                is GamePacket.Pong -> {}

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
                delay(15_000)
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

    fun sendJudgeVerdict(isCorrect: Boolean) {
        sendPacket(GamePacket.JudgeVerdict(isCorrect))
    }

    fun sendStartQuestion(difficulty: QuestionDifficulty, duration: QuestionDuration) {
        sendPacket(GamePacket.StartQuestion(difficulty, duration))
    }

    fun sendSpinWheel() {
        sendPacket(GamePacket.SpinWheel())
    }

    fun sendWheelDone() {
        sendPacket(GamePacket.WheelDone())
    }

    fun sendSelectCard(cardIndex: Int) {
        sendPacket(GamePacket.SelectCard(cardIndex))
    }

    fun sendCardsFlipDone() {
        sendPacket(GamePacket.CardsFlipDone())
    }

    fun sendProceedFromCards() {
        sendPacket(GamePacket.ProceedFromCards())
    }

    private fun sendPacket(packet: GamePacket) {
        scope.launch(Dispatchers.IO) {
            try {
                val text = json.encodeToString<GamePacket>(packet)
                activeSession?.send(Frame.Text(text))
            } catch (e: Exception) {
                Log.w(tag, "Failed to send packet: ${e.message}")
            }
        }
    }

    fun disconnect(resetAutoReconnect: Boolean = true) {
        if (resetAutoReconnect) {
            shouldAutoReconnect = false
        }
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
        if (resetAutoReconnect) {
            _connectionStatus.value = ClientConnectionStatus.IDLE
        }
    }
}
