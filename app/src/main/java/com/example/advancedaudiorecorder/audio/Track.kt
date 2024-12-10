package com.example.advancedaudiorecorder.audio

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.advancedaudiorecorder.utils.FileUtils

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

    private var wavUri : Uri? = null
    private val wavName = "track_${id}.wav"
    private var projectFolderDocument : DocumentFile? = null


    fun setWavUri(projectFolder: Uri) {
        projectFolderDocument = FileUtils.getDirectory(context, projectFolder)
        wavUri = FileUtils.getFileUriFromDirectory(projectFolder, "track_${id}.wav")
    }

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

    fun isReady() : Boolean = exoPlayer.playbackState == Player.STATE_READY || !isEnabled || FileUtils.findFileInDirectory(projectFolderDocument, wavName) == null
    fun isCompleted() : Boolean = !exoPlayer.isPlaying || !isEnabled || FileUtils.findFileInDirectory(projectFolderDocument, wavName) == null

    fun preparePlaybackIfEnabled() {
        if (!isEnabled || projectFolderDocument == null) return

        //if (FileUtils.fileExists(context, wavUri)) {
        //Log.d("checkData", wavUri.toString())
        val mediaItem = MediaItem.fromUri(wavUri!!)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.volume = volume
        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        exoPlayer.playbackParameters = PlaybackParameters(speed, pitch)
        exoPlayer.prepare()
        //}
    }

    fun startPlaybackIfEnabled() {
        if (!isEnabled) return
        //if (FileUtils.fileExists(context, wavUri)) {
            exoPlayer.play()
       //}
    }

    fun stopPlayback() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    fun release() {
        exoPlayer.release()
    }
}