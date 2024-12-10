package com.example.advancedaudiorecorder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.advancedaudiorecorder.audio.AudioEngine

@Composable
fun Playlist(audioEngine: AudioEngine?) {
    val currentProjectName = audioEngine?.currentProjectName?.collectAsState()?.value ?: ""
    Box(
        modifier = Modifier
            .fillMaxSize().background(color = MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center
    ) {
        TrackList(audioEngine)
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

        Text(
            text = currentProjectName,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistPreview() {
    Playlist (
        audioEngine = null
    )
}