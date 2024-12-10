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
import com.example.advancedaudiorecorder.project.ProjectManager
import com.example.advancedaudiorecorder.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class AudioEngine (
    private val context: Context,
)   {

    enum class Mode {
        IDLE, PREPARE_RECORD, PREPARE_PLAYBACK, RECORD, PLAYBACK
    }

    //region Variables
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

    private val _currentProjectFolder = MutableStateFlow(appPreferences.lastOpenedProjectFolder)
    val currentProjectFolder: StateFlow<Uri?> = _currentProjectFolder

    private val _currentProjectName = MutableStateFlow(appPreferences.lastOpenedProject)
    val currentProjectName: StateFlow<String?> = _currentProjectName

    private val projectManager = ProjectManager(context)

    private var trackNumber: Int = 0;
    //endregion

    init {
        if (FileUtils.getDirectory(context, _currentProjectFolder.value) == null) {
            Log.d("checkData", "project not find ${_currentProjectFolder.value?.path}")
            _currentProjectFolder.value = null
        }


        if (_currentProjectFolder.value != null) {
            val projectFolder : Uri = _currentProjectFolder.value!!
            Log.d("checkData", _currentProjectFolder.value?.path.toString())
            val projectData = projectManager.loadProject(_currentProjectFolder.value!!)
            projectData?.let {
                loadProjectData(projectData, projectFolder)
                Toast.makeText(context, "Проект ${getProjectName()} загружен", Toast.LENGTH_SHORT).show()
            }
            //TODO Если проекта в этой директории больше нет, то создавать новый проект
        }
        else {
            initializeDefaultProject()

            val projectFolder = createUniqueSubdirectory(context, _projectsDirectory.value)
            projectFolder?.let {
                _currentProjectName.value = projectFolder.name
                _currentProjectFolder.value = projectFolder.uri

                projectManager.saveProject(projectFolder, getProjectData())
                _currentProjectFolder.value?.let {
                    tracks.forEach { track->track.setWavUri(it)}
                }
                Toast.makeText(context, "Проект ${getProjectName()} создан", Toast.LENGTH_SHORT).show()
            }

        }
        metronome.setVolume(appPreferences.metronomeVolume)
    }

    fun changeProjectsDirectory(context: Context, uri: Uri, moveFromOldDirectory: Boolean = false) {
        val newDirectory = FileUtils.getDirectory(context, uri) ?: throw IllegalArgumentException("Invalid URI")
        val oldDirectory = _projectsDirectory.value.let { FileUtils.getDirectory(context, it) }

        if (!moveFromOldDirectory || FileUtils.directoriesAreSame(oldDirectory, newDirectory)) {
            _projectsDirectory.value = uri
            return
        }

        _currentProjectFolder.value?.let {
            if ( FileUtils.isSubDirectory(parentUri = _projectsDirectory.value, childUri = it) ) {
                _currentProjectFolder.value = FileUtils.getDirectory(context, it)
                    ?.let { it1 -> FileUtils.moveFile(context, it1, newDirectory).uri }
            }
        }
        FileUtils.moveFiles(context, oldDirectory, newDirectory)
        _projectsDirectory.value = uri
    }

    //region Tracks
    fun addTrack() {
        //val projectFolder = _currentProjectFolder.value ?: return
        val track = Track(trackNumber++, context, ::onPlaybackComplete, ::onPlaybackReady)
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
    //endregion

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
            Log.d("checkData", "allReady ${allReady(true)}")
            if (allReady(true) && _currentProjectFolder.value != null) {
                mode = Mode.RECORD
                audioRecorder.startRecording(tracks[_selectedTrackIndex.value].id,
                    _currentProjectFolder.value!!
                )
                tracks.forEach {
                    if (it.id != _selectedTrackIndex.value)
                        it.startPlaybackIfEnabled()
                }
                if (_isMetronomeEnabled.value)
                    metronome.start()
            }
        }
    }

    //region Recording
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
    //endregion

    //region Playback
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
    //endregion


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
        //savePreferences()
        metronome.release()
    }

    private fun initializeDefaultProject() {
        Log.d("checkData", "initializeDefaultProject")
        trackNumber = 0
        repeat(2) { addTrack() }
        _isMetronomeEnabled.value = true
        metronome.setBpm(120)
        Log.d("checkData", "tracks count ${tracks.count()}")
    }

    //region ProjectData
    private fun getProjectData(): ProjectData {
        val projectName = _currentProjectName.value
            ?: throw IllegalStateException("Отсутствует название проекта")

        return ProjectData(
            name = projectName,
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
            },
            selectedTrackIndex = _selectedTrackIndex.value
        )
    }
     private fun loadProjectData(project: ProjectData, projectFolder : Uri) {
        _currentProjectName.value = project.name
        _isMetronomeEnabled.value = project.isMetronomeEnabled

        // Остановка записи и воспроизведения
        //TODO останавливать запись и проигрывание
        //stopRecording()
        //stopPlayback()
        metronome.stop()

        // Установка BPM
        metronome.setBpm(project.bpm)

        // Инициализация треков
        tracks.clear()
        tracks.addAll(project.tracks.map { trackData ->
            Track(
                id = trackData.id,
                context = context,
                onPlaybackComplete = ::onPlaybackComplete,
                onPlaybackReady = ::onPlaybackReady,
            ).apply {
                isEnabled = trackData.isEnabled
                isLooping = trackData.isLooping
                volume = trackData.volume
                pitch = trackData.pitch
                speed = trackData.speed
                _currentProjectFolder.value?.let { setWavUri(it) }
            }
        })
         trackNumber = 0
         if (tracks.isNotEmpty())
            trackNumber = tracks.maxOf { it.id + 1}
         _selectedTrackIndex.value = project.selectedTrackIndex
    }
    //endregion

    private fun saveProject() {
        _currentProjectFolder.value?.let {
            projectManager.saveProject(it, getProjectData())
            Toast.makeText(context, "Проект ${getProjectName()} сохранен", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getProjectName() : String? {
        return _currentProjectFolder.value?.path?.split("/")?.last()
    }

    private fun createUniqueSubdirectory(context: Context, uri: Uri): DocumentFile? {
        val existingNames = FileUtils.getExistingDirectoryNames(context, uri)
        val uniqueName = FileUtils.generateUniqueName(existingNames)
        return FileUtils.createSubdirectory(context, uri, uniqueName)
    }

    fun savePreferences() {
        appPreferences.metronomeVolume = metronome.volume.value
        appPreferences.projectsDirectory = _projectsDirectory.value
        appPreferences.lastOpenedProject = _currentProjectName.value
        appPreferences.lastOpenedProjectFolder = _currentProjectFolder.value
        saveProject()
    }
}