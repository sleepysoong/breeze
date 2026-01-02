package com.sleepysoong.breeze.ui.detail

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.route.RouteLineLayer
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import com.sleepysoong.breeze.ml.PaceSegment
import com.sleepysoong.breeze.service.LatLngPoint
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.PaceGraph
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordDetailScreen(
    record: RunningRecordEntity?,
    onBack: () -> Unit,
    onDelete: (RunningRecordEntity) -> Unit
) {
    if (record == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "기록을 찾을 수 없어요",
                style = BreezeTheme.typography.bodyLarge,
                color = BreezeTheme.colors.textSecondary
            )
        }
        return
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        RecordDetailContent(
            record = record,
            onBack = onBack,
            onDelete = onDelete
        )
    }
}

@Composable
private fun RecordDetailContent(
    record: RunningRecordEntity,
    onBack: () -> Unit,
    onDelete: (RunningRecordEntity) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val routePoints = remember(record) {
        parseRoutePoints(record.routePoints)
    }

    val paceSegments = remember(record) {
        parsePaceSegments(record.paceSegments)
    }

    val dateFormat = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREAN)

    val date = dateFormat.format(Date(record.startTime))
    val time = timeFormat.format(Date(record.startTime))

    val distanceKm = record.totalDistance / 1000.0
    val timeMinutes = (record.totalTime / 1000 / 60).toInt()
    val timeSecs = (record.totalTime / 1000 % 60).toInt()
    val paceMin = record.averagePace / 60
    val paceSec = record.averagePace % 60
    val targetPaceMin = record.targetPace / 60
    val targetPaceSec = record.targetPace % 60

    val paceDeviation = record.averagePace - record.targetPace
    val isGoalAchieved = kotlin.math.abs(paceDeviation) <= 30

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete(record)
                onBack()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BreezeTheme.colors.background.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BreezeTheme.colors.cardBackground)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = BreezeTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "러닝 기록",
                    style = BreezeTheme.typography.titleLarge,
                    color = BreezeTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(40.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = date,
                    style = BreezeTheme.typography.headlineMedium,
                    color = BreezeTheme.colors.textPrimary
                )
                Text(
                    text = "$time 시작",
                    style = BreezeTheme.typography.bodyMedium,
                    color = BreezeTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    if (routePoints.isNotEmpty()) {
                        KakaoMapRouteView(
                            routePoints = routePoints,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "경로 데이터 없음",
                                    style = BreezeTheme.typography.bodyLarge,
                                    color = BreezeTheme.colors.textTertiary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "이 기록에는 GPS 경로가 저장되지 않았어요",
                                    style = BreezeTheme.typography.bodySmall,
                                    color = BreezeTheme.colors.textTertiary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DetailStatItem(
                                label = "시간",
                                value = String.format("%d:%02d", timeMinutes, timeSecs)
                            )
                            DetailStatItem(
                                label = "평균 페이스",
                                value = String.format("%d'%02d\"", paceMin, paceSec),
                                highlight = isGoalAchieved
                            )
                            DetailStatItem(
                                label = "칼로리",
                                value = "${record.calories}"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                text = String.format("%d'%02d\"", targetPaceMin, targetPaceSec),
                                style = BreezeTheme.typography.titleLarge,
                                color = BreezeTheme.colors.textPrimary
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (isGoalAchieved) "목표 달성" else "차이",
                                style = BreezeTheme.typography.bodySmall,
                                color = if (isGoalAchieved) BreezeTheme.colors.primary else BreezeTheme.colors.textSecondary
                            )
                            Text(
                                text = when {
                                    isGoalAchieved -> "성공"
                                    paceDeviation > 0 -> "+${paceDeviation}초"
                                    else -> "${paceDeviation}초"
                                },
                                style = BreezeTheme.typography.titleLarge,
                                color = when {
                                    isGoalAchieved -> BreezeTheme.colors.primary
                                    paceDeviation > 0 -> BreezeTheme.colors.textSecondary
                                    else -> BreezeTheme.colors.primaryLight
                                }
                            )
                        }
                    }
                }

                if (paceSegments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PaceGraph(
                            paceSegments = paceSegments,
                            targetPace = record.targetPace,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BreezeTheme.colors.primary)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "닫기",
                        style = BreezeTheme.typography.titleMedium,
                        color = BreezeTheme.colors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BreezeTheme.colors.cardBackground)
                        .clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "기록 삭제",
                        style = BreezeTheme.typography.titleMedium,
                        color = BreezeTheme.colors.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    .clickable(enabled = false) {}
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
                        text = "삭제하면 이 러닝 기록이\n완전히 사라져요",
                        style = BreezeTheme.typography.bodyMedium,
                        color = BreezeTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "취소",
                                style = BreezeTheme.typography.labelLarge,
                                color = BreezeTheme.colors.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BreezeTheme.colors.error)
                                .clickable { onConfirm() },
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
private fun DetailStatItem(
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

@Composable
fun KakaoMapRouteView(
    routePoints: List<LatLngPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

    val apiKey = remember {
        getKakaoApiKey(context)
    }

    if (apiKey.isNullOrEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "카카오 지도 API 키가 필요해요",
                    style = BreezeTheme.typography.bodyLarge,
                    color = BreezeTheme.colors.textTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "설정에서 API 키를 입력해주세요",
                    style = BreezeTheme.typography.bodySmall,
                    color = BreezeTheme.colors.textTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.finish()
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapView = this
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {}
                        override fun onMapError(error: Exception?) {}
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            kakaoMap = map
                            if (routePoints.isNotEmpty()) {
                                drawRoute(map, routePoints)
                                moveCameraToRoute(map, routePoints)
                            }
                        }
                    }
                )
            }
        },
        modifier = modifier
    )
}

