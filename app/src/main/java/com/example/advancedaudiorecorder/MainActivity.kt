package com.example.advancedaudiorecorder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.advancedaudiorecorder.service.AudioService
import com.example.advancedaudiorecorder.ui.theme.AdvancedAudioRecorderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var audioService: AudioService
    private var bound = false

    private val isRecording = mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as AudioService.LocalBinder
            audioService = localBinder.getService()
            bound = true

            // Подписываемся на изменения состояния из сервиса
            lifecycleScope.launch {
                audioService.isRecording.collect { isRunning ->
                    isRecording.value = isRunning
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, AudioService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdvancedAudioRecorderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                        onStartMetronome = { startMetronome() },
                        onStopMetronome = { stopMetronome() },
                        isRecording = isRecording.value
                    )
                }
            }
        }

    }
    private fun startMetronome() {
        sendCommandToService(AudioService.ACTION_START_METRONOME)
    }

    private fun stopMetronome() {
        sendCommandToService(AudioService.ACTION_STOP_METRONOME)
    }

    private fun startAudioService() {
        val intent = Intent(this, AudioService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopAudioService() {
        val intent = Intent(this, AudioService::class.java)
        stopService(intent)
    }

    private fun sendCommandToService(action: String) {
        val intent = Intent(this, AudioService::class.java)
        intent.action = action
        ContextCompat.startForegroundService(this, intent)
    }

    private fun setBpm(bpm: Int) {
        val intent = Intent(this, AudioService::class.java)
        intent.action = AudioService.ACTION_SET_BPM
        intent.putExtra(AudioService.EXTRA_BPM, bpm)
        ContextCompat.startForegroundService(this, intent)
    }

}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    onStartMetronome: () -> Unit,
    onStopMetronome: () -> Unit,
    isRecording: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Advanced Audio Recorder")

        Spacer(modifier = Modifier.height(16.dp))

        if (isRecording) {
            Button(onClick = onStopMetronome) {
                Text("Стоп")
            }
        } else {
            Button(onClick = onStartMetronome) {
                Text("Старт")
            }
        }
    }
}