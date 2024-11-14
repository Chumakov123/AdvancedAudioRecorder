package com.example.advancedaudiorecorder.presentation.main

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.advancedaudiorecorder.service.AudioService

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Состояние записи
    val isRecording = mutableStateOf(false)

    // Метод для обновления состояния записи
    fun updateRecordingState(isRecordingNow: Boolean) {
        isRecording.value = isRecordingNow
    }

    // Методы для управления метрономом
    fun startMetronome() {
        sendCommandToService(AudioService.ACTION_START_METRONOME)
    }

    fun stopMetronome() {
        sendCommandToService(AudioService.ACTION_STOP_METRONOME)
    }

    private fun sendCommandToService(action: String) {
        val intent = Intent(getApplication(), AudioService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }
}