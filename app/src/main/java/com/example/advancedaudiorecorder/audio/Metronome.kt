package com.example.advancedaudiorecorder.audio

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.*

class Metronome {
    private var bpm : Int = 120
    private var metronomeJob: Job? = null
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private var interval = calculateInterval(bpm)

    val getBpm : Int
        get() = bpm

    val isRunning : Boolean
        get() = metronomeJob?.isActive == true

    // Запуск метронома с учетом времени, выбранного пользователем
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
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
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
        toneGenerator.release()
    }
}