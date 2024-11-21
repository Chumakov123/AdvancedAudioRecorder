package com.example.advancedaudiorecorder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.advancedaudiorecorder.audio.AudioEngine

@Composable
fun Playlist() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Вертикальная линия
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight(1f)
                .width(2.dp),
            color = Color.Red
        )
        Box(
            modifier = Modifier
                .size(6.dp) // Размер точки
                .background(Color.Red, shape = CircleShape)
                .align(Alignment.TopCenter) // Позиционирование в верхней части
                .padding(top = 4.dp) // Немного отступить от верхнего края
        )

        // Точка в нижней части линии
        Box(
            modifier = Modifier
                .size(6.dp) // Размер точки
                .background(Color.Red, shape = CircleShape)
                .align(Alignment.BottomCenter) // Позиционирование в нижней части
                .padding(bottom = 4.dp) // Немного отступить от нижнего края
        )
    }
}

@Composable
fun TrackListView(audioEngine: AudioEngine) {
    Column {
        audioEngine.tracks.forEachIndexed { index, track ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(if (index == audioEngine.selectedTrackIndex) Color.LightGray else Color.Transparent)
                    .clickable { audioEngine.selectedTrackIndex = index }
            ) {
                Text("Track ${index + 1}")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = track.isEnabled,
                    onCheckedChange = { track.isEnabled = it }
                )
            }
        }
        Button(onClick = { audioEngine.addTrack() }) {
            Text("Add Track")
        }
    }
}