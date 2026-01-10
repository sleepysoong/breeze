package com.sleepysoong.breeze

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sleepysoong.breeze.service.RunningService
import com.sleepysoong.breeze.ui.navigation.BreezeNavHost
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var runningService: RunningService? = null
    private var isServiceBound by mutableStateOf(false)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? RunningService.RunningBinder
            runningService = binder?.getService()
            isServiceBound = true
            
            // 서비스가 러닝 중이면 러닝 화면으로 이동하도록 인텐트 설정
            if (runningService?.getCurrentState()?.isRunning == true) {
                val runningIntent = Intent().apply {
                    action = ACTION_OPEN_RUNNING
                    putExtra(RunningService.EXTRA_TARGET_PACE, runningService?.getCurrentState()?.targetPaceSeconds ?: 390)
                }
                setContentWithIntent(runningIntent)
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            runningService = null
            isServiceBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 서비스 바인딩 시도 (이미 실행 중인 서비스가 있다면 연결)
        val serviceIntent = Intent(this, RunningService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        handleIntentAction(intent)
        setContentWithIntent(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentAction(intent)
    }

    private fun setContentWithIntent(initialIntent: Intent?) {
        setContent {
            BreezeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BreezeTheme.colors.background
                ) {
                    BreezeNavHost(initialIntent = initialIntent)
                }
            }
        }
    }

    private fun handleIntentAction(intent: Intent) {
        if (intent.action == ACTION_OPEN_RUNNING) {
            setContentWithIntent(intent)
        }
    }

    companion object {
        const val ACTION_OPEN_RUNNING = "ACTION_OPEN_RUNNING"
    }
}
