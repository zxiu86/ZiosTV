package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class QuestionDifficulty(val points: Int, val labelArabic: String) {
    EASY(points = 1, labelArabic = "سهل (1 نقطة)"),
    MEDIUM(points = 2, labelArabic = "متوسط (2 نقطتان)"),
    HARD(points = 3, labelArabic = "صعب (3 نقاط)")
}
