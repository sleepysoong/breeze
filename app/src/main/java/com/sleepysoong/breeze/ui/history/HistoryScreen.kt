package com.sleepysoong.breeze.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import com.sleepysoong.breeze.service.LatLngPoint
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.RouteLineThumbnail
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    records: List<RunningRecordEntity> = emptyList(),
    onRecordClick: (RunningRecordEntity) -> Unit = {},
    onDeleteRecord: (RunningRecordEntity) -> Unit = {}
) {
    val haptic = rememberHapticFeedback()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "기록",
            style = BreezeTheme.typography.headlineLarge,
            color = BreezeTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "아직 러닝 기록이 없어요",
                        style = BreezeTheme.typography.bodyLarge,
                        color = BreezeTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "첫 러닝을 시작해보세요",
                        style = BreezeTheme.typography.bodyMedium,
                        color = BreezeTheme.colors.textTertiary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(records) { index, record ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(record.id) {
                        delay(index * 50L)
                        visible = true
                    }
                    
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    animationSpec = tween(300),
                                    initialOffsetY = { it / 4 }
                                )
                    ) {
                        HistoryRecordCard(
                            record = record,
                            onClick = { haptic(); onRecordClick(record) }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: RunningRecordEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("M월 d일 (E)", Locale.KOREAN)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREAN)
    
    val date = dateFormat.format(Date(record.startTime))
    val time = timeFormat.format(Date(record.startTime))
    
    val distanceKm = record.totalDistance / 1000.0
    val timeMinutes = record.totalTime / 1000 / 60
    val timeSecs = (record.totalTime / 1000 % 60).toInt()
    val paceMin = record.averagePace / 60
    val paceSec = record.averagePace % 60

    val routePoints = remember(record.routePoints) {
        parseRoutePoints(record.routePoints)
    }
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RouteLineThumbnail(
                routePoints = routePoints,
                size = 56.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date,
                        style = BreezeTheme.typography.titleMedium,
                        color = BreezeTheme.colors.textPrimary
                    )
                    Text(
                        text = time,
                        style = BreezeTheme.typography.bodyMedium,
                        color = BreezeTheme.colors.textSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HistoryStatItem(
                        label = "거리",
                        value = String.format("%.2f km", distanceKm)
                    )
                    HistoryStatItem(
                        label = "시간",
                        value = String.format("%d:%02d", timeMinutes, timeSecs)
                    )
                    HistoryStatItem(
                        label = "페이스",
                        value = String.format("%d'%02d\"", paceMin, paceSec)
                    )
                }
            }
        }
    }
}

private fun parseRoutePoints(json: String): List<LatLngPoint> {
    return try {
        if (json.isBlank()) return emptyList()
        val gson = Gson()
        val type = object : TypeToken<List<LatLngPoint>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
private fun HistoryStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = BreezeTheme.typography.bodyLarge,
            color = BreezeTheme.colors.textPrimary
        )
        Text(
            text = label,
            style = BreezeTheme.typography.labelSmall,
            color = BreezeTheme.colors.textSecondary
        )
    }
}
