package com.example.advancedaudiorecorder.presentation.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
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

@OptIn(UnstableApi::class)
@Composable
fun InfiniteNumberPicker(
    range: Iterable<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    val items = range.toList()
    val itemCount = items.size

// Находим индекс выбранного элемента
    val initialIndex = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % itemCount) + items.indexOf(selectedValue) - 1
    // Инициализация состояния списка с учетом цикличности
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(selectedValue) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                val center = listState.layoutInfo.viewportEndOffset / 2
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                val centralItem = visibleItems.minByOrNull { item ->
                    val itemCenter = (item.offset + item.size / 2)
                    kotlin.math.abs(itemCenter - center)
                }
                centralItem?.let {
                    val centralIndex = it.index
                    val newValue = items[centralIndex % itemCount]
                    if (newValue != selectedValue) {
                        onValueChange(newValue)
                    }
                    listState.animateScrollToItemCenter(
                        index = centralIndex - 1
                    )
                }
                Log.d("checkData","scroll")
            }
    }

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
            )
        }
    }
}

suspend fun LazyListState.animateScrollToItemCenter(index: Int) {
    layoutInfo.resolveItemOffsetToCenter(index)?.let {
        animateScrollToItem(index, it)
        return
    }

    scrollToItem(index)

    layoutInfo.resolveItemOffsetToCenter(index)?.let {
        animateScrollToItem(index, it)
    }
}

private fun LazyListLayoutInfo.resolveItemOffsetToCenter(index: Int): Int? {
    val itemInfo = visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    val containerSize = viewportSize.width - beforeContentPadding - afterContentPadding
    return -(containerSize - itemInfo.size) / 2
}