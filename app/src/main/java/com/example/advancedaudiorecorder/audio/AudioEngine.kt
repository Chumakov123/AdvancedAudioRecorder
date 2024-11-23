package com.example.advancedaudiorecorder.audio

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
open class AudioEngine (
    private val context: Context,
)   {

    enum class Mode {
        IDLE, PREPARE_RECORD, PREPARE_PLAYBACK, RECORD, PLAYBACK
    }

    private var audioRecorder: AudioRecorder = AudioRecorder(context)
    var metronome: Metronome = Metronome(context)

    private var mode : Mode = Mode.IDLE

    val tracks = mutableStateListOf<Track>()

    private val _selectedTrackIndex = MutableStateFlow(0)
    val selectedTrackIndex: StateFlow<Int> = _selectedTrackIndex

    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isMetronomeEnabled = MutableStateFlow(true)
    val isMetronomeEnabled: StateFlow<Boolean> = _isMetronomeEnabled

    init {
        Log.d("checkData", "init audio engine")
        repeat(2) { addTrack() }
    }

    fun addTrack() {
        val track = Track(tracks.count(), context, ::onPlaybackComplete, ::onPlaybackReady)
        tracks.add(track)
    }

    fun removeTrack(index: Int) {
        if (index in tracks.indices) {
            tracks[index].release()
            tracks.removeAt(index)
        }
    }

    fun selectTrack(index: Int) {
        if (tracks.count() > index && index >= 0)
            _selectedTrackIndex.value = index
    }

    fun onPlaybackComplete() {
        Log.d("checkData", "onPlaybackComplete()")
        if (mode == Mode.PLAYBACK) {
            if (allCompleted()) {
                mode = Mode.IDLE
                _isPlaying.value = false
                if (metronome.isRunning)
                    metronome.stop()
            }
        }
    }
    fun onPlaybackReady() {
        Log.d("checkData", "onPlaybackReady()")
        if (mode == Mode.PREPARE_PLAYBACK) {
            if (allReady()) {
                mode = Mode.PLAYBACK
                _isPlaying.value = true
                tracks.forEach{it.startPlaybackIfEnabled()}
            }
            if (_isMetronomeEnabled.value)
                metronome.start()
        }
        else if (mode == Mode.PREPARE_RECORD) {
            Log.d("checkData", "${allReady(true)}")
            if (allReady(true)) {
                mode = Mode.RECORD
                audioRecorder.startRecording(_selectedTrackIndex.value)
                tracks.forEach {
                    if (it.id != _selectedTrackIndex.value)
                        it.startPlaybackIfEnabled()
                }
                if (_isMetronomeEnabled.value)
                    metronome.start()
            }
        }
    }

    fun startRecording() : Boolean {
        if (!audioRecorder.checkPermission()) return false
        Log.d("checkData", "startRecording()")
        mode = Mode.PREPARE_RECORD
        tracks.forEach {
            if (it.id != _selectedTrackIndex.value)
                it.preparePlaybackIfEnabled()
        }
        onPlaybackReady()
        return true
    }

    fun stopRecording() {
        audioRecorder.stopRecording()
        tracks.forEach { it.stopPlayback() }
        if (metronome.isRunning)
            metronome.stop()
    }

    fun startPlayback(): Boolean {
        mode = Mode.PREPARE_PLAYBACK
        tracks.forEach { it.preparePlaybackIfEnabled() }
        onPlaybackReady()
        return true
    }

    fun stopPlayback() {
        _isPlaying.value = false
        tracks.forEach { it.stopPlayback() }
        if (metronome.isRunning)
            metronome.stop()
    }

    private fun allReady(excludeSelected : Boolean = false) : Boolean {
        return tracks.all {it.isReady() || (excludeSelected && it.id == _selectedTrackIndex.value)}
    }

    private fun allCompleted(excludeSelected : Boolean = false) : Boolean {
        return tracks.all {it.isCompleted() || (excludeSelected && it.id == _selectedTrackIndex.value)}
    }

    fun switchMetronome() {
        _isMetronomeEnabled.value = !_isMetronomeEnabled.value
        if (_isMetronomeEnabled.value) {
            if (mode == Mode.PLAYBACK || mode == Mode.RECORD) {
                metronome.start()
            }
        }
        else {
            if (metronome.isRunning)
                metronome.stop()
        }
    }

    fun release() {
        if (isRecording.value) stopRecording()
        if (isPlaying.value) stopPlayback()
        tracks.forEach { it.release() }
        metronome.release()
    }
}