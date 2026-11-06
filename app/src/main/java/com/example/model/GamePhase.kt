package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase {
    WAITING_FOR_PLAYERS,
    READY,
    QUESTION_SETUP,
    BUZZER_OPEN,
    ANSWERING,
    JUDGING,
    ROUND_SUCCESS,
    ROUND_FAILED,
    SPINNING_WHEEL
}
