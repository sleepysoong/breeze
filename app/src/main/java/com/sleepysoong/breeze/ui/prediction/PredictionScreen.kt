package com.sleepysoong.breeze.ui.prediction

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import com.sleepysoong.breeze.ml.ConditionAnalysis
import com.sleepysoong.breeze.ml.ConditionLevel
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme

data class SegmentPrediction(
    val segmentIndex: Int,
    val startKm: Int,
    val endKm: Int,
    val predictedPaceSeconds: Int,
    val predictedTimeSeconds: Int
)

@Composable
fun PredictionScreen(
    hasTrainedModel: Boolean,
    trainingCount: Int,
    allRecords: List<RunningRecordEntity>,
    conditionAnalysis: ConditionAnalysis?,
    onPredictFinishTime: (distanceMeters: Double, targetPaceSeconds: Int) -> Long
) {
    val haptic = rememberHapticFeedback()
    var targetDistanceKm by remember { mutableIntStateOf(5) }
    var showPrediction by remember { mutableStateOf(false) }
    
    val avgPace = remember(allRecords) {
        if (allRecords.isEmpty()) 390
        else (allRecords.sumOf { it.averagePace } / allRecords.size)
    }
    
    val predictions = remember(targetDistanceKm, allRecords, showPrediction) {
        if (!showPrediction || allRecords.isEmpty()) emptyList()
        else calculateSegmentPredictions(targetDistanceKm, allRecords, avgPace)
    }
    
    // AI 예상 소요 시간
    val predictedFinishTimeMs = remember(targetDistanceKm, avgPace, showPrediction, conditionAnalysis) {
        if (!showPrediction) 0L
        else onPredictFinishTime(targetDistanceKm * 1000.0, avgPace)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "러닝 예측",
            style = BreezeTheme.typography.headlineLarge,
            color = BreezeTheme.colors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "AI가 구간별 소요 시간을 예측해요",
            style = BreezeTheme.typography.bodyLarge,
            color = BreezeTheme.colors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!hasTrainedModel || allRecords.isEmpty()) {
            NoDataCard()
        } else {
            // 현재 컨디션 분석 카드
            conditionAnalysis?.let { analysis ->
                ConditionAnalysisCard(analysis = analysis)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "모델 상태",
                        style = BreezeTheme.typography.titleMedium,
                        color = BreezeTheme.colors.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ModelStatItem(label = "학습 기록", value = "${trainingCount}회")
                        ModelStatItem(label = "평균 페이스", value = formatPace(avgPace))
                        ModelStatItem(label = "총 기록", value = "${allRecords.size}개")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "목표 거리",
                        style = BreezeTheme.typography.titleMedium,
                        color = BreezeTheme.colors.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BreezeTheme.colors.surface)
                                .clickable {
                                    haptic()
                                    if (targetDistanceKm > 1) {
                                        targetDistanceKm--
                                        showPrediction = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "감소",
                                tint = BreezeTheme.colors.textPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$targetDistanceKm",
                                style = BreezeTheme.typography.displayLarge,
                                color = BreezeTheme.colors.textPrimary
                            )
                            Text(
                                text = "km",
                                style = BreezeTheme.typography.titleMedium,
                                color = BreezeTheme.colors.textSecondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BreezeTheme.colors.surface)
                                .clickable {
                                    haptic()
                                    if (targetDistanceKm < 42) {
                                        targetDistanceKm++
                                        showPrediction = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "증가",
                                tint = BreezeTheme.colors.textPrimary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BreezeTheme.colors.primary)
                            .clickable {
                                haptic()
                                showPrediction = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "예측하기",
                            style = BreezeTheme.typography.titleMedium,
                            color = BreezeTheme.colors.textPrimary
                        )
                    }
                }
            }
            
            if (showPrediction && predictions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // AI 예상 소요 시간 카드
                if (predictedFinishTimeMs > 0) {
                    AIPredictionCard(
                        predictedTimeMs = predictedFinishTimeMs,
                        conditionAnalysis = conditionAnalysis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                PredictionResultCard(predictions = predictions, avgPace = avgPace)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PredictionGraph(predictions = predictions, avgPace = avgPace)
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun NoDataCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "학습 데이터가 필요해요",
                style = BreezeTheme.typography.titleLarge,
                color = BreezeTheme.colors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "러닝 기록이 쌓이면\nAI가 구간별 소요 시간을 예측할 수 있어요",
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "먼저 러닝을 시작해보세요",
                style = BreezeTheme.typography.bodySmall,
                color = BreezeTheme.colors.textTertiary
            )
        }
    }
}

