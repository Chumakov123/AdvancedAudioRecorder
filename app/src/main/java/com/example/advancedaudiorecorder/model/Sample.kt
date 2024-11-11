package com.example.advancedaudiorecorder.model

data class Sample(
    val data: ByteArray,
    val startTime: Long,
    val duration: Long
)