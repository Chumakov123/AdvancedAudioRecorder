package com.example.advancedaudiorecorder.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.advancedaudiorecorder.presentation.main.MainActivity
import com.example.advancedaudiorecorder.utils.FileUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AudioEngine(private val context: Context)
{

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private lateinit var audioRecord: AudioRecord
    private lateinit var audioFile: File
    private var mediaPlayer: MediaPlayer? = null

    var isRecording = false
    var isPlaying = false

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Разрешение уже предоставлено
        } else {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    fun startRecording() : Boolean {
        // Проверка разрешения RECORD_AUDIO
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Нет разрешения на запись", Toast.LENGTH_SHORT).show()
            requestAudioPermission()
            return false
        }

        // Создаем файл для записи
        audioFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recorded_audio.pcm")
        if (audioFile.exists()) {
            audioFile.delete()
        }
        audioFile.createNewFile()

        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
        audioRecord.startRecording()
        isRecording = true

        // Запись в файл
        CoroutineScope(Dispatchers.IO).launch {
            FileOutputStream(audioFile).use { outputStream ->
                val audioData = ByteArray(bufferSize)
                while (isRecording && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
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
        isRecording = false
        audioRecord.stop()
        audioRecord.release()
        Toast.makeText(context, "Запись завершена", Toast.LENGTH_SHORT).show()

        val wavFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recorded_audio.wav")
        FileUtils.convertPcmToWav(audioFile, wavFile, sampleRate)
        audioFile = wavFile
    }

    fun startPlayback() : Boolean {
        if (this::audioFile.isInitialized && audioFile.exists()) {
            isPlaying = true
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaPlayer?.setOnCompletionListener {
                isPlaying = false
                Toast.makeText(context, "Воспроизведение завершено", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Файл записи не найден", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun stopPlayback() {
        isPlaying = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + context.packageName)
        context.startActivity(intent)
    }

    // Метод для обработки результата запроса разрешений
    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Если разрешение получено, запускаем запись
                startRecording()
            } else {
                // Проверка, можно ли ещё раз запросить разрешение
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    // Пользователь выбрал "Не спрашивать снова" — предложим перейти в настройки
                    Toast.makeText(context, "Пожалуйста, предоставьте разрешение на запись аудио в настройках", Toast.LENGTH_LONG).show()
                    openAppSettings()
                } else {
                    // Пользователь просто отклонил запрос
                    Toast.makeText(context, "Разрешение на запись аудио не предоставлено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }
}