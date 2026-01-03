package com.sleepysoong.breeze.ui.settings

import android.content.Context
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sleepysoong.breeze.ui.components.GlassCard
import com.sleepysoong.breeze.ui.components.rememberHapticFeedback
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import com.sleepysoong.breeze.ui.viewmodel.RunningViewModel

private const val PREFS_NAME = "breeze_settings"
private const val KEY_GOOGLE_MAPS_API = "google_maps_api_key"
private const val KEY_VOLUME = "metronome_volume"
private const val KEY_USE_KM = "use_kilometers"

@Composable
fun SettingsScreen(
    viewModel: RunningViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val haptic = rememberHapticFeedback()
    
    val totalRecords by viewModel.totalRecords.collectAsState()
    
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    var apiKey by remember { mutableStateOf(prefs.getString(KEY_GOOGLE_MAPS_API, "") ?: "") }
    var volume by remember { mutableFloatStateOf(prefs.getFloat(KEY_VOLUME, 1f)) }
    var useKilometers by remember { mutableStateOf(prefs.getBoolean(KEY_USE_KM, true)) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "설정",
            style = BreezeTheme.typography.headlineLarge,
            color = BreezeTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Google Maps API 키 설정
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { haptic(); showApiKeyDialog = true }
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Google Maps API 키",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (apiKey.isEmpty()) "설정되지 않음" else "설정됨",
                    style = BreezeTheme.typography.bodyMedium,
                    color = BreezeTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 메트로놈 볼륨 설정
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { haptic(); showVolumeDialog = true }
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "메트로놈 볼륨",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(volume * 100).toInt()}%",
                    style = BreezeTheme.typography.bodyMedium,
                    color = BreezeTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 거리 단위 설정
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { haptic(); showUnitDialog = true }
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "거리 단위",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (useKilometers) "킬로미터 (km)" else "마일 (mi)",
                    style = BreezeTheme.typography.bodyMedium,
                    color = BreezeTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 데이터 초기화
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { haptic(); showResetDialog = true }
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "데이터 초기화",
                    style = BreezeTheme.typography.titleMedium,
                    color = BreezeTheme.colors.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "총 ${totalRecords}개의 러닝 기록이 있어요",
                    style = BreezeTheme.typography.bodyMedium,
                    color = BreezeTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 앱 정보
        Text(
            text = "Breeze v1.0.0",
            style = BreezeTheme.typography.bodySmall,
            color = BreezeTheme.colors.textTertiary
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
    
    // API 키 다이얼로그
    if (showApiKeyDialog) {
        SettingsDialog(
            title = "Google Maps API 키",
            onDismiss = { showApiKeyDialog = false },
            onConfirm = { 
                prefs.edit().putString(KEY_GOOGLE_MAPS_API, apiKey).apply()
                showApiKeyDialog = false 
            }
        ) {
            var tempApiKey by remember { mutableStateOf(apiKey) }
            Column {
                Text(
                    text = "Google Cloud Console에서 발급받은 API 키를 입력하세요",
                    style = BreezeTheme.typography.bodySmall,
                    color = BreezeTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = tempApiKey,
                    onValueChange = { 
                        tempApiKey = it
                        apiKey = it
                    },
                    placeholder = { 
                        Text(
                            "API 키 입력",
                            color = BreezeTheme.colors.textTertiary
                        ) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = BreezeTheme.colors.textPrimary,
                        unfocusedTextColor = BreezeTheme.colors.textPrimary,
                        focusedContainerColor = BreezeTheme.colors.surface,
                        unfocusedContainerColor = BreezeTheme.colors.surface,
                        cursorColor = BreezeTheme.colors.primary,
                        focusedIndicatorColor = BreezeTheme.colors.primary,
                        unfocusedIndicatorColor = BreezeTheme.colors.cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
    
    // 볼륨 다이얼로그
    if (showVolumeDialog) {
        SettingsDialog(
            title = "메트로놈 볼륨",
            onDismiss = { showVolumeDialog = false },
            onConfirm = { 
                prefs.edit().putFloat(KEY_VOLUME, volume).apply()
                showVolumeDialog = false 
            }
        ) {
            var tempVolume by remember { mutableFloatStateOf(volume) }
            Column {
                Text(
                    text = "${(tempVolume * 100).toInt()}%",
                    style = BreezeTheme.typography.headlineMedium,
                    color = BreezeTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = tempVolume,
                    onValueChange = { 
                        tempVolume = it
                        volume = it
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = BreezeTheme.colors.primary,
                        activeTrackColor = BreezeTheme.colors.primary,
                        inactiveTrackColor = BreezeTheme.colors.cardBorder
                    )
                )
            }
        }
    }
    
    // 거리 단위 다이얼로그
    if (showUnitDialog) {
        SettingsDialog(
            title = "거리 단위",
            onDismiss = { showUnitDialog = false },
            onConfirm = { 
                prefs.edit().putBoolean(KEY_USE_KM, useKilometers).apply()
                showUnitDialog = false 
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UnitOption(
                    text = "킬로미터 (km)",
                    selected = useKilometers,
                    onClick = { useKilometers = true }
                )
                UnitOption(
                    text = "마일 (mi)",
                    selected = !useKilometers,
                    onClick = { useKilometers = false }
                )
            }
        }
    }
    
    // 데이터 초기화 확인 다이얼로그
    if (showResetDialog) {
        SettingsDialog(
            title = "데이터 초기화",
            onDismiss = { showResetDialog = false },
            onConfirm = { 
                viewModel.deleteAllRecords()
                showResetDialog = false 
            },
            confirmText = "초기화",
            isDestructive = true
        ) {
            Text(
                text = "모든 러닝 기록이 삭제돼요. 이 작업은 되돌릴 수 없어요.",
                style = BreezeTheme.typography.bodyMedium,
                color = BreezeTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "확인",
    isDestructive: Boolean = false,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BreezeTheme.colors.surface,
        title = {
            Text(
                text = title,
                style = BreezeTheme.typography.titleLarge,
                color = BreezeTheme.colors.textPrimary
            )
        },
        text = { content() },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDestructive) BreezeTheme.colors.error
                        else BreezeTheme.colors.primary
                    )
                    .clickable { onConfirm() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = confirmText,
                    style = BreezeTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "취소",
                    style = BreezeTheme.typography.labelLarge,
                    color = BreezeTheme.colors.textSecondary
                )
            }
        }
    )
}

@Composable
private fun UnitOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) BreezeTheme.colors.primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) BreezeTheme.colors.primary
                        else BreezeTheme.colors.cardBorder
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = BreezeTheme.typography.bodyLarge,
                color = if (selected) BreezeTheme.colors.textPrimary 
                       else BreezeTheme.colors.textSecondary
            )
        }
    }
}
