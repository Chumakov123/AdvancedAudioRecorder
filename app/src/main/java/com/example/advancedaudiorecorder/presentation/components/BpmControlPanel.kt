package com.example.advancedaudiorecorder.presentation.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BpmControlPanel(
    currentTempo: Int,
    onTempoChange: (Int) -> Unit, // Функция изменения темпа
    onTapTempo: (List<Long>) -> Unit // Функция для обработки настукивания темпа
) {
    var tempo by remember { mutableIntStateOf(currentTempo) }
    var isIncreasing by remember { mutableStateOf(false) }
    var isDecreasing by remember { mutableStateOf(false) }
    var tapTimes by remember { mutableStateOf(listOf<Long>()) }
    var showDialog by remember { mutableStateOf(false) }

    // Удержание для увеличения/уменьшения
    LaunchedEffect(isIncreasing, isDecreasing) {
        while (isIncreasing || isDecreasing) {
            tempo = (tempo + if (isIncreasing) 1 else -1).coerceIn(30, 300) // Ограничение диапазона темпа
            onTempoChange(tempo)
            delay(100) // Интервал изменения
        }
    }

    Box(
        modifier = Modifier
            .padding(4.dp)
            .background(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка уменьшения темпа
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = Color.Transparent, // Задний фон
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isDecreasing = true
                                tryAwaitRelease()
                                isDecreasing = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "-",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Разделитель
            Divider(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .width(1.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Центральная кнопка (отображение текущего темпа)
            Box(
                modifier = Modifier.combinedClickable(
                    onClick = { Log.d("checkData","Click bpm")
                        val now = System.currentTimeMillis()
                        tapTimes = (tapTimes + now).takeLast(8) // Сохраняем последние 8 удара
                        if (tapTimes.size > 1) {
                            val avgInterval = tapTimes.zipWithNext { a, b -> b - a }.average().toLong()
                            val calculatedTempo = (60000 / avgInterval).toInt()
                            onTapTempo(tapTimes)
                            tempo = calculatedTempo.coerceIn(30, 300)
                        } },
                    onLongClick = { showDialog = true } // Показать контекстное меню при удерживании
                ).padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tempo.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Диалог ввода темпа
            var inputTempo by remember { mutableStateOf(tempo.toString()) }
            if (showDialog) {
                Log.d("checkData", "showDialog")
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Введите темп") },

                    text = {

                        TextField(
                            value = inputTempo,
                            onValueChange = { inputTempo = it },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            tempo = inputTempo.toIntOrNull()?.coerceIn(30, 300) ?: tempo
                            onTempoChange(tempo)
                            showDialog = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            // Разделитель
            Spacer(modifier = Modifier.width(8.dp))
            Divider(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .width(1.dp)
            )

            // Кнопка увеличения темпа
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = Color.Transparent, // Задний фон
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isIncreasing = true
                                tryAwaitRelease()
                                isIncreasing = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Удержание кнопки увеличения
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        Log.d("checkData", "IncreasingON")
                        isIncreasing = true
                        tryAwaitRelease()
                        Log.d("checkData", "IncreasingOFF")
                        isIncreasing = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BpmControlPanelPreview() {
    BpmControlPanel(120, {}, {})
}