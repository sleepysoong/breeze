package com.sleepysoong.breeze.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepysoong.breeze.ui.theme.BreezeTheme

@Composable
fun HistoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "기록",
            style = BreezeTheme.typography.headlineLarge,
            color = BreezeTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "아직 러닝 기록이 없어요",
                    style = BreezeTheme.typography.bodyLarge,
                    color = BreezeTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "첫 러닝을 시작해보세요",
                    style = BreezeTheme.typography.bodyMedium,
                    color = BreezeTheme.colors.textTertiary
                )
            }
        }
    }
}
