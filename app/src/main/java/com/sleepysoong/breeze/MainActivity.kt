package com.sleepysoong.breeze

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sleepysoong.breeze.ui.navigation.BreezeNavHost
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntentAction(intent)
        setContentWithIntent(intent)
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
