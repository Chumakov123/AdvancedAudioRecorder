package com.example.advancedaudiorecorder.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import java.io.File

object LogUtils {
    //Печать в консоль pcm данных из файла
    fun printPcmDataAsShorts(audioFile : File) {
        if (audioFile.exists()) {
            val pcmData = audioFile.readBytes() // Читаем содержимое файла в массив байтов
            Log.d("PCMData", "Размер PCM файла: ${pcmData.size} байт")

            // Проверяем, что размер данных кратен 2 (для 16-битного аудио)
            if (pcmData.size % 2 != 0) {
                Log.e("PCMData", "Размер PCM данных нечетный, возможно, файл поврежден.")
                return
            }

            // Преобразуем байты в массив Short
            val shortData = ShortArray(pcmData.size / 2)
            for (i in shortData.indices) {
                val low = pcmData[i * 2].toInt() and 0xFF  // Младший байт
                val high = pcmData[i * 2 + 1].toInt() and 0xFF  // Старший байт
                shortData[i] = ((high shl 8) or low).toShort()  // Собираем Short
            }

            // Для отладки выводим первые 100 значений (или сколько нужно)
            val dataPreview = shortData.take(100).joinToString(", ")
            Log.d("PCMData", "Первые 100 значений данных (Short): $dataPreview")
        } else {
            Log.e("PCMData", "PCM файл не найден")
        }
    }

    fun printUriInfo(uri: Uri, context: Context? = null) {
        Log.d("checkFiles", "uri: $uri")
        Log.d("checkFiles", "uri path: ${uri.path}")

        if (context == null) return

        val documentFile = DocumentFile.fromSingleUri(context, uri)
        Log.d("checkFiles","exists: " + documentFile?.exists().toString())
        Log.d("checkFiles","ifFile: " + documentFile?.isFile.toString())
        Log.d("checkFiles","isDirectory: " + documentFile?.isDirectory.toString())
        Log.d("checkFiles","name: " + documentFile?.name.toString())
        Log.d("checkFiles","type: " + documentFile?.type.toString())
        Log.d("checkFiles","parent exists: " + documentFile?.parentFile?.exists().toString())
        Log.d("checkFiles","parent: " + documentFile?.parentFile?.uri?.path.toString())
    }

    fun showError(context: Context, message: String, exception: Exception) {
        exception.printStackTrace()
        Toast.makeText(context, "$message: ${exception.message}", Toast.LENGTH_LONG).show()
        Log.e("checkData", "$message: ${exception.message}", exception)
    }
}