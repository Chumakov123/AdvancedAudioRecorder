package com.example.advancedaudiorecorder.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

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
        return when (uri.scheme) {
            "content" -> {
                // Работаем с Tree URI через SAF
                val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
                if (!documentFile.isDirectory) return emptyList()

                documentFile.listFiles()
                    .filter { it.isDirectory }
                    .mapNotNull { it.name }
            }
            "file" -> {
                // Работаем с обычным файловым URI
                val file = File(uri.path ?: return emptyList())
                if (!file.isDirectory) return emptyList()

                file.listFiles()
                    ?.filter { it.isDirectory }
                    ?.mapNotNull { it.name }
                    ?: emptyList()
            }
            else -> {
                emptyList() // Неизвестный URI
            }
        }
    }

    fun generateUniqueName(existingNames: List<String>, baseName: String = "Untitled"): String {
        var index = 0
        var uniqueName: String
        do {
            uniqueName = "$baseName$index"
            index++
        } while (existingNames.contains(uniqueName))

        return uniqueName
    }

    fun createSubdirectory(context: Context, uri: Uri, name: String): DocumentFile? {
        return when (uri.scheme) {
            "content" -> {
                // Работаем с Tree URI через SAF
                val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return null
                if (!documentFile.isDirectory) return null

                documentFile.createDirectory(name)
            }
            "file" -> {
                // Работаем с обычным файловым URI
                val file = File(uri.path ?: return null)
                if (!file.isDirectory) return null

                val newDirectory = File(file, name)
                return if (newDirectory.mkdir()) {
                    DocumentFile.fromFile(newDirectory) // Создаём DocumentFile для локального файла
                } else {
                    null // Не удалось создать директорию
                }
            }
            else -> {
                null // Неизвестная схема URI
            }
        }
    }

    fun getDirectoryFromUri(context: Context, uri: Uri): DocumentFile? {
        return when (uri.scheme) {
            "content" -> {
                // Работаем с Tree URI через SAF
                val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return null
                if (!documentFile.isDirectory) return null
                documentFile
            }
            "file" -> {
                // Работаем с обычным файловым URI
                val file = File(uri.path ?: return null)
                if (!file.isDirectory) return null
                DocumentFile.fromFile(file)
            }
            else -> {
                null // Неизвестная схема URI
            }
        }
    }

    fun searchFile(context: Context, directory: DocumentFile, fileName: String): DocumentFile? {
        return if (directory.uri.scheme == "content") {
            directory.listFiles().find { file -> file.name == fileName }
        } else {
            val folderPath = File(directory.uri.path ?: return null)
            val filePath = File(folderPath, fileName)
            if (filePath.exists()) DocumentFile.fromFile(filePath) else null
        }
    }

    inline fun <reified T> readJson(context: Context, jsonFile: DocumentFile): T? {
        return try {
            context.contentResolver.openInputStream(jsonFile.uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                Json.decodeFromString(jsonString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("checkData", "Ошибка при чтении JSON: ${e.message}")
            null
        }
    }

     fun copyFiles(context: Context, from: DocumentFile, to: DocumentFile) {
        if (directoriesAreSame(from, to)) return
        if (!to.isDirectory || !to.canWrite()) {
            throw IllegalArgumentException("Destination is not a writable directory")
        }

        for (file in from.listFiles()) {
            if (file.isFile) {
                val destFile = to.createFile(file.type ?: "application/octet-stream", file.name ?: "unknown")
                if (destFile != null) {
                    file.uri.openInputStream(context)?.use { input ->
                        destFile.uri.openOutputStream(context)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } else if (file.isDirectory) {
                val subDir = to.createDirectory(file.name ?: "unknown") ?: continue
                copyFiles(context, file, subDir)
            }
        }
    }

    fun moveFiles(context: Context, from: DocumentFile, to: DocumentFile) {
        if (directoriesAreSame(from, to)) return

        if (!to.isDirectory || !to.canWrite()) {
            throw IllegalArgumentException("Destination is not a writable directory")
        }

        for (file in from.listFiles()) {
            if (file.isFile) {
                val destFile = to.createFile(file.type ?: "application/octet-stream", file.name ?: "unknown")
                if (destFile != null) {
                    file.uri.openInputStream(context)?.use { input ->
                        destFile.uri.openOutputStream(context)?.use { output ->
                            input.copyTo(output)
                            file.delete() // Удаляем файл только после успешного копирования
                        }
                    }
                }
            } else if (file.isDirectory) {
                val subDir = to.createDirectory(file.name ?: "unknown") ?: continue
                moveFiles(context, file, subDir)
                file.delete() // Удаляем папку только после успешного копирования всех её файлов
            }
        }
    }

    fun moveFile(context: Context, file: DocumentFile, destinationFolder: DocumentFile): DocumentFile {
        if (!destinationFolder.isDirectory || !destinationFolder.canWrite()) {
            throw IllegalArgumentException("Destination is not a writable directory")
        }

        val newFileName = generateUniqueFileName(destinationFolder, file.name ?: "unknown")

        return if (file.isFile) {
            val destFile = destinationFolder.createFile(file.type ?: "application/octet-stream", newFileName)
                ?: throw IllegalStateException("Failed to create destination file")

            file.uri.openInputStream(context)?.use { input ->
                destFile.uri.openOutputStream(context)?.use { output ->
                    input.copyTo(output)
                }
            }

            if (!file.delete()) {
                throw IllegalStateException("Failed to delete source file")
            }

            destFile
        } else if (file.isDirectory) {
            val existingDir = destinationFolder.findFile(file.name ?: "unknown")

            val destDir = if (existingDir != null && existingDir.isDirectory && existingDir.listFiles().isEmpty()) {
                existingDir // Используем существующую пустую директорию
            } else {
                destinationFolder.createDirectory(newFileName)
                    ?: throw IllegalStateException("Failed to create destination directory")
            }

            for (subFile in file.listFiles()) {
                moveFile(context, subFile, destDir)
            }

            if (!file.delete()) {
                throw IllegalStateException("Failed to delete source directory")
            }

            destDir
        } else {
            throw IllegalArgumentException("Source is neither a file nor a directory")
        }
    }

    fun isSubDirectory(parentUri: Uri, childUri: Uri): Boolean {
        val parentPath = parentUri.path ?: return false
        val childPath = childUri.path ?: return false

        // Проверяем, начинается ли путь ребенка с пути родителя
        return childPath.startsWith(parentPath)
    }

    private fun generateUniqueFileName(folder: DocumentFile, baseName: String): String {
        var uniqueName = baseName
        var counter = 1

        // Проверяем, существует ли файл или папка с таким именем
        while (folder.findFile(uniqueName) != null) {
            val dotIndex = baseName.lastIndexOf('.')
            uniqueName = if (dotIndex != -1) {
                val nameWithoutExtension = baseName.substring(0, dotIndex)
                val extension = baseName.substring(dotIndex)
                "$nameWithoutExtension ($counter)$extension"
            } else {
                "$baseName ($counter)"
            }
            counter++
        }

        return uniqueName
    }

    fun directoriesAreSame(dir1: DocumentFile, dir2: DocumentFile): Boolean {
        return dir1.uri == dir2.uri || (dir1.name == dir2.name && dir1.length() == dir2.length() && dir1.lastModified() == dir2.lastModified())
    }

    private fun Uri.openInputStream(context: Context): InputStream? = try {
        context.contentResolver.openInputStream(this)
    } catch (e: Exception) {
        null
    }

    private fun Uri.openOutputStream(context: Context): OutputStream? = try {
        context.contentResolver.openOutputStream(this)
    } catch (e: Exception) {
        null
    }
    //endregion
}