@Composable
private fun ConditionAnalysisCard(analysis: ConditionAnalysis) {
    val conditionColor = when (analysis.conditionLevel) {
        ConditionLevel.EXCELLENT -> BreezeTheme.colors.success
        ConditionLevel.GOOD -> Color(0xFF4CAF50)
        ConditionLevel.NORMAL -> BreezeTheme.colors.primary
        ConditionLevel.FAIR -> BreezeTheme.colors.warning
        ConditionLevel.POOR -> Color(0xFFE53935)
    }
    
    val conditionText = when (analysis.conditionLevel) {
        ConditionLevel.EXCELLENT -> "최고"
        ConditionLevel.GOOD -> "좋음"
        ConditionLevel.NORMAL -> "보통"
        ConditionLevel.FAIR -> "약간 저조"
        ConditionLevel.POOR -> "저조"
    }
    
    val conditionDescription = when (analysis.conditionLevel) {
        ConditionLevel.EXCELLENT -> "지금이 러닝하기 가장 좋은 시간이에요!"
        ConditionLevel.GOOD -> "러닝하기 좋은 컨디션이에요"
        ConditionLevel.NORMAL -> "평소와 비슷한 컨디션이에요"
        ConditionLevel.FAIR -> "평소보다 조금 힘들 수 있어요"
        ConditionLevel.POOR -> "컨디션이 좋지 않을 수 있어요"
    }
    
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "현재 컨디션",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(conditionColor.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = conditionText,
                        style = BreezeTheme.typography.labelLarge,
                        color = conditionColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 현재 시간 컨텍스트
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConditionContextItem(
                    label = "요일",
                    value = analysis.dayOfWeek
                )
                ConditionContextItem(
                    label = "시간대",
                    value = analysis.timeBlock
                )
                ConditionContextItem(
                    label = "계절",
                    value = analysis.season
                )
                ConditionContextItem(
                    label = "구분",
                    value = if (analysis.isWeekend) "주말" else "평일"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = conditionDescription,
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textSecondary
            )
            
            if (!analysis.hasEnoughData) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "더 많은 기록이 쌓이면 분석이 정확해져요",
                    style = BreezeTheme.typography.bodySmall,
                    color = BreezeTheme.colors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun ConditionContextItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = BreezeTheme.typography.titleMedium,
            color = BreezeTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = BreezeTheme.typography.bodySmall,
            color = BreezeTheme.colors.textTertiary
        )
    }
}

@Composable
private fun AIPredictionCard(
    predictedTimeMs: Long,
    conditionAnalysis: ConditionAnalysis?
) {
    val totalSeconds = (predictedTimeMs / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    val timeText = if (hours > 0) {
        "${hours}시간 ${minutes}분 ${seconds}초"
    } else {
        "${minutes}분 ${seconds}초"
    }
    
    val adjustmentPercent = conditionAnalysis?.let {
        ((it.conditionMultiplier - 1.0f) * 100).toInt()
    } ?: 0
    
    val adjustmentText = when {
        adjustmentPercent > 0 -> "+${adjustmentPercent}%"
        adjustmentPercent < 0 -> "${adjustmentPercent}%"
        else -> "±0%"
    }
    
    val adjustmentColor = when {
        adjustmentPercent > 0 -> BreezeTheme.colors.warning
        adjustmentPercent < 0 -> BreezeTheme.colors.success
        else -> BreezeTheme.colors.textSecondary
    }
    
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI 예상 소요 시간",
                        style = BreezeTheme.typography.titleMedium,
                        color = BreezeTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "현재 컨디션 반영",
                        style = BreezeTheme.typography.bodySmall,
                        color = BreezeTheme.colors.textTertiary
                    )
                }
                
                Text(
                    text = adjustmentText,
                    style = BreezeTheme.typography.labelLarge,
                    color = adjustmentColor
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = timeText,
                style = BreezeTheme.typography.displayMedium,
                color = BreezeTheme.colors.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            conditionAnalysis?.let { analysis ->
                if (analysis.conditionMultiplier != 1.0f) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val explanation = when {
                        analysis.conditionMultiplier < 1.0f -> 
                            "${analysis.dayOfWeek} ${analysis.timeBlock}에 평소보다 좋은 기록을 내는 편이에요"
                        analysis.conditionMultiplier > 1.05f ->
                            "${analysis.dayOfWeek} ${analysis.timeBlock}에 평소보다 힘들어하는 편이에요"
                        else ->
                            "평소와 비슷한 기록이 예상돼요"
                    }
                    
                    Text(
                        text = explanation,
                        style = BreezeTheme.typography.bodySmall,
                        color = BreezeTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = BreezeTheme.typography.titleLarge,
            color = BreezeTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = BreezeTheme.typography.bodySmall,
            color = BreezeTheme.colors.textSecondary
        )
    }
}

