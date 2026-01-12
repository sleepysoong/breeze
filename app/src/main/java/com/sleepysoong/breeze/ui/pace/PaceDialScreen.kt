package com.sleepysoong.breeze.ui.pace

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

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
    var seconds by remember { mutableIntStateOf((savedPace % 60) / 5 * 5) }
    
    val haptic = rememberHapticFeedback()
    val view = LocalView.current
    
    val paceSeconds = minutes * 60 + seconds
    
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
            
            Text(
                text = "오늘은 어떤 속도로\n뛰어 볼까요",
                style = BreezeTheme.typography.headlineMedium,
                color = BreezeTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "카드를 위아래로 플립하세요",
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textTertiary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 테니스 스코어보드 스타일 플립 다이얼
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 분 다이얼
                FlipCardDial(
                    value = minutes,
                    minValue = 1,
                    maxValue = 30,
                    label = "분",
                    onValueChange = { newValue ->
                        minutes = newValue
                    },
                    onFlip = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                )
                
                // 콜론 구분자
                Text(
                    text = ":",
                    style = BreezeTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BreezeTheme.colors.primary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                // 초 다이얼
                FlipCardDial(
                    value = seconds,
                    minValue = 0,
                    maxValue = 55,
                    label = "초",
                    formatValue = { String.format("%02d", it) },
                    onValueChange = { newValue ->
                        seconds = newValue
                    },
                    onFlip = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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

/**
 * 테니스 스코어보드 스타일 플립 카드 다이얼
 * - 위아래 스와이프로 값 변경
 * - 빠른 플링 시 연속 플립 (따라라락)
 * - 무한 순환 (max 넘으면 min, min 넘으면 max)
 * - 3D 플립 애니메이션
 * - 각 플립마다 햅틱 피드백
 */
@Composable
private fun FlipCardDial(
    value: Int,
    minValue: Int,
    maxValue: Int,
    label: String,
    formatValue: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
    onFlip: () -> Unit,
    step: Int = 1
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // 플립 애니메이션 상태
    var isFlipping by remember { mutableStateOf(false) }
    var flipDirection by remember { mutableIntStateOf(0) } // -1: 위로(값감소), 1: 아래로(값증가)
    val flipRotation = remember { Animatable(0f) }
    
    // 다음에 표시될 값 (플립 중에 사용)
    var nextValue by remember { mutableIntStateOf(value) }
    
    // 플링 처리를 위한 대기열
    var pendingFlips by remember { mutableIntStateOf(0) }
    
    // 값이 외부에서 변경되면 nextValue 동기화
    LaunchedEffect(value) {
        if (!isFlipping) {
            nextValue = value
        }
    }
    
    // 연속 플립 처리
    LaunchedEffect(pendingFlips) {
        if (pendingFlips != 0 && !isFlipping) {
            val direction = pendingFlips.sign
            isFlipping = true
            flipDirection = direction
            
            // 다음 값 계산 (무한 순환)
            val newValue = if (direction > 0) {
                // 아래로 드래그 = 값 증가
                val next = value + step
                if (next > maxValue) minValue else next
            } else {
                // 위로 드래그 = 값 감소
                val prev = value - step
                if (prev < minValue) maxValue else prev
            }
            nextValue = newValue
            
            // 플립 애니메이션 실행
            flipRotation.snapTo(0f)
            flipRotation.animateTo(
                targetValue = 180f * direction,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            
            // 애니메이션 완료 후 값 적용
            onValueChange(newValue)
            onFlip()
            
            // 리셋
            flipRotation.snapTo(0f)
            isFlipping = false
            
            // 남은 플립 처리
            pendingFlips = if (abs(pendingFlips) > 1) {
                pendingFlips - direction
            } else {
                0
            }
            
            // 연속 플립 시 약간의 딜레이
            if (pendingFlips != 0) {
                delay(30)
            }
        }
    }
    
    // 드래그 및 플링 처리
    val velocityTracker = remember { VelocityTracker() }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val dragThreshold = with(density) { 40.dp.toPx() }
    val flingThreshold = 800f // px/sec
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 130.dp, height = 180.dp)
                .pointerInput(minValue, maxValue, step) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        dragAccumulator = 0f
                        
                        var change: PointerInputChange? = down
                        
                        while (change != null && change.pressed) {
                            change = awaitPointerEvent().changes.firstOrNull()
                            if (change != null && change.pressed) {
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val dragAmount = change.positionChange().y
                                dragAccumulator += dragAmount
                                
                                // 드래그로 값 변경 (threshold 초과 시)
                                while (abs(dragAccumulator) >= dragThreshold) {
                                    val direction = dragAccumulator.sign.toInt()
                                    pendingFlips += direction
                                    dragAccumulator -= dragThreshold * direction
                                }
                                
                                change.consume()
                            }
                        }
                        
                        // 플링 처리
                        val velocity = velocityTracker.calculateVelocity()
                        val yVelocity = velocity.y
                        
                        if (abs(yVelocity) > flingThreshold) {
                            // 속도에 비례해서 추가 플립
                            val extraFlips = (abs(yVelocity) / 1500f).roundToInt().coerceIn(1, 8)
                            val direction = yVelocity.sign.toInt()
                            pendingFlips += extraFlips * direction
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 글래스모피즘 배경
            GlassCard(
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 20.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.02f)
                                )
                            )
                        )
                )
            }
            
            // 중앙 분리선 (테니스 스코어보드 느낌)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BreezeTheme.colors.primary.copy(alpha = 0.3f),
                                BreezeTheme.colors.primary.copy(alpha = 0.5f),
                                BreezeTheme.colors.primary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // 상단 반쪽 카드 (이전 값 또는 현재 값)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .graphicsLayer {
                        // 플립 중일 때 3D 회전
                        if (isFlipping && flipDirection != 0) {
                            val rotation = flipRotation.value
                            if (abs(rotation) < 90f) {
                                rotationX = rotation
                                cameraDistance = 12f * density.density
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // 상단에는 현재 값 표시 (플립 중이고 90도 넘으면 다음 값)
                val displayValue = if (isFlipping && abs(flipRotation.value) >= 90f) {
                    nextValue
                } else {
                    value
                }
                
                Text(
                    text = formatValue(displayValue),
                    style = BreezeTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BreezeTheme.colors.textPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 0.dp)
                        .graphicsLayer {
                            // 90도 이상 회전 시 뒤집어진 텍스트 보정
                            if (isFlipping && abs(flipRotation.value) >= 90f) {
                                rotationX = 180f
                            }
                        }
                )
            }
            
            // 하단 반쪽 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .graphicsLayer {
                        if (isFlipping && flipDirection != 0) {
                            val rotation = flipRotation.value
                            if (abs(rotation) >= 90f) {
                                rotationX = rotation - 180f * flipDirection
                                cameraDistance = 12f * density.density
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val displayValue = if (isFlipping && abs(flipRotation.value) < 90f) {
                    value
                } else {
                    nextValue
                }
                
                Text(
                    text = formatValue(displayValue),
                    style = BreezeTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BreezeTheme.colors.textPrimary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 0.dp)
                )
            }
            
            // 상단/하단 그라디언트 효과
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BreezeTheme.colors.cardBackground.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BreezeTheme.colors.cardBackground.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
            
            // 테두리 하이라이트 (글래스모피즘)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 라벨
        Text(
            text = label,
            style = BreezeTheme.typography.bodyMedium,
            color = BreezeTheme.colors.textTertiary
        )
    }
}
