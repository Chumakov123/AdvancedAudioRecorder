package com.example.advancedaudiorecorder.presentation.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedaudiorecorder.audio.AudioEngine
import com.example.advancedaudiorecorder.service.AudioService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state

    val isRecording = state.map { it.isRecording }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isPlaying = state.map { it.isPlaying }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isMetronomeEnabled = state.map { it.isMetronomeEnabled }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val metronomeVolume = state.map { it.metronomeVolume }.stateIn(viewModelScope, SharingStarted.Lazily, 1f)
    val projectsDirectory = state.map { it.projectsDirectory }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val audioEngine = MutableStateFlow<AudioEngine?>(null)

    fun updateState(newState: AudioState) {
        _state.value = newState
    }

    fun updateAudioEngineState(audioEngineNow: AudioEngine) {
        audioEngine.value = audioEngineNow
    }

    fun setBpm(newBpm:Int) {
        audioEngine.value?.metronome?.setBpm(newBpm)
    }
    fun setMetronomeVolume(newVolume:Float) {
        audioEngine.value?.metronome?.setVolume(newVolume)
    }
    fun setProjectsDirectory(context: Context, uri: Uri, moveFromOldDirectory: Boolean) {
        audioEngine.value?.changeProjectsDirectory(context, uri, moveFromOldDirectory)
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

data class AudioState(
    val isRecording: Boolean = false,
    val isPlaying: Boolean = false,
    val isMetronomeEnabled: Boolean = false,
    val metronomeVolume: Float = 1f,
    val projectsDirectory: Uri? = null
)