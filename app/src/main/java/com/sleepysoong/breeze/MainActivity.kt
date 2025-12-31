package com.sleepysoong.breeze

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
        setContent {
            BreezeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BreezeTheme.colors.background
                ) {
                    BreezeNavHost()
                }
            }
        }
    }
}
