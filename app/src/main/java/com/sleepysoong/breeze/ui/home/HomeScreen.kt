package com.sleepysoong.breeze.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.theme.BreezeTheme

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Breeze",
            style = BreezeTheme.typography.headlineLarge,
            color = BreezeTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "오늘도 가볍게 달려볼까요",
            style = BreezeTheme.typography.bodyLarge,
            color = BreezeTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(60.dp))

        // 러닝 시작 버튼
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(BreezeTheme.colors.primary)
                .clickable { /* TODO: Navigate to pace selection */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "러닝 시작",
                modifier = Modifier.size(64.dp),
                tint = BreezeTheme.colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "러닝 시작",
            style = BreezeTheme.typography.titleMedium,
            color = BreezeTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 최근 기록 요약 카드
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "최근 기록",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(label = "거리", value = "- km")
                    StatItem(label = "시간", value = "- 분")
                    StatItem(label = "페이스", value = "-'--\"")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "아직 기록이 없어요",
                    style = BreezeTheme.typography.bodyMedium,
                    color = BreezeTheme.colors.textTertiary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 주간 통계 카드
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "이번 주",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(label = "총 거리", value = "0 km")
                    StatItem(label = "총 시간", value = "0 분")
                    StatItem(label = "러닝 횟수", value = "0 회")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = BreezeTheme.typography.headlineSmall,
            color = BreezeTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = BreezeTheme.typography.bodySmall,
            color = BreezeTheme.colors.textSecondary
        )
    }
}
