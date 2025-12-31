package com.sleepysoong.breeze.ui.result

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.breeze.ui.components.GlassButton
import com.sleepysoong.breeze.ui.components.GlassCard
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
    val distanceKm = distanceMeters / 1000.0
    val elapsedMinutes = (elapsedTimeMs / 1000 / 60).toInt()
    val elapsedSeconds = (elapsedTimeMs / 1000 % 60).toInt()
    
    // 칼로리 계산 (간단 공식: 거리(km) * 체중(kg) * 1.036)
    // 평균 체중 65kg 가정
    val calories = (distanceKm * 65 * 1.036).toInt()
    
    // 목표 달성 여부
    val paceDeviation = averagePaceSeconds - targetPaceSeconds
    val isGoalAchieved = kotlin.math.abs(paceDeviation) <= 30 // 30초 이내 오차
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BreezeTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
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
                    .height(200.dp)
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
                            text = "카카오 지도 API 키 설정 후 표시됩니다",
                            style = BreezeTheme.typography.bodySmall,
                            color = BreezeTheme.colors.textTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 주요 통계
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
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
                            style = BreezeTheme.typography.displayLarge.copy(fontSize = 56.sp),
                            color = BreezeTheme.colors.textPrimary
                        )
                        Text(
                            text = "km",
                            style = BreezeTheme.typography.titleMedium,
                            color = BreezeTheme.colors.textSecondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
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
                            value = "$calories kcal"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 목표 비교 카드
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
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
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 버튼 영역
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 저장 버튼
                GlassButton(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "기록 저장",
                            style = BreezeTheme.typography.titleMedium,
                            color = BreezeTheme.colors.textPrimary
                        )
                    }
                }
                
                // 삭제 버튼
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BreezeTheme.colors.cardBackground)
                            .clickable { onDiscard() },
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
            
            Spacer(modifier = Modifier.height(32.dp))
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