private fun drawRoute(map: KakaoMap, points: List<LatLngPoint>) {
    if (points.size < 2) return

    try {
        val routeLineLayer: RouteLineLayer = map.routeLineManager?.layer ?: return
        val latLngs = points.map { LatLng.from(it.latitude, it.longitude) }

        val styles = RouteLineStylesSet.from(
            RouteLineStyles.from(
                RouteLineStyle.from(8f, 0xFFFF3B30.toInt())
            )
        )

        val segment = RouteLineSegment.from(latLngs)
            .setStyles(styles.getStyles(0))

        val options = RouteLineOptions.from(segment)
            .setStylesSet(styles)

        routeLineLayer.addRouteLine(options)
    } catch (e: Exception) {
    }
}

private fun moveCameraToRoute(map: KakaoMap, points: List<LatLngPoint>) {
    if (points.isEmpty()) return

    try {
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }

        val centerLat = (minLat + maxLat) / 2
        val centerLng = (minLng + maxLng) / 2

        val latDiff = maxLat - minLat
        val lngDiff = maxLng - minLng
        val maxDiff = maxOf(latDiff, lngDiff)

        val zoomLevel = when {
            maxDiff > 0.1 -> 10
            maxDiff > 0.05 -> 12
            maxDiff > 0.02 -> 13
            maxDiff > 0.01 -> 14
            maxDiff > 0.005 -> 15
            else -> 16
        }

        val cameraUpdate = CameraUpdateFactory.newCenterPosition(
            LatLng.from(centerLat, centerLng),
            zoomLevel
        )
        map.moveCamera(cameraUpdate)
    } catch (e: Exception) {
    }
}

private fun getKakaoApiKey(context: Context): String? {
    return try {
        val prefs = context.getSharedPreferences("breeze_settings", Context.MODE_PRIVATE)
        prefs.getString("kakao_api_key", null)
    } catch (e: Exception) {
        null
    }
}

private fun parseRoutePoints(json: String): List<LatLngPoint> {
    return try {
        val gson = Gson()
        val type = object : TypeToken<List<LatLngPoint>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun parsePaceSegments(json: String): List<PaceSegment> {
    return try {
        val gson = Gson()
        val type = object : TypeToken<List<PaceSegment>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
