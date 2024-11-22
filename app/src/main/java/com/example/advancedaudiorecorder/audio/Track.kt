package com.example.advancedaudiorecorder.audio

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.io.File

class Track (
    val id: Int,
    private val context: Context,
    private val onPlaybackComplete: () -> Unit,
    private val onPlaybackReady: () -> Unit
) {
    var isEnabled by mutableStateOf(true)
    var isLooping = false
    var volume = 1f
    var pitch = 1f
    var speed = 1f

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val wavFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recorded_audio${id}.wav")

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> onPlaybackComplete()
                    Player.STATE_READY -> onPlaybackReady()
                }
            }
        })
    }

    fun isReady() : Boolean = exoPlayer.playbackState == Player.STATE_READY || !isEnabled || !wavFile.exists()
    fun isCompleted() : Boolean = !exoPlayer.isPlaying || !isEnabled || !wavFile.exists()

    fun preparePlaybackIfEnabled() {
        if (!isEnabled) return
        if (wavFile.exists()) {
            val mediaItem = MediaItem.fromUri(wavFile.toUri())
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.volume = volume
            exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            exoPlayer.playbackParameters = PlaybackParameters(speed, pitch)
            exoPlayer.prepare()
        }
    }

    fun startPlaybackIfEnabled() {
        if (!isEnabled) return
        val wavFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recorded_audio${id}.wav")
        if (wavFile.exists()) {
            exoPlayer.play()
        }
    }

    fun stopPlayback() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    fun release() {
        exoPlayer.release()
    }
}