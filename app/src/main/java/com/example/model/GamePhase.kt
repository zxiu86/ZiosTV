package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase {
    WAITING_FOR_PLAYERS,
    READY,
    SPINNING_WHEEL,
    CARD_SELECTION,
    QUESTION_SETUP,
    BUZZER_OPEN,
    ANSWERING,
    JUDGING,
    ROUND_SUCCESS,
    ROUND_FAILED
}
