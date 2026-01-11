package com.sleepysoong.breeze.ui.pace

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PREF_NAME = "pace_dial_prefs"
private const val KEY_LAST_PACE = "last_pace_seconds"
private const val DEFAULT_PACE = 390

@Composable
fun PaceDialScreen(
    onDismiss: () -> Unit,
    onStartRunning: (paceSeconds: Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    val savedPace = remember { prefs.getInt(KEY_LAST_PACE, DEFAULT_PACE) }
    
    var minutes by remember { mutableIntStateOf(savedPace / 60) }
    var seconds by remember { mutableIntStateOf((savedPace % 60) / 5 * 5) } // 5초 단위로 맞춤
    
    val haptic = rememberHapticFeedback()
    val view = LocalView.current
    
    val paceSeconds = minutes * 60 + seconds
    
    // 페이스 변경 시 저장
    LaunchedEffect(paceSeconds) {
        prefs.edit().putInt(KEY_LAST_PACE, paceSeconds).apply()
    }
    
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
                text = "다이얼을 위아래로 스크롤하세요",
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
                ScrollableNumberDial(
                    value = minutes,
                    minValue = 1,
                    maxValue = 30,
                    label = "분",
                    onValueChange = { newValue ->
                        minutes = newValue
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onLimitReached = {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
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
                ScrollableNumberDial(
                    value = seconds,
                    minValue = 0,
                    maxValue = 55,
                    label = "초",
                    formatValue = { String.format("%02d", it) },
                    onValueChange = { newValue ->
                        seconds = newValue
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onLimitReached = {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    },
                    step = 5
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
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { !it.pressed && it.previousPressed }) {
                                    haptic()
                                    onStartRunning(paceSeconds)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
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
private fun ScrollableNumberDial(
    value: Int,
    minValue: Int,
    maxValue: Int,
    label: String,
    formatValue: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
    onLimitReached: () -> Unit,
    step: Int = 1
) {
    val scope = rememberCoroutineScope()
    
    // 드래그 누적값
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    
    // 쫀득한 오프셋 애니메이션
    val offsetY = remember { Animatable(0f) }
    
    // 스케일 애니메이션 (값 변경 시 펄스)
    val scale = remember { Animatable(1f) }
    
    val threshold = 30f // 값 변경에 필요한 드래그 양
    
    GlassCard(
        modifier = Modifier
            .size(width = 120.dp, height = 200.dp)
            .pointerInput(minValue, maxValue, step) {
                detectVerticalDragGestures(
                    onDragStart = {
                        // 드래그 시작 시 진행 중인 애니메이션 정지
                        scope.launch { offsetY.stop() }
                    },
                    onDragEnd = {
                        // 드래그 끝나면 오프셋을 0으로 스프링 애니메이션 (반동 효과)
                        scope.launch {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetY.animateTo(0f, spring())
                        }
                        dragAccumulator = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        
                        // 드래그 중 오프셋 업데이트 (쫀득한 느낌 - threshold 내에서만 움직임)
                        scope.launch {
                            val clampedOffset = (dragAccumulator % threshold).coerceIn(-threshold, threshold)
                            offsetY.snapTo(clampedOffset * 0.6f)
                        }
                        
                        // 값 변경 체크 (연속 스크롤 - while로 여러 번 변경 가능)
                        while (dragAccumulator >= threshold) {
                            // 아래로 드래그 = 값 증가
                            val newValue = value + step
                            if (newValue <= maxValue) {
                                onValueChange(newValue)
                                // 펄스 애니메이션
                                scope.launch {
                                    scale.snapTo(1.1f)
                                    scale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessHigh
                                        )
                                    )
                                }
                            } else {
                                onLimitReached()
                            }
                            dragAccumulator -= threshold
                        }
                        
                        while (dragAccumulator <= -threshold) {
                            // 위로 드래그 = 값 감소
                            val newValue = value - step
                            if (newValue >= minValue) {
                                onValueChange(newValue)
                                // 펄스 애니메이션
                                scope.launch {
                                    scale.snapTo(1.1f)
                                    scale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessHigh
                                        )
                                    )
                                }
                            } else {
                                onLimitReached()
                            }
                            dragAccumulator += threshold
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
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset { IntOffset(0, offsetY.value.roundToInt()) }
            ) {
                // 이전 값 (위) - 더 작은 값
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
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 다음 값 (아래) - 더 큰 값
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
