package com.example.advancedaudiorecorder.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateListOf
import androidx.documentfile.provider.DocumentFile
import com.example.advancedaudiorecorder.model.ProjectData
import com.example.advancedaudiorecorder.model.TrackData
import com.example.advancedaudiorecorder.preferences.AppPreferences
import com.example.advancedaudiorecorder.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

@OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class AudioEngine (
    private val context: Context,
)   {

    enum class Mode {
        IDLE, PREPARE_RECORD, PREPARE_PLAYBACK, RECORD, PLAYBACK
    }

    private var audioRecorder: AudioRecorder = AudioRecorder(context)
    var metronome: Metronome = Metronome(context)

    private var mode : Mode = Mode.IDLE

    val tracks = mutableStateListOf<Track>()

    private val _selectedTrackIndex = MutableStateFlow(0)
    val selectedTrackIndex: StateFlow<Int> = _selectedTrackIndex

    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isMetronomeEnabled = MutableStateFlow(true)
    val isMetronomeEnabled: StateFlow<Boolean> = _isMetronomeEnabled

    private val appPreferences = AppPreferences(context)

    private val _projectsDirectory = MutableStateFlow(appPreferences.projectsDirectory)
    val projectsDirectory: StateFlow<Uri> = _projectsDirectory

    private val _projectName = MutableStateFlow(appPreferences.lastOpenedProject)
    val projectName: StateFlow<String?> = _projectName

    init {
        if (_projectName.value != null) {
            loadProject(_projectName.value.toString())
        }
        else {
            createProject()
        }
        metronome.setVolume(appPreferences.metronomeVolume)
    }

    fun createUniqueSubdirectory(context: Context, uri: Uri): DocumentFile? {
        val existingNames = FileUtils.getExistingDirectoryNames(context, uri)
        val uniqueName = FileUtils.generateUniqueName(existingNames)
        return FileUtils.createSubdirectory(context, uri, uniqueName)
    }

    fun setProjectsDirectory(uri: Uri) {
        _projectsDirectory.value = uri
    }

    fun addTrack() {
        val track = Track(tracks.count(), context, ::onPlaybackComplete, ::onPlaybackReady)
        tracks.add(track)
    }

    fun removeTrack(index: Int) {
        if (index in tracks.indices) {
            tracks[index].release()
            tracks.removeAt(index)
        }
    }

    fun selectTrack(index: Int) {
        if (tracks.count() > index && index >= 0)
            _selectedTrackIndex.value = index
    }

    fun onPlaybackComplete() {
        Log.d("checkData", "onPlaybackComplete()")
        if (mode == Mode.PLAYBACK) {
            if (allCompleted()) {
                mode = Mode.IDLE
                _isPlaying.value = false
                if (metronome.isRunning)
                    metronome.stop()
            }
        }
    }
    fun onPlaybackReady() {
        Log.d("checkData", "onPlaybackReady()")
        if (mode == Mode.PREPARE_PLAYBACK) {
            if (allReady()) {
                mode = Mode.PLAYBACK
                _isPlaying.value = true
                tracks.forEach{it.startPlaybackIfEnabled()}
            }
            if (_isMetronomeEnabled.value)
                metronome.start()
        }
        else if (mode == Mode.PREPARE_RECORD) {
            Log.d("checkData", "${allReady(true)}")
            if (allReady(true)) {
                mode = Mode.RECORD
                audioRecorder.startRecording(_selectedTrackIndex.value)
                tracks.forEach {
                    if (it.id != _selectedTrackIndex.value)
                        it.startPlaybackIfEnabled()
                }
                if (_isMetronomeEnabled.value)
                    metronome.start()
            }
        }
    }

    fun startRecording() : Boolean {
        if (!audioRecorder.checkPermission()) return false
        Log.d("checkData", "startRecording()")
        mode = Mode.PREPARE_RECORD
        tracks.forEach {
            if (it.id != _selectedTrackIndex.value)
                it.preparePlaybackIfEnabled()
        }
        onPlaybackReady()
        return true
    }

    fun stopRecording() {
        audioRecorder.stopRecording()
        tracks.forEach { it.stopPlayback() }
        if (metronome.isRunning)
            metronome.stop()
    }

    fun startPlayback(): Boolean {
        mode = Mode.PREPARE_PLAYBACK
        tracks.forEach { it.preparePlaybackIfEnabled() }
        onPlaybackReady()
        return true
    }

    fun stopPlayback() {
        _isPlaying.value = false
        tracks.forEach { it.stopPlayback() }
        if (metronome.isRunning)
            metronome.stop()
    }

    private fun allReady(excludeSelected : Boolean = false) : Boolean {
        return tracks.all {it.isReady() || (excludeSelected && it.id == _selectedTrackIndex.value)}
    }

    private fun allCompleted(excludeSelected : Boolean = false) : Boolean {
        return tracks.all {it.isCompleted() || (excludeSelected && it.id == _selectedTrackIndex.value)}
    }

    fun switchMetronome() {
        _isMetronomeEnabled.value = !_isMetronomeEnabled.value
        if (_isMetronomeEnabled.value) {
            if (mode == Mode.PLAYBACK || mode == Mode.RECORD) {
                metronome.start()
            }
        }
        else {
            if (metronome.isRunning)
                metronome.stop()
        }
    }

    fun release() {
        if (isRecording.value) stopRecording()
        if (isPlaying.value) stopPlayback()
        tracks.forEach { it.release() }
        savePreferences()
        metronome.release()
    }

    //TODO следующие методы вынести в класс ProjectManager
    fun createProject() {
        try {
            val projectFolder = createUniqueSubdirectory(context, _projectsDirectory.value)
                ?: throw IllegalStateException("Не удалось создать директорию проекта")

            _projectName.value = projectFolder.name
            repeat(2) { addTrack() }

            val project = ProjectData(
                name = _projectName.value
                    ?: throw IllegalStateException("Отсутствует название проекта"),
                isMetronomeEnabled = _isMetronomeEnabled.value,
                bpm = metronome.bpm.value,
                tracks = tracks.map { track ->
                    TrackData(
                        id = track.id,
                        isEnabled = track.isEnabled,
                        isLooping = track.isLooping,
                        volume = track.volume,
                        pitch = track.pitch,
                        speed = track.speed
                    )
                }
            )
            val json = Json.encodeToString(project)
            val jsonFile = projectFolder.createFile("application/json", "${projectFolder.name}.json")
                ?: throw IllegalStateException("Не удалось создать JSON-файл")

            context.contentResolver.openOutputStream(jsonFile.uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }

            Toast.makeText(context, "Проект ${projectFolder.name} создан",Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка при создании проекта: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    fun deleteProject(title: String) {
        //TODO удаление проекта
        // Нужна ли эта функция?
    }
    fun renameProject(title: String, newTitle: String) {
        //TODO переименование существующего проекта
        // Нужна ли эта функция?
    }
    fun loadProject(title: String) {
        val documentFile = DocumentFile.fromTreeUri(context, _projectsDirectory.value)
        val projectFolder = documentFile?.listFiles()?.find { it.name == title }
        projectFolder?.let {
            val jsonFile = it.listFiles().find { file -> file.name == "${projectFolder.name}.json" }
            jsonFile?.let { file ->
                context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    val jsonString = reader.readText()

                    // Десериализуем JSON в объект ProjectData
                    try {
                        val project = Json.decodeFromString<ProjectData>(jsonString)

                        _projectName.value = project.name
                        _isMetronomeEnabled.value = project.isMetronomeEnabled
                        metronome.stop()
                        stopRecording()
                        stopPlayback()
                        metronome.setBpm(project.bpm)
                        tracks.clear()
                        tracks.addAll(project.tracks.map { trackData ->
                            Track(
                                id = trackData.id,
                                context = context,
                                onPlaybackComplete = ::onPlaybackComplete,
                                onPlaybackReady = ::onPlaybackReady
                            ).apply {
                                isEnabled = trackData.isEnabled
                                isLooping = trackData.isLooping
                                volume = trackData.volume
                                pitch = trackData.pitch
                                speed = trackData.speed
                            }
                        })
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }
        Toast.makeText(context, "Проект $title открыт",Toast.LENGTH_SHORT).show()
        //TODO загрузка проекта из json
        //TODO навести порядок
    }
    fun saveProject(title: String) {
        Toast.makeText(context, "Проект $title сохранен",Toast.LENGTH_SHORT).show()
        //TODO сохранение проекта в json
    }
    fun savePreferences() {
        appPreferences.metronomeVolume = metronome.volume.value
        appPreferences.projectsDirectory = _projectsDirectory.value
        appPreferences.lastOpenedProject = _projectName.value
    }
}