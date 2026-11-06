package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class QuestionDuration(val totalSeconds: Int, val labelArabic: String) {
    ONE_MINUTE(totalSeconds = 60, labelArabic = "دقيقة واحدة"),
    TWO_MINUTES(totalSeconds = 120, labelArabic = "دقيقتان"),
    THREE_MINUTES(totalSeconds = 180, labelArabic = "3 دقائق"),
    FOUR_MINUTES(totalSeconds = 240, labelArabic = "4 دقائق");

    fun isAllowedFor(difficulty: QuestionDifficulty): Boolean {
        return when (difficulty) {
            QuestionDifficulty.EASY,
            QuestionDifficulty.MEDIUM -> true
            QuestionDifficulty.HARD -> this != ONE_MINUTE
        }
    }
}
