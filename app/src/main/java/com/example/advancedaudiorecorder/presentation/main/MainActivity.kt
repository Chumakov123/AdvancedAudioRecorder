package com.example.advancedaudiorecorder.presentation.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.advancedaudiorecorder.service.AudioService
import com.example.advancedaudiorecorder.ui.theme.AdvancedAudioRecorderTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.advancedaudiorecorder.presentation.screens.MainScreen
import com.example.advancedaudiorecorder.utils.UiUtils

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var audioService: AudioService? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as AudioService.LocalBinder
            audioService = localBinder.getService()
            bound = true

            // Подписываемся на изменения состояния записи из сервиса
            lifecycleScope.launch {
                audioService?.isRecording?.collect { isRunning ->
                    mainViewModel.updateRecordingState(isRunning)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            audioService = null
        }
    }

    override fun onStart() {
        super.onStart()
        // Привязываем сервис
        val intent = Intent(this, AudioService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        // Отвязываем сервис
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            UiUtils.setFullscreen(window)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdvancedAudioRecorderTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartMetronome = { mainViewModel.startMetronome() },
                        onStopMetronome = { mainViewModel.stopMetronome() },
                        isRecording = mainViewModel.isRecording.value
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        modifier = Modifier,
        onStartMetronome = { /* Пустой обработчик для превью */ },
        onStopMetronome = { /* Пустой обработчик для превью */ },
        isRecording = false // Или true для тестирования состояния записи
    )
}