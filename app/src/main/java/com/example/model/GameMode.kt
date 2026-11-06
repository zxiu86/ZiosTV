package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameMode(val maxPlayers: Int, val labelArabic: String, val descriptionArabic: String) {
    DUO(
        maxPlayers = 2,
        labelArabic = "ثنائي (لاعبان)",
        descriptionArabic = "شخص يسأل وشخص يجاوب بالتناوب التام"
    ),
    TRIO(
        maxPlayers = 3,
        labelArabic = "ثلاثي (3 لاعبين)",
        descriptionArabic = "سائل واحد ومتنافسان على البوزير"
    ),
    SQUAD(
        maxPlayers = 4,
        labelArabic = "رباعي (4 لاعبين)",
        descriptionArabic = "سائل واحد و3 متنافسين على البوزير"
    )
}
