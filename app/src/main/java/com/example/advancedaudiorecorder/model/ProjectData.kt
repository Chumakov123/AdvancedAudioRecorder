package com.example.advancedaudiorecorder.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectData(
    val name: String,
    val isMetronomeEnabled: Boolean,
    val bpm: Int,
    val tracks: List<TrackData>
)