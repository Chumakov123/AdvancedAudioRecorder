package com.example.advancedaudiorecorder.presentation.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.advancedaudiorecorder.audio.AudioEngine
import com.example.advancedaudiorecorder.presentation.components.BottomBar
import com.example.advancedaudiorecorder.presentation.components.Playlist
import com.example.advancedaudiorecorder.presentation.components.SettingsDialog
import com.example.advancedaudiorecorder.presentation.components.TopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    modifier: Modifier,
    onSwitchRecording: () -> Unit,
    onSwitchPlaying: () -> Unit,
    onSwitchMetronome: () -> Unit,
    isRecording: Boolean,
    isPlaying: Boolean,
    isMetronomeEnabled: Boolean,
    audioEngine: AudioEngine?,
    onSetBpm: (Int) -> Unit,
    onExitConfirmed: () -> Unit,

    metronomeVolume: Float,
    onVolumeChange: (Float) -> Unit,
    projectsDirectory: Uri?,
    onFolderPick: () -> Unit
) {
    val bpm = audioEngine?.metronome?.bpm?.collectAsState(initial = 120)?.value ?: 120
    var showPopup by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope() // Для управления корутинами
    var popupJob by remember { mutableStateOf<Job?>(null) } // Для хранения текущей задачи

    var isSettingsDialogOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(onExitConfirmed, bpm, onChangeTempo = { newTempo ->
            onSetBpm(newTempo)
            showPopup = true
            popupJob?.cancel()
            popupJob = coroutineScope.launch {
                delay(800)
                showPopup = false
            }
        })
        Box(modifier = Modifier.weight(1f)) {
            Playlist(audioEngine)
        }
        BottomBar(
            onSwitchRecording = onSwitchRecording,
            onSwitchPlaying = onSwitchPlaying,
            onSwitchMetronome = onSwitchMetronome,
            isRecording = isRecording,
            isPlaying = isPlaying,
            isMetronomeEnabled = isMetronomeEnabled,
            onSettingsOpen = { isSettingsDialogOpen = true}
        )
    }
    if (showPopup) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bpm.toString(),
                fontSize = 80.sp, // Крупный текст
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error // Цвет текста
            )
        }
    }
    if (isSettingsDialogOpen) {
        SettingsDialog(
            initialVolume = metronomeVolume,
            onVolumeChange = onVolumeChange,
            initialDirectory = projectsDirectory,
            onFolderPick = onFolderPick,
            onDismissRequest = { isSettingsDialogOpen = false }
        )
    }
}