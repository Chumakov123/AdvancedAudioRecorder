package com.example.advancedaudiorecorder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.advancedaudiorecorder.R

@Composable
fun BottomBar(
    onSwitchRecording: () -> Unit,
    onSwitchPlaying: () -> Unit,
    onSwitchMetronome: () -> Unit,
    isRecording: Boolean,
    isPlaying: Boolean,
    isMetronomeEnabled: Boolean,
    onSettingsOpen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.secondary)
            .padding(0.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { onSettingsOpen() },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .size(40.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Настройки",
                modifier = Modifier.size(30.dp),
                tint = Color.Unspecified
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка "Rewind" слева
            IconButton(
                onClick = { /* TODO Действие для кнопки Rewind */ },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_rewind),
                    contentDescription = "Перемотать в начало",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            IconButton(onClick = { onSwitchRecording() },
                modifier = Modifier.size(60.dp)) {
                Icon(
                    painter = painterResource(id = if (isRecording) R.drawable.ic_stop_recording else R.drawable.ic_record),
                    contentDescription = if (isRecording) "Остановить запись" else "Начать запись",
                    modifier = Modifier.size(60.dp),
                    tint = Color.Unspecified
                )
            }
            // Кнопка "Play" справа
            IconButton(
                onClick = { onSwitchPlaying() },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    painter = painterResource(id = if (isPlaying) R.drawable.ic_pause_audio else R.drawable.ic_play_audio),
                    contentDescription = "Проиграть",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        // Кнопка "Метроном" в правом углу
        IconButton(
            onClick = { onSwitchMetronome() },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(45.dp)
                .background(if (isMetronomeEnabled) MaterialTheme.colorScheme.inversePrimary else Color.Transparent, shape = CircleShape) // Подсветка, если включен
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_metronome),
                contentDescription = "Метроном",
                modifier = Modifier.size(45.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomBarPreview() {
    BottomBar (
        onSwitchRecording = {},
        onSwitchPlaying = {},
        onSwitchMetronome = {},
        isRecording = false,
        isPlaying = false,
        isMetronomeEnabled = true,
        onSettingsOpen = {}
    )
}