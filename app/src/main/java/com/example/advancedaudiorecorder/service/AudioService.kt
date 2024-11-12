package com.example.advancedaudiorecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.advancedaudiorecorder.MainActivity
import com.example.advancedaudiorecorder.R
import com.example.advancedaudiorecorder.audio.Metronome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioService : Service() {

    companion object {
        const val ACTION_START_METRONOME = "com.example.ACTION_START_METRONOME"
        const val ACTION_STOP_METRONOME = "com.example.ACTION_STOP_METRONOME"
        const val ACTION_SET_BPM = "com.example.ACTION_SET_BPM"
        const val EXTRA_BPM = "com.example.EXTRA_BPM"

        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_SERVICE"
        const val ACTION_TOGGLE_METRONOME = "com.example.ACTION_TOGGLE_METRONOME"
        const val ACTION_INCREASE_BPM = "com.example.ACTION_INCREASE_BPM"
        const val ACTION_DECREASE_BPM = "com.example.ACTION_DECREASE_BPM"
    }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private lateinit var metronome: Metronome

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
        metronome = Metronome()
    }
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        when (action) {
            ACTION_START_METRONOME -> metronome.start()
            ACTION_STOP_METRONOME -> metronome.stop()
            ACTION_SET_BPM -> {
                val bpm = intent.getIntExtra(EXTRA_BPM, 120)
                metronome.setBpm(bpm)
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
            ACTION_TOGGLE_METRONOME -> {
                if (metronome.isRunning) {
                    metronome.stop()
                } else {
                    metronome.start()
                }
            }
            ACTION_INCREASE_BPM -> {
                metronome.setBpm(metronome.getBpm + 5)
            }
            ACTION_DECREASE_BPM -> {
                metronome.setBpm(metronome.getBpm - 5)
            }
        }

        _isRecording.value = metronome.isRunning

        Log.d("checkData", "AudioService: onStartCommand")

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
        val toggleMetronomeIntent = Intent(this, AudioService::class.java).apply {
            action = ACTION_TOGGLE_METRONOME
        }
        val toggleMetronomePendingIntent = PendingIntent.getService(this, 1, toggleMetronomeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val icon : Int
        val (title, text, buttonTitle) = if (metronome.isRunning) {
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

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("checkData", "AudioService: onBind")
        return binder
    }

    override fun onDestroy() {
        Log.d("checkData", "AudioService: onDestroy")
        super.onDestroy()
        metronome.stop()
        metronome.release()
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

    fun isMetronomeRunning(): Boolean {
        return metronome.isRunning
    }
}