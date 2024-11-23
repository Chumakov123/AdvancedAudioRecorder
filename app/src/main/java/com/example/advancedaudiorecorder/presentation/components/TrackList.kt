package com.example.advancedaudiorecorder.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.advancedaudiorecorder.audio.AudioEngine

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackList(audioEngine: AudioEngine?) {
    val selectedTrackIndex = audioEngine?.selectedTrackIndex?.collectAsState()?.value ?: 0
    var expandedTrackIndex by remember { mutableStateOf<Int?>(null) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        itemsIndexed(audioEngine?.tracks ?: emptyList()) { index, track ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
                    .height(72.dp)
                    .background(if (index == selectedTrackIndex) MaterialTheme.colorScheme.inverseOnSurface else Color.Transparent)
                    .combinedClickable(
                        onClick = { audioEngine?.selectTrack(index) },
                        onLongClick = { expandedTrackIndex = index } // Показать контекстное меню при удерживании
                    )
            ) {
                if (audioEngine != null) {
                    Text("Track ${audioEngine.tracks[index].id}")
                }
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = track.isEnabled,
                    onCheckedChange = { track.isEnabled = it }
                )

                // Контекстное меню
                DropdownMenu(
                    expanded = expandedTrackIndex == index,
                    onDismissRequest = { expandedTrackIndex = null }
                ) {
                    DropdownMenuItem(text = {
                        Text(text = "Удалить")
                    },
                        onClick = {
                            audioEngine?.removeTrack(index) // Удалить трек
                            expandedTrackIndex = null
                        })
                    DropdownMenuItem(text = {
                        Text(text = "Импорт")
                    },
                        onClick = {
                            //TODO Импорт аудио из проводника
                            expandedTrackIndex = null
                        })
                    DropdownMenuItem(text = {
                        Text(text = "Экспорт")
                    },
                        onClick = {
                            //TODO Экспорт аудио в одном из форматов
                            expandedTrackIndex = null
                        })
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Button(
                    onClick = { audioEngine?.addTrack() },
                    modifier = Modifier.size(40.dp), // Фиксированный размер кнопки
                    contentPadding = PaddingValues(0.dp) // Минимизация отступов внутри кнопки
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}