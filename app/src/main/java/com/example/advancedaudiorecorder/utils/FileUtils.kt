package com.example.advancedaudiorecorder.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileUtils {

    //region Audio formats
    fun convertPcmToWav(pcmFile: File, wavFile: File, sampleRate: Int) {
        FileInputStream(pcmFile).use { pcmInputStream ->
            FileOutputStream(wavFile).use { wavOutputStream ->
                convertPcmToWav(pcmInputStream, wavOutputStream, sampleRate)
            }
        }
    }
    fun convertPcmToWav(inputStream: InputStream, outputStream: OutputStream, sampleRate: Int) {
        val pcmData = inputStream.available()
        val wavHeader = ByteArray(44)

        // Создание заголовка WAV
        wavHeader[0] = 'R'.code.toByte()
        wavHeader[1] = 'I'.code.toByte()
        wavHeader[2] = 'F'.code.toByte()
        wavHeader[3] = 'F'.code.toByte()
        wavHeader[4] = (pcmData + 36).toByte()
        wavHeader[5] = ((pcmData + 36) shr 8).toByte()
        wavHeader[6] = ((pcmData + 36) shr 16).toByte()
        wavHeader[7] = ((pcmData + 36) shr 24).toByte()
        wavHeader[8] = 'W'.code.toByte()
        wavHeader[9] = 'A'.code.toByte()
        wavHeader[10] = 'V'.code.toByte()
        wavHeader[11] = 'E'.code.toByte()
        wavHeader[12] = 'f'.code.toByte()
        wavHeader[13] = 'm'.code.toByte()
        wavHeader[14] = 't'.code.toByte()
        wavHeader[15] = ' '.code.toByte()
        wavHeader[16] = 16
        wavHeader[17] = 0
        wavHeader[18] = 0
        wavHeader[19] = 0
        wavHeader[20] = 1
        wavHeader[21] = 0
        wavHeader[22] = 1
        wavHeader[23] = 0
        wavHeader[24] = (sampleRate and 0xff).toByte()
        wavHeader[25] = ((sampleRate shr 8) and 0xff).toByte()
        wavHeader[26] = ((sampleRate shr 16) and 0xff).toByte()
        wavHeader[27] = ((sampleRate shr 24) and 0xff).toByte()
        wavHeader[28] = (sampleRate * 2 and 0xff).toByte()
        wavHeader[29] = ((sampleRate * 2 shr 8) and 0xff).toByte()
        wavHeader[30] = ((sampleRate * 2 shr 16) and 0xff).toByte()
        wavHeader[31] = ((sampleRate * 2 shr 24) and 0xff).toByte()
        wavHeader[32] = 2
        wavHeader[33] = 0
        wavHeader[34] = 16
        wavHeader[35] = 0
        wavHeader[36] = 'd'.code.toByte()
        wavHeader[37] = 'a'.code.toByte()
        wavHeader[38] = 't'.code.toByte()
        wavHeader[39] = 'a'.code.toByte()
        wavHeader[40] = (pcmData and 0xff).toByte()
        wavHeader[41] = ((pcmData shr 8) and 0xff).toByte()
        wavHeader[42] = ((pcmData shr 16) and 0xff).toByte()
        wavHeader[43] = ((pcmData shr 24) and 0xff).toByte()

        // Запись заголовка и данных
        outputStream.use { output ->
            output.write(wavHeader)
            inputStream.use { input ->
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

    fun getDirectory(context: Context, uri: Uri?): DocumentFile? {
        return when (uri?.scheme) {
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
    fun findFileInDirectory(directory: DocumentFile?, fileName: String): DocumentFile? {
        if (directory == null || !directory.isDirectory) return null
        //return directory.listFiles().firstOrNull { it.name == fileName && it.isFile }
        return directory.findFile(fileName)
    }
//    fun searchFile(directory: DocumentFile, fileName: String): DocumentFile? {
//        return if (directory.uri.scheme == "content") {
//            directory.listFiles().find { file -> file.name == fileName }
//        } else {
//            val folderPath = File(directory.uri.path ?: return null)
//            val filePath = File(folderPath, fileName)
//            if (filePath.exists()) DocumentFile.fromFile(filePath) else null
//        }
//    }

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

    fun moveFiles(context: Context, from: DocumentFile?, to: DocumentFile?) {
        if (from == null || to == null) return
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

    fun directoriesAreSame(dir1: DocumentFile?, dir2: DocumentFile?): Boolean {
        // Если обе директории null, они считаются одинаковыми
        if (dir1 == null && dir2 == null) return true

        // Если только одна из директорий null, они разные
        if (dir1 == null || dir2 == null) return false

        val uri1 = dir1.uri
        val uri2 = dir2.uri

        // Преобразуем URI в нормализованный вид
        val normalizedPath1 = uri1.normalizeUri()
        val normalizedPath2 = uri2.normalizeUri()

        // Сравниваем URI или свойства файлов
        return normalizedPath1 == normalizedPath2 ||
                (dir1.name == dir2.name && dir1.length() == dir2.length() && dir1.lastModified() == dir2.lastModified())
    }

    // Расширение для нормализации URI
    fun Uri.normalizeUri(): String {
        return when (scheme) {
            "content" -> path.orEmpty()  // Для content:// используем путь
            "file" -> toString().removePrefix("file://")  // Убираем префикс file://
            else -> toString()  // Для других схем используем полное представление
        }.trimEnd('/')
    }

    fun Uri.openInputStream(context: Context): InputStream? = try {
        context.contentResolver.openInputStream(this)
    } catch (e: Exception) {
        null
    }

    fun Uri.openOutputStream(context: Context): OutputStream? = try {
        context.contentResolver.openOutputStream(this)
    } catch (e: Exception) {
        null
    }

    fun getFileUriFromDirectory(projectFolder: Uri, fileName: String): Uri? {
        return when (projectFolder.scheme) {
            "content" -> {
                projectFolder.buildUpon()
                .path("document")
                .appendEncodedPath(Uri.encode(projectFolder.path?.substringAfter("/document/")) +Uri.encode("/$fileName"))
                .build()
                //Log.d("checkData","doc ${documentUri.path.toString()}")
                //Log.d("checkData","doc $documentUri")
            }
            "file" -> {
                val folder = File(projectFolder.path ?: return null)
                val file = File(folder, fileName)
                Uri.fromFile(file)
            }
            else -> null // Если схема неизвестна, возвращаем null
        }
    }

    fun createFile(context: Context, parentUri: Uri, fileName: String, mimeType: String): Uri? {
        return try {
            val resolver = context.contentResolver

            val document = findFileInDirectory(getDirectory(context, parentUri), fileName)
            if (document != null) {
                Log.d("checkData", "File already exists: ${document.uri}")
                return document.uri
            }

            if (parentUri.scheme == "content") {
                return DocumentsContract.createDocument(resolver, parentUri, mimeType, fileName)
            }

            if (parentUri.scheme == "file") {
                val file = File(parentUri.path, fileName)
                if (file.createNewFile()) {
                    return Uri.fromFile(file)
                } else {
                    throw IOException("Failed to create file: $file")
                }
            }
            null
        } catch (e: Exception) {
            Log.e("FileCreation", "Failed to create file: $fileName", e)
            null
        }
    }
    //endregion
}