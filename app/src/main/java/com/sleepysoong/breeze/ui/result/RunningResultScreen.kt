package com.sleepysoong.breeze.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme

@Composable
fun RunningResultScreen(
    distanceMeters: Double,
    elapsedTimeMs: Long,
    averagePaceSeconds: Int,
    targetPaceSeconds: Int,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    
    val distanceKm = distanceMeters / 1000.0
    val elapsedMinutes = (elapsedTimeMs / 1000 / 60).toInt()
    val elapsedSeconds = (elapsedTimeMs / 1000 % 60).toInt()
    
    // 칼로리 계산 (간단 공식: 거리(km) * 체중(kg) * 1.036)
    // 평균 체중 65kg 가정
    val calories = (distanceKm * 65 * 1.036).toInt()
    
    // 목표 달성 여부
    val paceDeviation = averagePaceSeconds - targetPaceSeconds
    val isGoalAchieved = kotlin.math.abs(paceDeviation) <= 30 // 30초 이내 오차
    
    // 저장하지 않음 확인 다이얼로그
    if (showDiscardDialog) {
        DiscardConfirmDialog(
            onConfirm = {
                showDiscardDialog = false
                onDiscard()
            },
            onDismiss = {
                showDiscardDialog = false
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BreezeTheme.colors.background)
    ) {
        // 스크롤 영역
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // 완료 메시지
            Text(
                text = if (isGoalAchieved) "목표 달성" else "러닝 완료",
                style = BreezeTheme.typography.headlineLarge,
                color = if (isGoalAchieved) BreezeTheme.colors.primary else BreezeTheme.colors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isGoalAchieved) {
                    "오늘도 멋진 러닝이었어요"
                } else {
                    "다음에는 목표를 달성해봐요"
                },
                style = BreezeTheme.typography.bodyLarge,
                color = BreezeTheme.colors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 지도 영역 (플레이스홀더)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "경로 지도",
                            style = BreezeTheme.typography.bodyLarge,
                            color = BreezeTheme.colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Google Maps API 키 설정 후 표시됩니다",
                            style = BreezeTheme.typography.bodySmall,
                            color = BreezeTheme.colors.textTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 주요 통계
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 거리 (대형)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "총 거리",
                            style = BreezeTheme.typography.bodyMedium,
                            color = BreezeTheme.colors.textSecondary
                        )
                        Text(
                            text = String.format("%.2f", distanceKm),
                            style = BreezeTheme.typography.displayLarge.copy(fontSize = 48.sp),
                            color = BreezeTheme.colors.textPrimary
                        )
                        Text(
                            text = "km",
                            style = BreezeTheme.typography.titleMedium,
                            color = BreezeTheme.colors.textSecondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 시간, 페이스, 칼로리
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ResultStatItem(
                            label = "시간",
                            value = String.format("%d:%02d", elapsedMinutes, elapsedSeconds)
                        )
                        ResultStatItem(
                            label = "평균 페이스",
                            value = if (averagePaceSeconds > 0) {
                                String.format("%d'%02d\"", averagePaceSeconds / 60, averagePaceSeconds % 60)
                            } else {
                                "--'--\""
                            },
                            highlight = isGoalAchieved
                        )
                        ResultStatItem(
                            label = "칼로리",
                            value = "$calories"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 목표 비교 카드
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "목표 페이스",
                            style = BreezeTheme.typography.bodySmall,
                            color = BreezeTheme.colors.textSecondary
                        )
                        Text(
                            text = String.format("%d'%02d\"", targetPaceSeconds / 60, targetPaceSeconds % 60),
                            style = BreezeTheme.typography.titleLarge,
                            color = BreezeTheme.colors.textPrimary
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "차이",
                            style = BreezeTheme.typography.bodySmall,
                            color = BreezeTheme.colors.textSecondary
                        )
                        Text(
                            text = when {
                                paceDeviation > 0 -> "+${paceDeviation}초"
                                paceDeviation < 0 -> "${paceDeviation}초"
                                else -> "정확히 일치"
                            },
                            style = BreezeTheme.typography.titleLarge,
                            color = when {
                                kotlin.math.abs(paceDeviation) <= 30 -> BreezeTheme.colors.primary
                                paceDeviation > 0 -> BreezeTheme.colors.textSecondary
                                else -> BreezeTheme.colors.primaryLight
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        // 버튼 영역 (고정)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 돌아가기 버튼 (저장 후 홈으로)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BreezeTheme.colors.primary)
                    .clickable { haptic(); onSave() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "돌아가기",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )
            }
            
            // 저장하지 않음 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BreezeTheme.colors.cardBackground)
                    .clickable { haptic(); showDiscardDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "저장하지 않음",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun DiscardConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BreezeTheme.colors.background.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            // 글래스모피즘 다이얼로그
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BreezeTheme.colors.cardBackground)
                    .border(
                        width = 1.dp,
                        color = BreezeTheme.colors.cardBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(enabled = false) {} // 내부 클릭 방지
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "기록을 삭제할까요?",
                        style = BreezeTheme.typography.titleLarge,
                        color = BreezeTheme.colors.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "저장하지 않으면 이번 러닝 기록이\n완전히 사라져요",
                        style = BreezeTheme.typography.bodyMedium,
                        color = BreezeTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 버튼들
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 취소 버튼
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BreezeTheme.colors.surface)
                                .border(
                                    width = 1.dp,
                                    color = BreezeTheme.colors.cardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { haptic(); onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "취소",
                                style = BreezeTheme.typography.labelLarge,
                                color = BreezeTheme.colors.textSecondary
                            )
                        }
                        
                        // 삭제 버튼
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BreezeTheme.colors.primary)
                                .clickable { haptic(); onConfirm() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "삭제",
                                style = BreezeTheme.typography.labelLarge,
                                color = BreezeTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = BreezeTheme.typography.bodySmall,
            color = BreezeTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = BreezeTheme.typography.headlineSmall,
            color = if (highlight) BreezeTheme.colors.primary else BreezeTheme.colors.textPrimary
        )
    }
}
