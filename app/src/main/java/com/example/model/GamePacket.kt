package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class GamePacket {
    @Serializable
    @SerialName("join_req")
    data class JoinRequest(val playerId: String, val playerName: String) : GamePacket()

    @Serializable
    @SerialName("join_res")
    data class JoinResponse(
        val accepted: Boolean,
        val reason: String? = null,
        val yourPlayerId: String? = null,
        val initialState: GameState? = null
    ) : GamePacket()

    @Serializable
    @SerialName("set_mode")
    data class SetGameMode(val mode: GameMode) : GamePacket()

    @Serializable
    @SerialName("start_q")
    data class StartQuestion(
        val difficulty: QuestionDifficulty,
        val duration: QuestionDuration
    ) : GamePacket()

    @Serializable
    @SerialName("buzzer_press")
    data class BuzzerPress(
        val playerId: String,
        val clientTimestampNanos: Long
    ) : GamePacket()

    @Serializable
    @SerialName("answer_done")
    data class AnswerDone(val playerId: String) : GamePacket()

    @Serializable
    @SerialName("judge_verdict")
    data class JudgeVerdict(val isCorrect: Boolean) : GamePacket()

    @Serializable
    @SerialName("spin_wheel")
    class SpinWheel : GamePacket()

    @Serializable
    @SerialName("wheel_done")
    class WheelDone : GamePacket()

    @Serializable
    @SerialName("state_sync")
    data class StateSync(val state: GameState) : GamePacket()

    @Serializable
    @SerialName("ping")
    data class Ping(val playerId: String) : GamePacket()

    @Serializable
    @SerialName("pong")
    class Pong : GamePacket()
}
