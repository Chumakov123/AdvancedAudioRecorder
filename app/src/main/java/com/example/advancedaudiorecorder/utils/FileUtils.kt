package com.example.advancedaudiorecorder.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileUtils {

    //region Audio formats
    fun convertPcmToWav(pcmFile: File, wavFile: File, sampleRate : Int) {
        val pcmSize = pcmFile.length().toInt()
        val wavHeader = ByteArray(44)

        // Заголовок WAV файла
        wavHeader[0] = 'R'.code.toByte()
        wavHeader[1] = 'I'.code.toByte()
        wavHeader[2] = 'F'.code.toByte()
        wavHeader[3] = 'F'.code.toByte()
        wavHeader[4] = (pcmSize + 36).toByte()
        wavHeader[5] = ((pcmSize + 36) shr 8).toByte()
        wavHeader[6] = ((pcmSize + 36) shr 16).toByte()
        wavHeader[7] = ((pcmSize + 36) shr 24).toByte()
        wavHeader[8] = 'W'.code.toByte()
        wavHeader[9] = 'A'.code.toByte()
        wavHeader[10] = 'V'.code.toByte()
        wavHeader[11] = 'E'.code.toByte()
        wavHeader[12] = 'f'.code.toByte()
        wavHeader[13] = 'm'.code.toByte()
        wavHeader[14] = 't'.code.toByte()
        wavHeader[15] = ' '.code.toByte()
        wavHeader[16] = 16
        wavHeader[20] = 1
        wavHeader[22] = 1
        wavHeader[24] = (sampleRate and 0xff).toByte()
        wavHeader[25] = ((sampleRate shr 8) and 0xff).toByte()
        wavHeader[32] = 2
        wavHeader[34] = 16
        wavHeader[36] = 'd'.code.toByte()
        wavHeader[37] = 'a'.code.toByte()
        wavHeader[38] = 't'.code.toByte()
        wavHeader[39] = 'a'.code.toByte()
        wavHeader[40] = pcmSize.toByte()
        wavHeader[41] = (pcmSize shr 8).toByte()
        wavHeader[42] = (pcmSize shr 16).toByte()
        wavHeader[43] = (pcmSize shr 24).toByte()

        FileOutputStream(wavFile).use { output ->
            output.write(wavHeader)
            FileInputStream(pcmFile).use { input ->
                input.copyTo(output)
            }
        }
    }

    //Получение массива PCM данных из файла OGG Vorbis
    fun getPcmDataFromOgg(context: Context, rawResId: Int): ByteArray {
        val assetFileDescriptor = context.resources.openRawResourceFd(rawResId)
        val extractor = MediaExtractor()
        extractor.setDataSource(
            assetFileDescriptor.fileDescriptor,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.length
        )
        assetFileDescriptor.close()

        var audioTrackIndex = -1
        var format: MediaFormat? = null

        // Поиск аудиодорожки
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }

        if (audioTrackIndex < 0 || format == null) {
            throw IllegalArgumentException("No audio track found in resource")
        }

        extractor.selectTrack(audioTrackIndex)

        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val pcmData = mutableListOf<Byte>()

        try {
            val inputBuffers = codec.inputBuffers
            val outputBuffers = codec.outputBuffers

            while (true) {
                // Загрузка данных в MediaCodec
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = inputBuffers[inputBufferIndex]
                    val size = extractor.readSampleData(inputBuffer, 0)
                    if (size >= 0) {
                        codec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            size,
                            extractor.sampleTime,
                            extractor.sampleFlags
                        )
                        extractor.advance()
                    } else {
                        codec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        break
                    }
                }

                // Извлечение PCM данных из MediaCodec
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = outputBuffers[outputBufferIndex]
                    val pcmChunk = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(pcmChunk)
                    pcmData.addAll(pcmChunk.toList())
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Формат изменился — обработка не требуется в большинстве случаев
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        return pcmData.toByteArray()
    }

    fun getSampleRateFromOgg(context: Context, rawResId: Int): Int {
        val assetFileDescriptor = context.resources.openRawResourceFd(rawResId)
        val extractor = MediaExtractor()

        try {
            // Устанавливаем источник данных
            extractor.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )

            // Ищем аудиотрек
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    // Возвращаем частоту дискретизации
                    return format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
            }

            throw IllegalArgumentException("No audio track found in resource")
        } finally {
            extractor.release()
            assetFileDescriptor.close()
        }
    }
    //endregion

    //region Files and folders
    fun getExistingDirectoryNames(context: Context, uri: Uri): List<String> {
        val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
        if (!documentFile.isDirectory) return emptyList()

        return documentFile.listFiles()
            .filter { it.isDirectory }
            .mapNotNull { it.name }
    }

    fun generateUniqueName(existingNames: List<String>, baseName: String = "Untitled"): String {
        var index = 1
        var uniqueName: String
        do {
            uniqueName = "$baseName$index"
            index++
        } while (existingNames.contains(uniqueName))

        return uniqueName
    }

    fun createSubdirectory(context: Context, uri: Uri, name: String): DocumentFile? {
        val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return null
        if (!documentFile.isDirectory) return null

        return documentFile.createDirectory(name)
    }
    //endregion
}