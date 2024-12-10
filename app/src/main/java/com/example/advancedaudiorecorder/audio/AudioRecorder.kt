package com.example.advancedaudiorecorder.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.advancedaudiorecorder.presentation.main.MainActivity
import com.example.advancedaudiorecorder.utils.FileUtils
import com.example.advancedaudiorecorder.utils.FileUtils.openInputStream
import com.example.advancedaudiorecorder.utils.FileUtils.openOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AudioRecorder (private val context: Context) {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private lateinit var audioRecord: AudioRecord
    private var selectedTrack = 0

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private lateinit var audioFileUri : Uri
    private lateinit var projectFolder: Uri

    fun startRecording(trackId: Int, uri: Uri): Boolean {
        if (!checkPermission()) return false

        selectedTrack = trackId

        // Создаем PCM файл
        val audioFileName = "track_${selectedTrack}.pcm"
        projectFolder = uri
        Log.d("checkData","create pcm")
        audioFileUri = FileUtils.createFile(context, projectFolder, audioFileName, "audio/pcm")!!

        val outputStream = audioFileUri.openOutputStream(context)
        if (outputStream == null) {
            Log.d("startRecording", "PCM output stream is null")
            return false
        }

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
            outputStream.use { stream ->
                val audioData = ByteArray(bufferSize)
                while (_isRecording.value && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readBytes = audioRecord.read(audioData, 0, bufferSize)
                    if (readBytes > 0) {
                        stream.write(audioData, 0, readBytes)
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

        // Создаем WAV файл
        val wavFileName = "track_${selectedTrack}.wav"
        val wavFileName2 = "track_${selectedTrack}"

        Log.d("checkData","create wav")
        val wavFileUri = FileUtils.createFile(context, projectFolder, wavFileName, "audio/wav")
        if (wavFileUri == null) {
            Toast.makeText(context, "Не удалось создать WAV файл", Toast.LENGTH_SHORT).show()
            return
        }

        val pcmInputStream = audioFileUri.openInputStream(context)
        val wavOutputStream = wavFileUri.openOutputStream(context)

        if (pcmInputStream == null || wavOutputStream == null) {
            Toast.makeText(context, "Ошибка при доступе к потокам", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            FileUtils.convertPcmToWav(pcmInputStream, wavOutputStream, sampleRate)
        } catch (e: Exception) {
            Log.e("stopRecording", "Ошибка при конвертации PCM в WAV", e)
        } finally {
            pcmInputStream.close()
            wavOutputStream.close()
        }
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