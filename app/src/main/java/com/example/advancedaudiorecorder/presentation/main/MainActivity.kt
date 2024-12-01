package com.example.advancedaudiorecorder.presentation.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.advancedaudiorecorder.service.AudioService
import com.example.advancedaudiorecorder.ui.theme.AdvancedAudioRecorderTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.advancedaudiorecorder.presentation.screens.MainScreen
import com.example.advancedaudiorecorder.utils.UiUtils
import kotlinx.coroutines.flow.combine

class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val mainViewModel: MainViewModel by viewModels()
    private var audioService: AudioService? = null
    private var bound = false

    private val dirRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            Log.d("checkData", uri.path.toString())
            mainViewModel.setProjectsDirectory(this, uri, true)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d("checkData", "onServiceConnected")
            val localBinder = binder as AudioService.LocalBinder
            audioService = localBinder.getService()
            bound = true

            audioService?.let { service ->
                setupAudioServiceObservers(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("checkData", "onServiceDisconnected")
            audioService = null
            bound = false
        }
    }

    private fun setupAudioServiceObservers(service: AudioService) {
        // Передача состояния AudioEngine в ViewModel
        mainViewModel.updateAudioEngineState(service.audioEngine)

        // Наблюдение за разрешениями
        service.permissionRequest.observe(this@MainActivity) { permission ->
            requestPermission(permission)
        }

        // Подписка на состояния из сервиса
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                service.audioEngine.run {
                    combine(
                        isRecording,
                        isPlaying,
                        isMetronomeEnabled,
                        metronome.volume,
                        projectsDirectory
                    ) { isRecording, isPlaying, isMetronomeEnabled, metronomeVolume, projectsDirectory ->
                        AudioState(
                            isRecording,
                            isPlaying,
                            isMetronomeEnabled,
                            metronomeVolume,
                            projectsDirectory
                        )
                    }.collect { state ->
                        mainViewModel.updateState(state)
                    }
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        // Привязываем сервис
        val intent = Intent(this, AudioService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        mainViewModel.startService()
    }

    override fun onStop() {
        super.onStop()
        // Отвязываем сервис
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            UiUtils.setFullscreen(window)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerPermissionListener()
        //enableEdgeToEdge()
        setContent {
            AdvancedAudioRecorderTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSwitchRecording = { mainViewModel.switchRecording() },
                        onSwitchPlaying = { mainViewModel.switchPlaying() },
                        onSwitchMetronome = { mainViewModel.switchMetronome() },
                        isRecording = mainViewModel.isRecording.collectAsState().value,
                        isPlaying = mainViewModel.isPlaying.collectAsState().value,
                        isMetronomeEnabled = mainViewModel.isMetronomeEnabled.collectAsState().value,
                        audioEngine = mainViewModel.audioEngine.collectAsState().value,
                        onExitConfirmed = { exitApplication() },
                        onSetBpm = { newBpm -> mainViewModel.setBpm(newBpm) },
                        metronomeVolume = mainViewModel.metronomeVolume.collectAsState().value,
                        onVolumeChange = { newVolume -> mainViewModel.setMetronomeVolume(newVolume) },
                        onFolderPick = {dirRequest.launch(null)},
                        projectsDirectory = mainViewModel.projectsDirectory.collectAsState().value
                    )
                }
            }
        }
    }

    fun exitApplication() {
        val stopIntent = Intent(this, AudioService::class.java)
        this.stopService(stopIntent)

        mainViewModel.stopService()

        // Закрытие приложения
        this.finish()
        //exitProcess(0) // Завершение процесса приложения
    }

    fun requestPermission(permission : String) {
        permissionLauncher.launch(permission)
    }
    private fun registerPermissionListener() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()) {

        }

    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        modifier = Modifier,
        onSwitchRecording = {},
        onSwitchPlaying = {},
        onSwitchMetronome = {},
        onExitConfirmed = {},
        isRecording = false,
        isPlaying = false,
        isMetronomeEnabled = false,
        audioEngine = null,
        onSetBpm = {},
        metronomeVolume = 1f,
        onVolumeChange = {},
        onFolderPick =  {},
        projectsDirectory = null
    )
}