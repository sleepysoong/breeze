package com.sleepysoong.breeze.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.breeze.ml.PaceSegment
import com.sleepysoong.breeze.ui.theme.BreezeTheme

@Composable
fun PaceGraph(
    paceSegments: List<PaceSegment>,
    targetPace: Int,
    modifier: Modifier = Modifier
) {
    if (paceSegments.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "구간 데이터 없음",
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textTertiary
            )
        }
        return
    }
    
    val textMeasurer = rememberTextMeasurer()
    
    val primaryColor = BreezeTheme.colors.primary
    val successColor = BreezeTheme.colors.success
    val warningColor = BreezeTheme.colors.warning
    val textSecondary = BreezeTheme.colors.textSecondary
    val textTertiary = BreezeTheme.colors.textTertiary
    val cardBorder = BreezeTheme.colors.cardBorder
    
    val graphData = remember(paceSegments, targetPace) {
        val paces = paceSegments.map { it.pace }
        val minPace = (paces.minOrNull() ?: targetPace) - 30
        val maxPace = (paces.maxOrNull() ?: targetPace) + 30
        val avgPace = if (paces.isNotEmpty()) paces.average().toInt() else targetPace
        GraphData(paces, minPace, maxPace, avgPace, targetPace)
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "구간별 페이스",
                style = BreezeTheme.typography.titleMedium,
                color = BreezeTheme.colors.textPrimary
            )
            Text(
                text = "평균 ${graphData.avgPace / 60}'${String.format("%02d", graphData.avgPace % 60)}\"",
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
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
                
                val paceRange = (graphData.maxPace - graphData.minPace).toFloat()
                val barWidth = graphWidth / graphData.paces.size * 0.7f
                val barSpacing = graphWidth / graphData.paces.size
                
                val targetY = paddingTop + graphHeight * (1 - (targetPace - graphData.minPace) / paceRange)
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(paddingLeft, targetY),
                    end = Offset(width - paddingRight, targetY),
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
                    
                    val pace = graphData.maxPace - (graphData.maxPace - graphData.minPace) * i / gridLines
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
                
                graphData.paces.forEachIndexed { index, pace ->
                    val x = paddingLeft + barSpacing * index + (barSpacing - barWidth) / 2
                    val normalizedPace = ((pace - graphData.minPace) / paceRange).coerceIn(0f, 1f)
                    val barHeight = graphHeight * normalizedPace
                    val y = paddingTop + graphHeight - barHeight
                    
                    val barColor = when {
                        pace <= targetPace -> successColor
                        pace <= targetPace + 30 -> warningColor
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
                    
                    val kmText = "${index + 1}km"
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
            LegendItem(color = successColor, label = "목표 이하")
            Spacer(modifier = Modifier.padding(horizontal = 12.dp))
            LegendItem(color = warningColor, label = "목표 근접")
            Spacer(modifier = Modifier.padding(horizontal = 12.dp))
            LegendItem(color = primaryColor, label = "목표 초과")
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.height(12.dp).padding(end = 4.dp)) {
            drawCircle(color = color, radius = 6f)
        }
        Text(
            text = label,
            style = BreezeTheme.typography.bodySmall,
            color = BreezeTheme.colors.textTertiary
        )
    }
}

private data class GraphData(
    val paces: List<Int>,
    val minPace: Int,
    val maxPace: Int,
    val avgPace: Int,
    val targetPace: Int
)
