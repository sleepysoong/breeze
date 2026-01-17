package com.sleepysoong.breeze.ui.running

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.kyant.backdrop.Backdrop
import com.sleepysoong.breeze.service.LatLngPoint
import com.sleepysoong.breeze.service.MetronomeManager
import com.sleepysoong.breeze.service.RunningService
import com.sleepysoong.breeze.service.RunningState
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.SafeGoogleMap
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import com.sleepysoong.breeze.ui.viewmodel.RunningViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun RunningScreen(
    backdrop: Backdrop,
    targetPaceSeconds: Int,
    viewModel: RunningViewModel,
    onFinish: (distance: Double, time: Long, averagePace: Int) -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    var runningService by remember { mutableStateOf<RunningService?>(null) }
    var isBound by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    
    val runningState by (runningService?.runningState ?: MutableStateFlow(RunningState())).collectAsState()
    val locationPoints by (runningService?.locationPoints ?: MutableStateFlow(emptyList())).collectAsState()
    
    // 배터리 최적화 제외 확인
    fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    // 배터리 최적화 제외 요청
    fun requestBatteryOptimizationExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        if (hasLocationPermission) {
            // 배터리 최적화 제외 확인
            if (!checkBatteryOptimization()) {
                showBatteryDialog = true
            } else {
                RunningService.startService(context, targetPaceSeconds)
            }
        }
    }
    
    // 배터리 최적화 제외 다이얼로그
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { 
                showBatteryDialog = false
                RunningService.startService(context, targetPaceSeconds)
            },
            title = { Text("배터리 최적화 해제") },
            text = { 
                Text("러닝 중 앱이 백그라운드에서 종료되지 않도록 배터리 최적화를 해제해주세요. 이 설정을 하지 않으면 러닝 기록이 유실될 수 있습니다.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    requestBatteryOptimizationExemption()
                    RunningService.startService(context, targetPaceSeconds)
                }) {
                    Text("설정하기")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    RunningService.startService(context, targetPaceSeconds)
                }) {
                    Text("나중에")
                }
            }
        )
    }
    
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? RunningService.RunningBinder
                runningService = binder?.getService()
                isBound = true
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                runningService = null
                isBound = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        permissionLauncher.launch(permissions.toTypedArray())
        
        val intent = Intent(context, RunningService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            if (isBound) {
                context.unbindService(serviceConnection)
            }
        }
    }
    
    val elapsedTimeMs = runningState.elapsedTimeMs
    val distanceMeters = runningState.distanceMeters
    val currentPaceSeconds = runningState.currentPaceSeconds
    val isPaused = runningState.isPaused
    val currentBpm = runningState.currentBpm.takeIf { it > 0 } 
        ?: MetronomeManager.calculateBpm(targetPaceSeconds)
    val isSmartMode = runningState.isSmartMode
    
    val elapsedMinutes = (elapsedTimeMs / 1000 / 60).toInt()
    val elapsedSeconds = (elapsedTimeMs / 1000 % 60).toInt()
    
    val distanceKm = distanceMeters / 1000.0
    val averagePaceSeconds = if (distanceKm > 0.01) {
        ((elapsedTimeMs / 1000.0) / distanceKm).toInt()
    } else {
        0
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BreezeTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                LiveRunningMapView(
                    locationPoints = locationPoints,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "거리",
                    style = BreezeTheme.typography.bodyMedium,
                    color = BreezeTheme.colors.textSecondary
                )
                Text(
                    text = String.format("%.2f", distanceKm),
                    style = BreezeTheme.typography.displayLarge.copy(fontSize = 64.sp),
                    color = BreezeTheme.colors.textPrimary
                )
                Text(
                    text = "km",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RunningStatItem(
                        label = "시간",
                        value = String.format("%d:%02d", elapsedMinutes, elapsedSeconds)
                    )
                    RunningStatItem(
                        label = "현재 페이스",
                        value = if (currentPaceSeconds > 0) {
                            String.format("%d'%02d\"", currentPaceSeconds / 60, currentPaceSeconds % 60)
                        } else {
                            "--'--\""
                        }
                    )
                    RunningStatItem(
                        label = "평균 페이스",
                        value = if (averagePaceSeconds > 0) {
                            String.format("%d'%02d\"", averagePaceSeconds / 60, averagePaceSeconds % 60)
                        } else {
                            "--'--\""
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                                color = BreezeTheme.colors.primary
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (isSmartMode) "스마트 BPM" else "메트로놈",
                                style = BreezeTheme.typography.bodySmall,
                                color = if (isSmartMode) BreezeTheme.colors.primary else BreezeTheme.colors.textSecondary
                            )
                            Text(
                                text = "$currentBpm BPM",
                                style = BreezeTheme.typography.titleLarge,
                                color = BreezeTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RunningControlButton(
                    icon = Icons.Default.Stop,
                    contentDescription = "종료",
                    isPrimary = false,
                    onClick = {
                        haptic()
                        runningService?.let { service ->
                            viewModel.setPendingRunningData(
                                paceSegmentsJson = service.getPaceSegmentsJson(),
                                routePointsJson = service.getRoutePointsJson()
                            )
                        }
                        RunningService.stopService(context)
                        onFinish(distanceMeters, elapsedTimeMs, averagePaceSeconds)
                    }
                )
                
                Spacer(modifier = Modifier.width(32.dp))
                
                RunningControlButton(
                    icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "재개" else "일시정지",
                    isPrimary = true,
                    onClick = {
                        haptic()
                        if (isPaused) {
                            RunningService.resumeService(context)
                        } else {
                            RunningService.pauseService(context)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RunningStatItem(
    label: String,
    value: String
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
            color = BreezeTheme.colors.textPrimary
        )
    }
}

@Composable
private fun RunningControlButton(
    icon: ImageVector,
    contentDescription: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val size = if (isPrimary) 80.dp else 64.dp
    val iconSize = if (isPrimary) 40.dp else 32.dp
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (isPrimary) {
                    Modifier.background(BreezeTheme.colors.primary)
                } else {
                    Modifier
                        .background(BreezeTheme.colors.cardBackground)
                        .border(
                            width = 1.dp,
                            color = BreezeTheme.colors.cardBorder,
                            shape = CircleShape
                        )
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = BreezeTheme.colors.textPrimary
        )
    }
}

@Composable
private fun LiveRunningMapView(
    locationPoints: List<LatLngPoint>,
    modifier: Modifier = Modifier
) {
    val latLngs = remember(locationPoints) {
        locationPoints.map { LatLng(it.latitude, it.longitude) }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(37.5665, 126.9780),
            16f
        )
    }

    LaunchedEffect(locationPoints) {
        if (locationPoints.isNotEmpty()) {
            val lastPoint = locationPoints.last()
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(lastPoint.latitude, lastPoint.longitude),
                        16f
                    )
                )
            } catch (e: Exception) {
                Log.e("LiveRunningMapView", "Camera animation failed", e)
            }
        }
    }

    val mapProperties = remember {
        MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = false
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
            rotationGesturesEnabled = false,
            scrollGesturesEnabled = false,
            tiltGesturesEnabled = false,
            zoomGesturesEnabled = false
        )
    }

    SafeGoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings
    ) {
        if (latLngs.size >= 2) {
            Polyline(
                points = latLngs,
                color = Color(0xFFFF3B30),
                width = 12f
            )
        }
    }
}
