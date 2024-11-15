package com.example.advancedaudiorecorder.utils

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileUtils {
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
}