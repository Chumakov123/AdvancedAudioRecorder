package com.example.advancedaudiorecorder.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.media3.exoplayer.ExoPlayer
import com.example.advancedaudiorecorder.R
import com.example.advancedaudiorecorder.utils.FileUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Metronome(private val context: Context) {

    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm

    private var metronomeJob: Job? = null
    private var interval = calculateInterval(_bpm.value)

    val getBpm: Int
        get() = _bpm.value

    val isRunning: Boolean
        get() = metronomeJob?.isActive == true

    private lateinit var audioTrack: AudioTrack
    private var audioData: ByteArray
    private var audioSampleRate: Int = 0

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume

    @SuppressLint("NewApi")
    fun initialize() {
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(audioSampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioFormat(audioFormat)
            .setTransferMode(AudioTrack.MODE_STATIC) // Статический режим для минимизации задержки
            .setBufferSizeInBytes(audioData.size)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        audioTrack.write(audioData, 0, audioData.size) // Загрузка PCM в AudioTrack
        updateVolume()
    }

    init {
        audioSampleRate = FileUtils.getSampleRateFromOgg(context, R.raw.cowbell)
        audioData = FileUtils.getPcmDataFromOgg(context, R.raw.cowbell)
        initialize()
    }

    // Запуск метронома с кастомным звуком
    fun start(trackStartTimeMillis: Long = 0) {
        // Если метроном уже запущен, ничего не делаем
        if (isRunning) return

        val startTimeNs = System.nanoTime() // Абсолютное время начала

        var prev = startTimeNs

        metronomeJob = CoroutineScope(Dispatchers.Main).launch {
            var tickCount = 0 // Счётчик тиков

            while (isActive) {
                // Абсолютное время текущего тика
                val currentTickTimeNs = startTimeNs + tickCount * interval * 1_000_000

                // Текущее время
                val now = System.nanoTime()

                val sleepTimeMs = (currentTickTimeNs - now) / 1_000_000
                if (sleepTimeMs > 0) delay(sleepTimeMs)

                audioTrack.stop()
                audioTrack.reloadStaticData()
                audioTrack.play()

                // Логирование текущего тика
                //Время тика совпадает с позицией контента
//                val actualIntervalNs = now - prev
//                Log.d(
//                    "checkData",
//                    "metronome: ${actualIntervalNs / 1_000_000} ms"
//                )
//
//                if (exoPlayer.isPlaying) {
//                    Log.d("checkData", "exo: ${exoPlayer.contentPosition}")
//                }
                prev = now
                tickCount++
            }

            audioTrack.stop()
        }
    }

    // Остановка метронома
    fun stop() {
        metronomeJob?.cancel()
        audioTrack.stop()
    }

    // Изменение темпа
    fun setBpm(newBpm: Int) {
        if (_bpm.value == newBpm) return
        _bpm.value = newBpm
        interval = calculateInterval(_bpm.value) // Пересчитываем интервал
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

    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0.0f, 1.0f) // Ограничиваем значение между 0.0 и 1.0
        updateVolume()
    }

    // Применение уровня громкости к AudioTrack
    private fun updateVolume() {
        if (::audioTrack.isInitialized) {
            audioTrack.setVolume(_volume.value)
        }
    }

    // Очистка ресурсов
    fun release() {
        stop()
        audioTrack.release()
    }
}