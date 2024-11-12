package com.example.advancedaudiorecorder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
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

    val serviceConnection = object : ServiceConnection {
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

    private fun setFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        // Прозрачный цвет для навигационной панели
        //window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullscreen()

        enableEdgeToEdge()
        setContent {
            AdvancedAudioRecorderTheme {
                Scaffold(modifier = Modifier.fillMaxSize().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)) { innerPadding ->
                    MainScreen(
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
fun MainScreen(
    modifier: Modifier = Modifier,
    onStartMetronome: () -> Unit,
    onStopMetronome: () -> Unit,
    isRecording: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Верхняя панель с названием программы
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Advanced Audio Recorder",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }

        // Средняя часть с вертикальной линией и пустым пространством
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f), // Занимает оставшуюся часть экрана
            contentAlignment = Alignment.Center
        ) {
            // Вертикальная линия
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight(1f) // Линия займет 80% высоты
                    .width(2.dp),
                color = Color.Red
            )
            Box(
                modifier = Modifier
                    .size(6.dp) // Размер точки
                    .background(Color.Red, shape = CircleShape)
                    .align(Alignment.TopCenter) // Позиционирование в верхней части
                    .padding(top = 4.dp) // Немного отступить от верхнего края
            )

            // Точка в нижней части линии
            Box(
                modifier = Modifier
                    .size(6.dp) // Размер точки
                    .background(Color.Red, shape = CircleShape)
                    .align(Alignment.BottomCenter) // Позиционирование в нижней части
                    .padding(bottom = 4.dp) // Немного отступить от нижнего края
            )
        }

        // Нижняя панель с кнопками
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.secondary)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (!isRecording) {
                        onStartMetronome()
                    } else {
                        onStopMetronome()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = if (isRecording) R.drawable.ic_record else R.drawable.ic_record),
                        contentDescription = "Toggle Metronome",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}