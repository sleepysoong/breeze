package com.sleepysoong.breeze.ui.running

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.breeze.service.MetronomeManager
import com.sleepysoong.breeze.service.RunningService
import com.sleepysoong.breeze.service.RunningState
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun RunningScreen(
    targetPaceSeconds: Int,
    onFinish: (distance: Double, time: Long, averagePace: Int) -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    var runningService by remember { mutableStateOf<RunningService?>(null) }
    var isBound by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    val runningState by (runningService?.runningState ?: MutableStateFlow(RunningState())).collectAsState()
    
    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        if (hasLocationPermission) {
            RunningService.startService(context, targetPaceSeconds)
        }
    }
    
    // 서비스 연결
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
    
    // 권한 요청 및 서비스 시작
    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
        
        // 서비스 바인딩
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
            // 상단: 지도 영역 (플레이스홀더)
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
                            text = "지도 영역",
                            style = BreezeTheme.typography.bodyLarge,
                            color = BreezeTheme.colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "카카오 지도 API 키를 설정에서 입력해주세요",
                            style = BreezeTheme.typography.bodySmall,
                            color = BreezeTheme.colors.textTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 중앙: 러닝 데이터
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 거리
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
                
                // 시간, 현재 페이스, 평균 페이스
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
                
                // 목표 페이스 표시
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
                                text = "메트로놈",
                                style = BreezeTheme.typography.bodySmall,
                                color = BreezeTheme.colors.textSecondary
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
            
            // 하단: 컨트롤 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 종료 버튼
                RunningControlButton(
                    icon = Icons.Default.Stop,
                    contentDescription = "종료",
                    isPrimary = false,
                    onClick = {
                        RunningService.stopService(context)
                        onFinish(distanceMeters, elapsedTimeMs, averagePaceSeconds)
                    }
                )
                
                Spacer(modifier = Modifier.width(32.dp))
                
                // 일시정지/재개 버튼
                RunningControlButton(
                    icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "재개" else "일시정지",
                    isPrimary = true,
                    onClick = {
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
