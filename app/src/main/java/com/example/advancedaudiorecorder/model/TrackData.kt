package com.example.advancedaudiorecorder.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackData(
    val id: Int,
    val isEnabled: Boolean,
    val isLooping: Boolean,
    val volume: Float,
    val pitch: Float,
    val speed: Float
)