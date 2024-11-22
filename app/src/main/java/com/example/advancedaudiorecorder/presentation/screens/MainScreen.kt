package com.example.advancedaudiorecorder.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.advancedaudiorecorder.audio.AudioEngine
import com.example.advancedaudiorecorder.presentation.components.BottomBar
import com.example.advancedaudiorecorder.presentation.components.Playlist
import com.example.advancedaudiorecorder.presentation.components.TopBar

@Composable
fun MainScreen(
    modifier: Modifier,
    onSwitchRecording: () -> Unit,
    onSwitchPlaying: () -> Unit,
    onSwitchMetronome: () -> Unit,
    isRecording: Boolean,
    isPlaying: Boolean,
    isMetronomeEnabled: Boolean,
    audioEngine: AudioEngine?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar()
        Box(modifier = Modifier.weight(1f)) {
            Playlist(audioEngine)
        }
        BottomBar(
            onSwitchRecording = onSwitchRecording,
            onSwitchPlaying = onSwitchPlaying,
            onSwitchMetronome = onSwitchMetronome,
            isRecording = isRecording,
            isPlaying = isPlaying,
            isMetronomeEnabled = isMetronomeEnabled
        )
    }
}