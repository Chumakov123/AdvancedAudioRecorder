package com.example.advancedaudiorecorder.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.advancedaudiorecorder.presentation.components.BottomBar
import com.example.advancedaudiorecorder.presentation.components.Playlist
import com.example.advancedaudiorecorder.presentation.components.TopBar

@Composable
fun MainScreen(
    modifier: Modifier,
    onStartMetronome: () -> Unit,
    onStopMetronome: () -> Unit,
    isRecording: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar()
        Box(modifier = Modifier.weight(1f)) {
            Playlist()
        }
        BottomBar(
            onStartMetronome = onStartMetronome,
            onStopMetronome = onStopMetronome,
            isRecording = isRecording
        )
    }
}