package com.sleepysoong.breeze.ui.detail

import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.sleepysoong.breeze.service.LatLngPoint
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordDetailScreen(
    record: RunningRecordEntity?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
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
    
    val routePoints = remember(record) {
        parseRoutePoints(record.routePoints)
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
    
    // 목표 달성 여부
    val paceDeviation = record.averagePace - record.targetPace
    val isGoalAchieved = kotlin.math.abs(paceDeviation) <= 30
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BreezeTheme.colors.background)
    ) {
        // 상단 바
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 날짜/시간
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
            
            // 지도 영역
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
            
            Spacer(modifier = Modifier.height(32.dp))
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
    
    // API 키 확인
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
                        override fun onMapDestroy() {
                            // 지도 종료
                        }
                        
                        override fun onMapError(error: Exception?) {
                            // 에러 처리
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            kakaoMap = map
                            
                            if (routePoints.isNotEmpty()) {
                                // 경로 그리기
                                drawRoute(map, routePoints)
                                
                                // 카메라 이동
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
        
        // 경로 좌표 변환
        val latLngs = points.map { LatLng.from(it.latitude, it.longitude) }
        
        // 경로 스타일 설정 (빨간색)
        val styles = RouteLineStylesSet.from(
            RouteLineStyles.from(
                RouteLineStyle.from(8f, 0xFFFF3B30.toInt())
            )
        )
        
        // 경로 세그먼트 생성
        val segment = RouteLineSegment.from(latLngs)
            .setStyles(styles.getStyles(0))
        
        // 경로 옵션
        val options = RouteLineOptions.from(segment)
            .setStylesSet(styles)
        
        routeLineLayer.addRouteLine(options)
    } catch (e: Exception) {
        // 경로 그리기 실패 시 무시
    }
}

private fun moveCameraToRoute(map: KakaoMap, points: List<LatLngPoint>) {
    if (points.isEmpty()) return
    
    try {
        // 경로의 중심점과 bounds 계산
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }
        
        val centerLat = (minLat + maxLat) / 2
        val centerLng = (minLng + maxLng) / 2
        
        // 줌 레벨 계산 (경로 크기에 따라)
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
        // 카메라 이동 실패 시 무시
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
