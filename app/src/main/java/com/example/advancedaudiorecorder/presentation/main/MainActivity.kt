package com.example.advancedaudiorecorder.presentation.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.advancedaudiorecorder.service.AudioService
import com.example.advancedaudiorecorder.ui.theme.AdvancedAudioRecorderTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.test.platform.app.InstrumentationRegistry
import com.example.advancedaudiorecorder.audio.AudioEngine
import com.example.advancedaudiorecorder.presentation.screens.MainScreen
import com.example.advancedaudiorecorder.utils.UiUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.security.Permissions

class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val mainViewModel: MainViewModel by viewModels()
    private var audioService: AudioService? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as AudioService.LocalBinder
            audioService = localBinder.getService()
            bound = true

            audioService?.let { service ->
                val engine = service.audioEngine // Получаем AudioEngine из сервиса
                mainViewModel.updateAudioEngineState(engine)  // Передаем в ViewModel
            }

            audioService?.permissionRequest?.observe(this@MainActivity) { permission ->
                requestPermission(permission)
            }
            // Подписываемся на изменения состояния записи из сервиса
            lifecycleScope.launch {
                combine(
                    audioService?.isRecording ?: flowOf(false),
                    audioService?.isPlaying ?: flowOf(false),
                    audioService?.isMetronomeEnabled ?: flowOf(false)
                ) { isRecording, isPlaying, isMetronomeEnabled ->
                    Triple(isRecording, isPlaying, isMetronomeEnabled)
                }.collect { (isRecording, isPlaying, isMetronomeEnabled) ->
                    mainViewModel.updateRecordingState(isRecording)
                    mainViewModel.updatePlayingState(isPlaying)
                    mainViewModel.updateMetronomeState(isMetronomeEnabled)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            audioService = null
        }
    }
    override fun onStart() {
        super.onStart()
        // Привязываем сервис
        val intent = Intent(this, AudioService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        mainViewModel.sendCommandToService("START") //Такой команды нет, но нужно просто запустить сервис
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
        enableEdgeToEdge()
        setContent {
            AdvancedAudioRecorderTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSwitchRecording = { mainViewModel.switchRecording() },
                        onSwitchPlaying = { mainViewModel.switchPlaying() },
                        onSwitchMetronome = { mainViewModel.switchMetronome() },
                        isRecording = mainViewModel.isRecording.value,
                        isPlaying = mainViewModel.isPlaying.value,
                        isMetronomeEnabled = mainViewModel.isMetronomeEnabled.value,
                        audioEngine = mainViewModel.audioEngine.value
                    )
                }
            }
        }
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

//@Preview(showBackground = true)
//@Composable
//fun MainScreenPreview() {
//    MainScreen(
//        modifier = Modifier,
//        onSwitchRecording = {},
//        onSwitchPlaying = {},
//        onSwitchMetronome =  {},
//        isRecording = false,
//        isPlaying = false,
//        isMetronomeEnabled = false
//
//    )
//}

class MockAudioEngine : AudioEngine(
    context = mockContext()
) {
    init {
        // Инициализируйте фиктивные треки, если требуется
        addTrack()
        addTrack()
    }
}

// Вспомогательная функция для создания фиктивного контекста
fun mockContext(): Context = InstrumentationRegistry.getInstrumentation().targetContext