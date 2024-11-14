package com.example.advancedaudiorecorder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.advancedaudiorecorder.R

@Composable
fun TopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { /* TODO Действие для выхода */ },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_exit),
                contentDescription = "Выход",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
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
                    tint = Color.Unspecified
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
                    tint = Color.Unspecified
                )
            }
            IconButton(onClick = { /*TODO Действия для кнопки настроек*/},
                modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Настройки",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}