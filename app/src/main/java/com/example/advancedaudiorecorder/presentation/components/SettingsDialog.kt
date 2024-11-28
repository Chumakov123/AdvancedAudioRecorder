package com.example.advancedaudiorecorder.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

@Composable
fun SettingsDialog(
    initialVolume: Float,
    initialDirectory: Uri?,
    onVolumeChange: (Float) -> Unit,
    onFolderPick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Текущая громкость
                Text(text = "Громкость метронома: ${(initialVolume * 100).toInt()}%", modifier = Modifier.padding(bottom = 16.dp))

                Slider(
                    value = initialVolume,
                    onValueChange = { onVolumeChange(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Text(text = "Рабочий каталог:")

                var showTooltip by remember { mutableStateOf(false) }
                Box {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = initialDirectory?.path.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { showTooltip = true }
                                    )
                                },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = {
                                onFolderPick()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(text = "Изменить")
                        }
                    }
                    if (showTooltip) {
                        Popup(
                            alignment = Alignment.CenterStart,
                            onDismissRequest = { showTooltip = false }
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp)
                                    .clickable {
                                        // Копирование текста в буфер обмена
                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Copied Directory", initialDirectory?.path.toString())
                                        clipboardManager.setPrimaryClip(clip)

                                        // Показ тоста
                                        Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()

                                        // Закрытие всплывающего окна
                                        showTooltip = false
                                    }
                            ) {
                                Text(
                                    text = initialDirectory?.path.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Кнопка закрытия
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "Закрыть")
                }
            }
        }
    }
}