package com.example.advancedaudiorecorder.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.advancedaudiorecorder.presentation.main.MainActivity
import com.example.advancedaudiorecorder.R
import com.example.advancedaudiorecorder.audio.AudioEngine
import com.example.advancedaudiorecorder.audio.Metronome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioService : Service() {

    companion object {
        const val ACTION_SWITCH_RECORDING = "com.example.ACTION_SWITCH_RECORDING"
        const val ACTION_SWITCH_METRONOME = "com.example.ACTION_SWITCH_METRONOME"
        const val ACTION_SWITCH_PLAYING = "com.example.ACTION_SWITCH_PLAYING"

        const val ACTION_SET_BPM = "com.example.ACTION_SET_BPM"
        const val EXTRA_BPM = "com.example.EXTRA_BPM"
        const val ACTION_INCREASE_BPM = "com.example.ACTION_INCREASE_BPM"
        const val ACTION_DECREASE_BPM = "com.example.ACTION_DECREASE_BPM"

        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_SERVICE"
    }

    lateinit var audioEngine: AudioEngine

    private val _permissionRequest = MutableLiveData<String>()
    val permissionRequest: LiveData<String> = _permissionRequest

    private lateinit var _isPlaying: StateFlow<Boolean>
    private lateinit var _isRecording: StateFlow<Boolean>
    private lateinit var _isMetronomeEnabled: StateFlow<Boolean>

    val isPlaying: StateFlow<Boolean> get() = _isPlaying
    val isRecording: StateFlow<Boolean> get() = _isRecording
    val isMetronomeEnabled: StateFlow<Boolean> get() = _isMetronomeEnabled

    // Пример уведомления для фонового сервиса
    private val channelId = "audio_service_channel"
    private val notificationId = 1

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("checkData", "AudioService: onCreate")
        createNotificationChannel()
        audioEngine = AudioEngine(this)
        _isPlaying = audioEngine.isPlaying
        _isRecording = audioEngine.isRecording
        _isMetronomeEnabled = audioEngine.isMetronomeEnabled

    }
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        var isStopped = false
        when (action) {
            ACTION_SWITCH_RECORDING -> {
                if (audioEngine.isRecording.value) {
                    audioEngine.stopRecording()
                }
                else {
                    if (audioEngine.startRecording()) {
                        if (audioEngine.isPlaying.value) { //Остановка проигрывания при начале записи
                            audioEngine.stopPlayback()
                        }
                    }
                    else {
                        checkRecordAudioPermission()
                    }
                }
            }
            ACTION_SWITCH_PLAYING -> {
                if (!audioEngine.isRecording.value) { //Запрет смены режима прослушивания во время записи
                    if (audioEngine.isPlaying.value) {
                        audioEngine.stopPlayback()
                    }
                    else {
                        audioEngine.startPlayback()
                    }
                }
            }
            ACTION_SWITCH_METRONOME -> {
                audioEngine.switchMetronome()
            }
            ACTION_SET_BPM -> {
                val bpm = intent.getIntExtra(EXTRA_BPM, 120)
                audioEngine.metronome.setBpm(bpm)
            }
            ACTION_STOP_SERVICE -> {
                isStopped = true
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            ACTION_INCREASE_BPM -> {
                audioEngine.metronome.setBpm(audioEngine.metronome.getBpm + 1)
            }
            ACTION_DECREASE_BPM -> {
                audioEngine.metronome.setBpm(audioEngine.metronome.getBpm - 1)
            }
        }
        //Log.d("checkData", "AudioService: onStartCommand")

        if (!isStopped)
            startForeground(notificationId, createNotification())
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        // Интент для остановки сервиса
        val stopIntent = Intent(this, AudioService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        // Интент для старта/остановки метронома
        val switchRecordingIntent = Intent(this, AudioService::class.java).apply {
            action = ACTION_SWITCH_RECORDING
        }
        val toggleMetronomePendingIntent = PendingIntent.getService(this, 1, switchRecordingIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val icon : Int
        val (title, text, buttonTitle) = if (audioEngine.metronome.isRunning) {
            icon = R.drawable.ic_stop
            Triple("Запись звука", "Идет запись звука...", "Остановить запись")
        } else {
            icon = R.drawable.ic_play
            Triple("Запись звука отключена", "Вы можете начать новую запись", "Начать запись")
        }
        // Создаем уведомление
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_music, buttonTitle, toggleMetronomePendingIntent) // Кнопка для управления метрономом
            .addAction(R.drawable.ic_close, "Выход", stopPendingIntent) // Кнопка для остановки сервиса
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("checkData", "AudioService: onBind")
        return binder
    }

    override fun onDestroy() {
        Log.d("checkData", "AudioService: onDestroy")
        super.onDestroy()
        audioEngine.release()
    }

    // Создаем канал уведомлений для API >= 26
    private fun createNotificationChannel() {
        Log.d("checkData", "AudioService: createNotificationChannel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Audio Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
    private fun checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            //Разрешение уже получено
        }
        else {
            requestAudioPermission()
        }
    }
    private fun requestAudioPermission() {
        if (!_permissionRequest.hasActiveObservers()) {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        _permissionRequest.postValue(Manifest.permission.RECORD_AUDIO)
    }
}