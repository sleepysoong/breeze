package com.sleepysoong.breeze.ui.pace

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import kotlin.math.roundToInt

@Composable
fun PaceDialScreen(
    initialPaceSeconds: Int = 390,
    onDismiss: () -> Unit,
    onStartRunning: (paceSeconds: Int) -> Unit
) {
    var paceSeconds by remember { mutableIntStateOf(initialPaceSeconds) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    
    val minutes = paceSeconds / 60
    val seconds = paceSeconds % 60
    
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
                IconButton(onClick = onDismiss) {
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
                text = "위아래로 드래그해서 조절하세요",
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textTertiary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 페이스 다이얼
            GlassCard(
                modifier = Modifier
                    .size(280.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                dragAccumulator = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragAccumulator += dragAmount
                                
                                val threshold = 30f
                                if (dragAccumulator > threshold) {
                                    val newPace = paceSeconds + 10
                                    if (newPace <= 1800) {
                                        paceSeconds = newPace
                                    }
                                    dragAccumulator = 0f
                                } else if (dragAccumulator < -threshold) {
                                    val newPace = paceSeconds - 10
                                    if (newPace >= 60) {
                                        paceSeconds = newPace
                                    }
                                    dragAccumulator = 0f
                                }
                            }
                        )
                    },
                cornerRadius = 140.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // 그라디언트 페이드 효과 (상단)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
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
                    
                    // 그라디언트 페이드 효과 (하단)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
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
                        // 이전 값 (희미하게)
                        val prevMinutes = (paceSeconds + 10) / 60
                        val prevSeconds = (paceSeconds + 10) % 60
                        if (paceSeconds + 10 <= 1800) {
                            Text(
                                text = String.format("%d:%02d", prevMinutes, prevSeconds),
                                style = BreezeTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                                color = BreezeTheme.colors.textTertiary.copy(alpha = 0.4f)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(36.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 현재 값 (크고 강조)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%d", minutes),
                                style = BreezeTheme.typography.displayLarge.copy(fontSize = 64.sp),
                                color = BreezeTheme.colors.textPrimary
                            )
                            Text(
                                text = ":",
                                style = BreezeTheme.typography.displayLarge.copy(fontSize = 64.sp),
                                color = BreezeTheme.colors.primary
                            )
                            Text(
                                text = String.format("%02d", seconds),
                                style = BreezeTheme.typography.displayLarge.copy(fontSize = 64.sp),
                                color = BreezeTheme.colors.textPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 다음 값 (희미하게)
                        val nextMinutes = (paceSeconds - 10) / 60
                        val nextSeconds = (paceSeconds - 10) % 60
                        if (paceSeconds - 10 >= 60) {
                            Text(
                                text = String.format("%d:%02d", nextMinutes, nextSeconds),
                                style = BreezeTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                                color = BreezeTheme.colors.textTertiary.copy(alpha = 0.4f)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }
                }
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
