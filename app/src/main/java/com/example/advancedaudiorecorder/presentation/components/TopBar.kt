package com.example.advancedaudiorecorder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.advancedaudiorecorder.R
import com.example.advancedaudiorecorder.presentation.screens.MainScreen

@Composable
fun TopBar(onExitConfirmed: () -> Unit) {
    var showExitDialog by remember { mutableStateOf(false) } // Управление видимостью диалога
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { showExitDialog = true },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_exit),
                contentDescription = "Выход",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Подтверждение выхода") },
                text = { Text("Вы действительно хотите выйти?") },
                confirmButton = {
                    TextButton(onClick = {
                        showExitDialog = false
                        onExitConfirmed() // Подтверждение выхода
                    }) {
                        Text("Выйти")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
        Box(
            modifier = Modifier
                .padding(end = 196.dp)
                .padding(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onPrimaryContainer, // Цвет внутренней заливки
                    shape = RoundedCornerShape(40.dp) // Та же форма, что и у границы
                )
                .border(
                    width = 2.dp, // Толщина границы
                    color = MaterialTheme.colorScheme.primaryContainer, // Цвет границы
                    shape = RoundedCornerShape(40.dp) // Овальная форма
                )
                .padding(8.dp) // Отступ внутри границы
        ) {
            //Кнопка изменения музыкального размера
            MusicalTimePickerButton()
        }
        Box(
            modifier = Modifier
                .padding(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onPrimaryContainer, // Цвет внутренней заливки
                    shape = RoundedCornerShape(20.dp) // Та же форма, что и у границы
                )
                .border(
                    width = 2.dp, // Толщина границы
                    color = MaterialTheme.colorScheme.primaryContainer, // Цвет границы
                    shape = RoundedCornerShape(20.dp) // Овальная форма
                )
                .padding(8.dp) // Отступ внутри границы
        ) {

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка "Rewind" слева
                Button(
                    onClick = { },
                    modifier = Modifier.size(30.dp), // Фиксированный размер кнопки
                    contentPadding = PaddingValues(0.dp), // Минимизация отступов внутри кнопки
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                // Вертикальная линия-разделитель
                //Spacer(modifier = Modifier.width(8.dp)) // Расстояние перед разделителем
                Divider(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxHeight(0.6f) // Высота разделителя
                        .width(1.dp) // Ширина линии
                )
                Spacer(modifier = Modifier.width(8.dp)) // Расстояние после разделителя

                Box(
                    modifier = Modifier
                        .clickable { } // Поведение кнопки
                        .padding(0.dp), // Минимальные отступы
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "120",
                        fontSize = 20.sp, // Увеличенный текст
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary // Цвет текста как в кнопке
                    )
                }

                // Вертикальная линия-разделитель
                Spacer(modifier = Modifier.width(8.dp))
                Divider(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .width(1.dp)
                )
                //Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { },
                    modifier = Modifier.size(30.dp), // Фиксированный размер кнопки
                    contentPadding = PaddingValues(0.dp), // Минимизация отступов внутри кнопки
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка "Rewind" слева
            IconButton(
                onClick = { /*TODO Действия для кнопки экспорта'*/ },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_export), // Ваш ресурс ic_rewind
                    contentDescription = "Экспорт",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(
                onClick = { /*TODO Действия для кнопки импорта*/ },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_import),
                    contentDescription = "Импорт",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    TopBar (
        onExitConfirmed = {}
    )
}