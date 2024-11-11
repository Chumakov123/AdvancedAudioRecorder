package com.example.advancedaudiorecorder

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.advancedaudiorecorder.service.AudioService
import com.example.advancedaudiorecorder.ui.theme.AdvancedAudioRecorderTheme

class MainActivity : ComponentActivity() {
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
                        onStopMetronome = { stopMetronome() }
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
    onStopMetronome: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Продвинутый рекордер")

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка для запуска метронома
        Button(onClick = onStartMetronome) {
            Text("Старт")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопка для остановки метронома
        Button(onClick = onStopMetronome) {
            Text("Стоп")
        }
    }
}