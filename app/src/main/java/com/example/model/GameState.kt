package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val roomCode: String = "",
    val hostIp: String = "",
    val gameMode: GameMode = GameMode.SQUAD,
    val phase: GamePhase = GamePhase.WAITING_FOR_PLAYERS,
    val players: List<Player> = emptyList(),
    val currentQuestionerId: String? = null,
    val currentAnswererId: String? = null,
    val disqualifiedIds: Set<String> = emptySet(),
    val difficulty: QuestionDifficulty = QuestionDifficulty.EASY,
    val duration: QuestionDuration = QuestionDuration.TWO_MINUTES,
    val remainingSeconds: Int = 120,
    val buzzerWinnerId: String? = null,
    val lastRoundWinnerName: String? = null,
    val lastRoundPoints: Int = 0,
    val lastRoundSummary: String = "",
    val wheelTargetPlayerId: String? = null,
    val wheelTargetAngle: Float = 0f,
    // Cards Selection system
    val cardPlayerIds: List<String> = emptyList(),
    val selectedCardIndex: Int? = null,
    val selectedCardPlayerId: String? = null,
    val cardsFlippedToBack: Boolean = false,
    val stateVersion: Long = 0L
) {
    val isRoomFull: Boolean
        get() = players.size >= gameMode.maxPlayers

    val currentQuestioner: Player?
        get() = players.find { it.id == currentQuestionerId }

    val currentAnswerer: Player?
        get() = players.find { it.id == currentAnswererId }

    fun getCompetitors(): List<Player> {
        return players.filter { it.id != currentQuestionerId }
    }

    fun getRemainingCompetitors(): List<Player> {
        return players.filter { it.id != currentQuestionerId && it.id !in disqualifiedIds && it.isConnected }
    }
}
