package com.example.advancedaudiorecorder.model

data class Track(
    val id: Int,
    val samples: MutableList<Sample>,
    val volume: Float,
    val enabled: Boolean
)
