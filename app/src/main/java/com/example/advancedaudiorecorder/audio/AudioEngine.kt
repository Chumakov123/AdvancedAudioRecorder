package com.example.advancedaudiorecorder.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

@OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class AudioEngine
    (
    private val context: Context,
    private val onPlaybackComplete: () -> Unit,
    private val onPlaybackReady: () -> Unit)
{

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // Инициализируем ExoPlayer
    private var exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    val isRecording get() = audioRecorder.isRecording
    var isPlaying = false
    var selectedTrack = 0

    private var audioRecorder: AudioRecorder = AudioRecorder(context)
    var metronome: Metronome = Metronome(context, exoPlayer)

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        onPlaybackComplete()
                        Toast.makeText(context, "Воспроизведение завершено", Toast.LENGTH_SHORT).show()
                    }
                    Player.STATE_READY -> {
                        onPlaybackReady()
                        Log.d("checkData","ExoPlayer STATE_READY ${System.currentTimeMillis()}")
                        Log.d("checkData","currentPosition ${exoPlayer.currentPosition}")
                    }
                    Player.STATE_BUFFERING -> {
                        Log.d("checkData","ExoPlayer STATE_BUFFERING ${System.currentTimeMillis()}")
                    }
                    Player.STATE_IDLE -> {
                        Log.d("checkData","ExoPlayer STATE_IDLE ${System.currentTimeMillis()}")
                    }
                }
            }
        })
        //val playbackParameters = PlaybackParameters(1f,0.5f) //Понижение высоты звука на октаву на октаву
        //exoPlayer.playbackParameters = playbackParameters
    }

    fun startRecording() : Boolean {
        val res = audioRecorder.startRecording(selectedTrack)
        return res
    }

    fun stopRecording() {
        audioRecorder.stopRecording()
    }

    fun startPlayback(): Boolean {
        val pcmFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recorded_audio${selectedTrack}.wav")
        Log.d("checkData","${pcmFile.toUri()}")
        if (pcmFile.exists()) {
            isPlaying = true
            val mediaItem = MediaItem.fromUri(pcmFile.toUri())
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            Log.d("checkData", "exoPlayer.play() ${System.currentTimeMillis()}")
        } else {
            Toast.makeText(context, "Файл записи не найден", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun stopPlayback() {
        isPlaying = false
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    fun release() {
        exoPlayer.release()
    }
}