package com.example.advancedaudiorecorder.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.filter

@Composable
fun MusicalTimePickerButton() {
    var showDialog by remember { mutableStateOf(false) }
    var selectedNumerator by remember { mutableIntStateOf(4) }
    var selectedDenominator by remember { mutableIntStateOf(4) }

    // Кнопка для открытия окна выбора
    Button(
        onClick = { showDialog = true },
        modifier = Modifier.size(32.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer)
    ) {
        Text("$selectedNumerator/$selectedDenominator", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                modifier = Modifier.wrapContentSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Выберите размер", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.Center) {
                        // Список для выбора числителя
                        InfiniteNumberPicker(
                            range = 1..6,
                            selectedValue = selectedNumerator,
                            onValueChange = { selectedNumerator = it }
                        )

                        Text(
                            "/",
                            fontSize = 24.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        InfiniteNumberPicker(
                            range = listOf(1, 2, 4, 8),
                            selectedValue = selectedDenominator,
                            onValueChange = { selectedDenominator = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun InfiniteNumberPicker(
    range: Iterable<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    val items = range.toList()
    val itemCount = items.size

    // Находим индекс выбранного элемента
    val initialIndex = items.indexOf(selectedValue).takeIf { it >= 0 } ?: (2)

    // Инициализация состояния списка с учетом цикличности
    val listState = rememberLazyListState(
        // Устанавливаем начальную позицию так, чтобы выбранный элемент был в центре
        initialFirstVisibleItemIndex = Int.MAX_VALUE / 2 - (itemCount / 2) + initialIndex
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .height(150.dp)
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(Int.MAX_VALUE) { index ->
            val value = items[index % itemCount] // Циклический доступ
            Text(
                text = value.toString(),
                fontSize = 24.sp,
                fontWeight = if (value == selectedValue) FontWeight.Bold else FontWeight.Normal,
                color = if (value == selectedValue) MaterialTheme.colorScheme.primary else Color.Unspecified,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onValueChange(value) }
            )
        }
    }

    // Центрирование на ближайший элемент после остановки прокрутки
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it } // Срабатывает, когда прокрутка завершена
            .collect {
                val index = listState.firstVisibleItemIndex % itemCount
                val centeredValue = items[index]
                if (centeredValue != selectedValue) {
                    onValueChange(centeredValue)
                }
            }
    }
}