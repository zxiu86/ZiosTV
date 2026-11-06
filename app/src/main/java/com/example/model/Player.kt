package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val isHost: Boolean = false,
    val isConnected: Boolean = true,
    val colorIndex: Int = 0
)
