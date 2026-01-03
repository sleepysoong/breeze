package com.sleepysoong.breeze.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sleepysoong.breeze.service.LatLngPoint
import com.sleepysoong.breeze.ui.theme.BreezeTheme

@Composable
fun RouteLineThumbnail(
    routePoints: List<LatLngPoint>,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    lineColor: Color = BreezeTheme.colors.primary,
    lineWidth: Float = 3f,
    backgroundColor: Color = BreezeTheme.colors.surface,
    padding: Float = 8f
) {
    if (routePoints.size < 2) {
        EmptyRoutePlaceholder(
            modifier = modifier,
            size = size,
            backgroundColor = backgroundColor
        )
        return
    }

    val normalizedPoints = remember(routePoints) {
        normalizeRoutePoints(routePoints, padding)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height

            if (normalizedPoints.size >= 2) {
                val path = Path()
                
                val firstPoint = normalizedPoints.first()
                path.moveTo(
                    firstPoint.x * canvasWidth,
                    firstPoint.y * canvasHeight
                )

                for (i in 1 until normalizedPoints.size) {
                    val point = normalizedPoints[i]
                    path.lineTo(
                        point.x * canvasWidth,
                        point.y * canvasHeight
                    )
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = lineWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val startPointColor = Color(0xFF34C759)
                val endPointColor = lineColor
                val pointRadius = lineWidth * 2

                drawCircle(
                    color = startPointColor,
                    radius = pointRadius,
                    center = Offset(
                        firstPoint.x * canvasWidth,
                        firstPoint.y * canvasHeight
                    )
                )

                val lastPoint = normalizedPoints.last()
                drawCircle(
                    color = endPointColor,
                    radius = pointRadius,
                    center = Offset(
                        lastPoint.x * canvasWidth,
                        lastPoint.y * canvasHeight
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyRoutePlaceholder(
    modifier: Modifier = Modifier,
    size: Dp,
    backgroundColor: Color
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2
            val dotRadius = 4f

            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = dotRadius,
                center = Offset(centerX - 12, centerY)
            )
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = dotRadius,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = dotRadius,
                center = Offset(centerX + 12, centerY)
            )
        }
    }
}

private data class NormalizedPoint(val x: Float, val y: Float)

private fun normalizeRoutePoints(
    points: List<LatLngPoint>,
    padding: Float
): List<NormalizedPoint> {
    if (points.isEmpty()) return emptyList()

    var minLat = Double.MAX_VALUE
    var maxLat = Double.MIN_VALUE
    var minLng = Double.MAX_VALUE
    var maxLng = Double.MIN_VALUE

    for (point in points) {
        if (point.latitude < minLat) minLat = point.latitude
        if (point.latitude > maxLat) maxLat = point.latitude
        if (point.longitude < minLng) minLng = point.longitude
        if (point.longitude > maxLng) maxLng = point.longitude
    }

    val latRange = maxLat - minLat
    val lngRange = maxLng - minLng

    if (latRange == 0.0 && lngRange == 0.0) {
        return listOf(NormalizedPoint(0.5f, 0.5f))
    }

    val paddingRatio = padding / 100f
    val usableRange = 1f - (paddingRatio * 2)

    return points.map { point ->
        val normalizedX = if (lngRange > 0) {
            paddingRatio + ((point.longitude - minLng) / lngRange * usableRange).toFloat()
        } else {
            0.5f
        }

        val normalizedY = if (latRange > 0) {
            paddingRatio + (1f - ((point.latitude - minLat) / latRange).toFloat()) * usableRange
        } else {
            0.5f
        }

        NormalizedPoint(normalizedX, normalizedY)
    }
}
