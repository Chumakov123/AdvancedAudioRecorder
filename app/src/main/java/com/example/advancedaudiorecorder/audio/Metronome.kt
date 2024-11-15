package com.example.advancedaudiorecorder.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import com.example.advancedaudiorecorder.R
import kotlinx.coroutines.*

class Metronome(private val context: Context) {
    private var bpm: Int = 150
    private var metronomeJob: Job? = null
    private var interval = calculateInterval(bpm)

    private var soundPool: SoundPool
    private var kickSoundId: Int = 0

    val getBpm: Int
        get() = bpm

    val isRunning: Boolean
        get() = metronomeJob?.isActive == true

    // Инициализация SoundPool и загрузка кастомного звука
    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // Загрузка звука бас-бочки
        kickSoundId = soundPool.load(context, R.raw.kick, 1)
    }

    // Запуск метронома с кастомным звуком
    fun start(trackStartTimeMillis: Long = 0) {
        // Если метроном уже запущен, ничего не делаем
        if (isRunning) return

        // Вычисление времени до следующего такта на основе времени начала трека и BPM
        val firstTickDelay = calculateFirstTickDelay(trackStartTimeMillis)

        metronomeJob = CoroutineScope(Dispatchers.IO).launch {
            // Задержка до первого тика
            delay(firstTickDelay)

            // Воспроизведение тиков
            while (isActive) {
                soundPool.play(kickSoundId, 1f, 1f, 1, 0, 1f) // Воспроизведение бас-бочки
                delay(interval)
            }
        }
    }

    // Остановка метронома
    fun stop() {
        metronomeJob?.cancel()
    }

    // Изменение темпа
    fun setBpm(newBpm: Int) {
        bpm = newBpm
        interval = calculateInterval(bpm) // Пересчитываем интервал
    }

    // Вычисление интервала на основе BPM
    private fun calculateInterval(bpm: Int): Long {
        return (60000 / bpm).toLong() // Интервал в миллисекундах
    }

    // Рассчитываем время до первого такта
    private fun calculateFirstTickDelay(trackStartTimeMillis: Long): Long {
        // Время, которое прошло с начала
        val timeSinceTrackStart = trackStartTimeMillis % interval

        // Остаток времени до следующего такта
        return if (timeSinceTrackStart == 0L) {
            0L // Если время на начало такта, первый тик сразу
        } else {
            interval - timeSinceTrackStart // Время до следующего такта
        }
    }

    // Очистка ресурсов
    fun release() {
        soundPool.release()
    }
}