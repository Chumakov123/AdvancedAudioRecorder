package com.example.advancedaudiorecorder.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.advancedaudiorecorder.presentation.main.MainActivity
import com.example.advancedaudiorecorder.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AudioRecorder (private val context: Context) {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private lateinit var audioRecord: AudioRecord
    private var selectedTrack = 0

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun startRecording(trackId: Int): Boolean {
        // Проверка разрешения RECORD_AUDIO
        if (!checkPermission()) return false

        selectedTrack = trackId

        // Создаем файл для записи
        val audioFile =
            File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recorded_audio${selectedTrack}.pcm")
        if (audioFile.exists()) {
            audioFile.delete()
        }
        audioFile.createNewFile()

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        audioRecord.startRecording()
        _isRecording.value = true

        // Запись в файл
        CoroutineScope(Dispatchers.IO).launch {
            FileOutputStream(audioFile).use { outputStream ->
                val audioData = ByteArray(bufferSize)
                while (_isRecording.value && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readBytes = audioRecord.read(audioData, 0, bufferSize)
                    if (readBytes > 0) {
                        outputStream.write(audioData, 0, readBytes)
                    }
                }
            }
        }
        return true
    }
    fun stopRecording() {
        _isRecording.value = false
        audioRecord.stop()
        audioRecord.release()
        Toast.makeText(context, "Запись завершена", Toast.LENGTH_SHORT).show()

        // Используем временный файл записи, созданный ранее
        val pcmFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recorded_audio${selectedTrack}.pcm")
        val wavFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recorded_audio${selectedTrack}.wav")

        FileUtils.convertPcmToWav(pcmFile, wavFile, sampleRate)
        //LogUtils.printPcmDataAsShorts(pcmFile) // Печать PCM для отладки
    }

    fun checkPermission() : Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Нет разрешения на запись", Toast.LENGTH_SHORT).show()
            requestAudioPermission()
            return false
        }
        return true
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Разрешение уже предоставлено
        } else {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
}