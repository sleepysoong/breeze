package com.sleepysoong.breeze.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import com.sleepysoong.breeze.ui.components.liquidglass.LiquidCard
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme

@Composable
fun HomeScreen(
    backdrop: Backdrop,
    latestRecord: RunningRecordEntity? = null,
    weeklyRecords: List<RunningRecordEntity> = emptyList(),
    onStartRunning: () -> Unit = {}
) {
    val haptic = rememberHapticFeedback()
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(150),
        label = "startButtonScale"
    )
    
    // 주간 통계 계산
    val weeklyDistance = weeklyRecords.sumOf { it.totalDistance } / 1000.0
    val weeklyTime = weeklyRecords.sumOf { it.totalTime } / 1000 / 60
    val weeklyCount = weeklyRecords.size
    
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

        // Liquid Glass 러닝 시작 버튼
        val density = LocalDensity.current
        val blurPx = with(density) { 8.dp.toPx() }
        val primaryColor = BreezeTheme.colors.primary
        
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(blurPx)
                    },
                    highlight = {
                        Highlight.Ambient.copy(alpha = if (isPressed) 0.8f else 0.5f)
                    },
                    shadow = {
                        Shadow(
                            radius = 16.dp,
                            color = primaryColor.copy(alpha = 0.3f)
                        )
                    },
                    onDrawSurface = {
                        drawRect(primaryColor.copy(alpha = 0.9f))
                    }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { haptic(); onStartRunning() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "러닝 시작",
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "러닝 시작",
            style = BreezeTheme.typography.titleMedium,
            color = BreezeTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 최근 기록 요약 카드 (Liquid Glass)
        LiquidCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "최근 기록",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (latestRecord != null) {
                    val distanceKm = latestRecord.totalDistance / 1000.0
                    val timeMinutes = latestRecord.totalTime / 1000 / 60
                    val paceMin = latestRecord.averagePace / 60
                    val paceSec = latestRecord.averagePace % 60
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "거리", value = String.format("%.2f km", distanceKm))
                        StatItem(label = "시간", value = "${timeMinutes}분")
                        StatItem(label = "페이스", value = String.format("%d'%02d\"", paceMin, paceSec))
                    }
                } else {
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 주간 통계 카드 (Liquid Glass)
        LiquidCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
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
                    StatItem(label = "총 거리", value = String.format("%.1f km", weeklyDistance))
                    StatItem(label = "총 시간", value = "${weeklyTime}분")
                    StatItem(label = "러닝 횟수", value = "${weeklyCount}회")
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
