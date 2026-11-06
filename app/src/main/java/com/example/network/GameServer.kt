package com.example.network

import android.util.Log
import com.example.engine.GameEngine
import com.example.model.GamePacket
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class GameServer(
    private val scope: CoroutineScope,
    val engine: GameEngine,
    val port: Int = 8080
) {
    private val tag = "GameServer"
    private var server: EmbeddedServer<*, *>? = null
    private val sessions = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private var stateBroadcastJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun start() {
        if (server != null) return

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(WebSockets) {
                pingPeriodMillis = 15_000
                timeoutMillis = 30_000
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/game") {
                    var boundPlayerId: String? = null
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                handleClientMessage(this, text) { playerId ->
                                    boundPlayerId = playerId
                                    sessions[playerId] = this
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Client session ended: ${e.message}")
                    } finally {
                        boundPlayerId?.let { pid ->
                            sessions.remove(pid)
                            scope.launch {
                                this@GameServer.engine.onPlayerDisconnected(pid)
                            }
                        }
                    }
                }
            }
        }.start(wait = false)

        Log.d(tag, "Ktor server started on port $port")

        // Broadcast state changes whenever engine state changes
        stateBroadcastJob = scope.launch(Dispatchers.IO) {
            this@GameServer.engine.state.collectLatest { gameState ->
                broadcastState(gameState)
            }
        }
    }

    private suspend fun handleClientMessage(
        session: DefaultWebSocketSession,
        rawText: String,
        onPlayerIdentified: (String) -> Unit
    ) {
        try {
            val packet = json.decodeFromString<GamePacket>(rawText)
            when (packet) {
                is GamePacket.JoinRequest -> {
                    val result = this@GameServer.engine.addPlayer(packet.playerId, packet.playerName, isHost = false)
                    val response = GamePacket.JoinResponse(
                        accepted = result.first,
                        reason = result.second,
                        yourPlayerId = packet.playerId,
                        initialState = this@GameServer.engine.state.value
                    )
                    session.send(Frame.Text(json.encodeToString<GamePacket>(response)))
                    if (result.first) {
                        onPlayerIdentified(packet.playerId)
                    }
                }

                is GamePacket.StartQuestion -> {
                    this@GameServer.engine.startQuestion(packet.difficulty, packet.duration)
                }

                is GamePacket.BuzzerPress -> {
                    this@GameServer.engine.onBuzzerPressed(packet.playerId)
                }

                is GamePacket.AnswerDone -> {
                    this@GameServer.engine.onAnswerDone(packet.playerId)
                }

                is GamePacket.JudgeVerdict -> {
                    this@GameServer.engine.onJudgeVerdict(packet.isCorrect)
                }

                is GamePacket.SpinWheel -> {
                    this@GameServer.engine.startWheelSpin()
                }

                is GamePacket.WheelDone -> {
                    this@GameServer.engine.completeWheelSpin()
                }

                is GamePacket.Ping -> {
                    session.send(Frame.Text(json.encodeToString<GamePacket>(GamePacket.Pong())))
                }

                else -> {}
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling client packet: ${e.message}")
        }
    }

    private suspend fun broadcastState(gameState: com.example.model.GameState) {
        val packet = GamePacket.StateSync(gameState)
        val text = json.encodeToString<GamePacket>(packet)
        for ((_, session) in sessions) {
            try {
                if (session.isActive) {
                    session.send(Frame.Text(text))
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed sending state broadcast to client: ${e.message}")
            }
        }
    }

    fun stop() {
        stateBroadcastJob?.cancel()
        stateBroadcastJob = null
        try {
            server?.stop(1000, 2000)
        } catch (_: Exception) {}
        server = null
        sessions.clear()
        Log.d(tag, "Ktor server stopped")
    }
}
