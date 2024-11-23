package com.example.advancedaudiorecorder.presentation.main

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.advancedaudiorecorder.audio.AudioEngine
import com.example.advancedaudiorecorder.service.AudioService
import kotlin.system.exitProcess

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val isRecording = mutableStateOf(false)
    val isPlaying = mutableStateOf(false)
    val isMetronomeEnabled = mutableStateOf(false)

    val audioEngine = mutableStateOf<AudioEngine?>(null)

    fun updateRecordingState(isRecordingNow: Boolean) {
        isRecording.value = isRecordingNow
    }
    fun updatePlayingState(isPlayingNow: Boolean) {
        isPlaying.value = isPlayingNow
    }
    fun updateMetronomeState(isMetronomeEnabledNow: Boolean) {
        isMetronomeEnabled.value = isMetronomeEnabledNow
    }

    fun updateAudioEngineState(audioEngineNow: AudioEngine) {
        audioEngine.value = audioEngineNow
    }

    fun switchRecording() {
        sendCommandToService(AudioService.ACTION_SWITCH_RECORDING)
    }
    fun switchPlaying() {
        sendCommandToService(AudioService.ACTION_SWITCH_PLAYING)
    }
    fun switchMetronome() {
        sendCommandToService(AudioService.ACTION_SWITCH_METRONOME)
    }

    fun stopService() {
        sendCommandToService(AudioService.ACTION_STOP_SERVICE)
    }
    fun startService() {
        sendCommandToService("START")  //Такой команды нет, но нужно просто запустить сервис
    }


     private fun sendCommandToService(action: String) {
        val intent = Intent(getApplication(), AudioService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }
}