@Composable
private fun PredictionResultCard(predictions: List<SegmentPrediction>, avgPace: Int) {
    val totalTimeSeconds = predictions.sumOf { it.predictedTimeSeconds }
    val totalMinutes = totalTimeSeconds / 60
    val totalSeconds = totalTimeSeconds % 60
    
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "예상 총 시간",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )
                Text(
                    text = "${totalMinutes}분 ${totalSeconds}초",
                    style = BreezeTheme.typography.headlineMedium,
                    color = BreezeTheme.colors.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            predictions.forEach { segment ->
                SegmentRow(segment = segment, avgPace = avgPace)
                if (segment != predictions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SegmentRow(segment: SegmentPrediction, avgPace: Int) {
    val minutes = segment.predictedTimeSeconds / 60
    val seconds = segment.predictedTimeSeconds % 60
    val isFaster = segment.predictedPaceSeconds < avgPace
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BreezeTheme.colors.surface.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${segment.startKm}-${segment.endKm}km",
            style = BreezeTheme.typography.bodyMedium,
            color = BreezeTheme.colors.textPrimary
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatPace(segment.predictedPaceSeconds),
                style = BreezeTheme.typography.bodySmall,
                color = if (isFaster) BreezeTheme.colors.success else BreezeTheme.colors.warning
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "${minutes}분 ${String.format("%02d", seconds)}초",
                style = BreezeTheme.typography.titleMedium,
                color = BreezeTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun PredictionGraph(predictions: List<SegmentPrediction>, avgPace: Int) {
    val textMeasurer = rememberTextMeasurer()
    
    val primaryColor = BreezeTheme.colors.primary
    val successColor = BreezeTheme.colors.success
    val warningColor = BreezeTheme.colors.warning
    val textSecondary = BreezeTheme.colors.textSecondary
    val textTertiary = BreezeTheme.colors.textTertiary
    val cardBorder = BreezeTheme.colors.cardBorder
    
    val paces = predictions.map { it.predictedPaceSeconds }
    val minPace = (paces.minOrNull() ?: avgPace) - 20
    val maxPace = (paces.maxOrNull() ?: avgPace) + 20
    
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "구간별 예상 페이스",
                style = BreezeTheme.typography.titleMedium,
                color = BreezeTheme.colors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 50f
                    val paddingRight = 20f
                    val paddingTop = 20f
                    val paddingBottom = 40f
                    
                    val graphWidth = width - paddingLeft - paddingRight
                    val graphHeight = height - paddingTop - paddingBottom
                    
                    val paceRange = (maxPace - minPace).toFloat()
                    val barWidth = graphWidth / predictions.size * 0.7f
                    val barSpacing = graphWidth / predictions.size
                    
                    val avgPaceY = paddingTop + graphHeight * (1 - (avgPace - minPace) / paceRange)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = Offset(paddingLeft, avgPaceY),
                        end = Offset(width - paddingRight, avgPaceY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                    
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = paddingTop + graphHeight * i / gridLines
                        drawLine(
                            color = cardBorder,
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1f
                        )
                        
                        val pace = maxPace - (maxPace - minPace) * i / gridLines
                        val paceText = "${pace / 60}'${String.format("%02d", pace % 60)}\""
                        val textLayoutResult = textMeasurer.measure(
                            text = paceText,
                            style = TextStyle(fontSize = 10.sp, color = textTertiary)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(4f, y - textLayoutResult.size.height / 2)
                        )
                    }
                    
                    predictions.forEachIndexed { index, prediction ->
                        val x = paddingLeft + barSpacing * index + (barSpacing - barWidth) / 2
                        val normalizedPace = ((prediction.predictedPaceSeconds - minPace) / paceRange).coerceIn(0f, 1f)
                        val barHeight = graphHeight * normalizedPace
                        val y = paddingTop + graphHeight - barHeight
                        
                        val barColor = when {
                            prediction.predictedPaceSeconds < avgPace -> successColor
                            prediction.predictedPaceSeconds <= avgPace + 15 -> warningColor
                            else -> primaryColor
                        }
                        
                        val gradient = Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.3f)),
                            startY = y,
                            endY = paddingTop + graphHeight
                        )
                        
                        drawRect(
                            brush = gradient,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )
                        
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, 4f)
                        )
                        
                        val kmText = "${prediction.endKm}km"
                        val kmTextLayoutResult = textMeasurer.measure(
                            text = kmText,
                            style = TextStyle(fontSize = 10.sp, color = textSecondary)
                        )
                        drawText(
                            textLayoutResult = kmTextLayoutResult,
                            topLeft = Offset(
                                x + barWidth / 2 - kmTextLayoutResult.size.width / 2,
                                paddingTop + graphHeight + 8f
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GraphLegendItem(color = successColor, label = "평균보다 빠름")
                Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                GraphLegendItem(color = warningColor, label = "평균 근접")
                Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                GraphLegendItem(color = primaryColor, label = "평균보다 느림")
            }
        }
    }
}

