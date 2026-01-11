package com.sleepysoong.breeze.ui.pace

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import kotlinx.coroutines.delay

@Composable
fun PaceDialScreen(
    initialPaceSeconds: Int = 390,
    onDismiss: () -> Unit,
    onStartRunning: (paceSeconds: Int) -> Unit
) {
    var minutes by remember { mutableIntStateOf(initialPaceSeconds / 60) }
    var seconds by remember { mutableIntStateOf(initialPaceSeconds % 60) }
    
    var isMinutePulse by remember { mutableStateOf(false) }
    var isSecondPulse by remember { mutableStateOf(false) }
    
    val haptic = rememberHapticFeedback()
    val view = LocalView.current
    
    val minuteScale by animateFloatAsState(
        targetValue = if (isMinutePulse) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "minuteScale"
    )
    
    val secondScale by animateFloatAsState(
        targetValue = if (isSecondPulse) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "secondScale"
    )
    
    LaunchedEffect(isMinutePulse) {
        if (isMinutePulse) {
            delay(100)
            isMinutePulse = false
        }
    }
    
    LaunchedEffect(isSecondPulse) {
        if (isSecondPulse) {
            delay(100)
            isSecondPulse = false
        }
    }
    
    val paceSeconds = minutes * 60 + seconds
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BreezeTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 닫기 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { haptic(); onDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = BreezeTheme.colors.textSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 안내 문구
            Text(
                text = "오늘은 어떤 속도로\n뛰어 볼까요",
                style = BreezeTheme.typography.headlineMedium,
                color = BreezeTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "각 다이얼을 위아래로 드래그해서 조절하세요",
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textTertiary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 분:초 다이얼 (분리형)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 분 다이얼
                NumberDial(
                    value = minutes,
                    minValue = 1,
                    maxValue = 30,
                    label = "분",
                    scale = minuteScale,
                    onValueChange = { newValue ->
                        minutes = newValue
                        isMinutePulse = true
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onLimitReached = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                )
                
                // 콜론 구분자
                Text(
                    text = ":",
                    style = BreezeTheme.typography.displayLarge.copy(fontSize = 56.sp),
                    color = BreezeTheme.colors.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                // 초 다이얼
                NumberDial(
                    value = seconds,
                    minValue = 0,
                    maxValue = 55,
                    label = "초",
                    scale = secondScale,
                    formatValue = { String.format("%02d", it) },
                    onValueChange = { newValue ->
                        seconds = newValue
                        isSecondPulse = true
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onLimitReached = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    },
                    step = 5 // 5초 단위로 조절
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "1km당 목표 페이스",
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textSecondary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 시작 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BreezeTheme.colors.primary)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, _ -> }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.pressed }) {
                                        // 터치 시작
                                    } else if (event.changes.any { !it.pressed }) {
                                        haptic()
                                        onStartRunning(paceSeconds)
                                    }
                                }
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = BreezeTheme.colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "러닝 시작",
                        style = BreezeTheme.typography.titleLarge,
                        color = BreezeTheme.colors.textPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NumberDial(
    value: Int,
    minValue: Int,
    maxValue: Int,
    label: String,
    scale: Float,
    formatValue: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
    onLimitReached: () -> Unit,
    step: Int = 1
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    
    GlassCard(
        modifier = Modifier
            .size(width = 120.dp, height = 200.dp)
            .pointerInput(value, minValue, maxValue, step) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        dragAccumulator = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        
                        val threshold = 20f
                        if (dragAccumulator > threshold) {
                            // 아래로 드래그 = 값 증가 (숫자가 위로 올라가는 느낌)
                            val newValue = value + step
                            if (newValue <= maxValue) {
                                onValueChange(newValue)
                            } else {
                                onLimitReached()
                            }
                            dragAccumulator = 0f
                        } else if (dragAccumulator < -threshold) {
                            // 위로 드래그 = 값 감소 (숫자가 아래로 내려가는 느낌)
                            val newValue = value - step
                            if (newValue >= minValue) {
                                onValueChange(newValue)
                            } else {
                                onLimitReached()
                            }
                            dragAccumulator = 0f
                        }
                    }
                )
            },
        cornerRadius = 24.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 상단 그라디언트 페이드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BreezeTheme.colors.cardBackground,
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // 하단 그라디언트 페이드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BreezeTheme.colors.cardBackground
                            )
                        )
                    )
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 이전 값 (위) - 드래그하면 이 값으로 감소
                val prevValue = value - step
                if (prevValue >= minValue) {
                    Text(
                        text = formatValue(prevValue),
                        style = BreezeTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                        color = BreezeTheme.colors.textTertiary.copy(alpha = 0.4f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 현재 값
                Text(
                    text = formatValue(value),
                    style = BreezeTheme.typography.displayLarge.copy(fontSize = 56.sp),
                    color = BreezeTheme.colors.textPrimary,
                    modifier = Modifier.scale(scale)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 다음 값 (아래) - 드래그하면 이 값으로 증가
                val nextValue = value + step
                if (nextValue <= maxValue) {
                    Text(
                        text = formatValue(nextValue),
                        style = BreezeTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                        color = BreezeTheme.colors.textTertiary.copy(alpha = 0.4f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            // 라벨
            Text(
                text = label,
                style = BreezeTheme.typography.bodySmall,
                color = BreezeTheme.colors.textTertiary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}