@Composable
private fun GraphLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color, radius = 6f)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = BreezeTheme.typography.bodySmall,
            color = BreezeTheme.colors.textTertiary
        )
    }
}

private fun formatPace(paceSeconds: Int): String {
    val min = paceSeconds / 60
    val sec = paceSeconds % 60
    return "${min}'${String.format("%02d", sec)}\""
}

private fun calculateSegmentPredictions(
    totalKm: Int,
    records: List<RunningRecordEntity>,
    avgPace: Int
): List<SegmentPrediction> {
    if (records.isEmpty()) return emptyList()
    
    val fatiguePattern = calculateFatiguePattern(records)
    
    return (0 until totalKm).map { kmIndex ->
        val progress = (kmIndex + 0.5f) / totalKm
        val fatigueMultiplier = getFatigueMultiplier(progress, fatiguePattern)
        val predictedPace = (avgPace * fatigueMultiplier).toInt()
        val predictedTime = predictedPace
        
        SegmentPrediction(
            segmentIndex = kmIndex,
            startKm = kmIndex,
            endKm = kmIndex + 1,
            predictedPaceSeconds = predictedPace,
            predictedTimeSeconds = predictedTime
        )
    }
}

private fun calculateFatiguePattern(records: List<RunningRecordEntity>): FloatArray {
    val pattern = FloatArray(10) { 1f }
    val counts = IntArray(10) { 0 }
    
    for (record in records) {
        val segments = parsePaceSegments(record.paceSegments)
        if (segments.isEmpty()) continue
        
        val avgRecordPace = record.averagePace.toFloat()
        if (avgRecordPace <= 0) continue
        
        for ((index, segment) in segments.withIndex()) {
            val progress = (index + 0.5f) / segments.size
            val bucketIndex = (progress * 10).toInt().coerceIn(0, 9)
            val paceRatio = segment.pace.toFloat() / avgRecordPace
            pattern[bucketIndex] += paceRatio
            counts[bucketIndex]++
        }
    }
    
    for (i in 0 until 10) {
        if (counts[i] > 0) {
            pattern[i] = pattern[i] / (counts[i] + 1)
        }
    }
    
    val avg = pattern.average().toFloat()
    if (avg > 0) {
        for (i in 0 until 10) {
            pattern[i] /= avg
        }
    }
    
    return pattern
}

private fun getFatigueMultiplier(progress: Float, pattern: FloatArray): Float {
    val exactIndex = progress * 9
    val lowerIndex = exactIndex.toInt().coerceIn(0, 9)
    val upperIndex = (lowerIndex + 1).coerceIn(0, 9)
    val fraction = exactIndex - lowerIndex
    return pattern[lowerIndex] * (1 - fraction) + pattern[upperIndex] * fraction
}

private data class PaceSegmentData(
    val segmentIndex: Int,
    val pace: Int,
    val startTime: Long,
    val endTime: Long
)

private fun parsePaceSegments(json: String): List<PaceSegmentData> {
    return try {
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<PaceSegmentData>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
